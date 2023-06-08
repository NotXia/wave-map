package com.example.wavemap.ui.main

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.wavemap.R
import com.example.wavemap.dialogs.*
import com.example.wavemap.dialogs.settings.*
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.measures.samplers.BluetoothSampler
import com.example.wavemap.measures.samplers.LTESampler
import com.example.wavemap.measures.samplers.NoiseSampler
import com.example.wavemap.measures.samplers.WiFiSampler
import com.example.wavemap.services.BackgroundScanService
import com.example.wavemap.ui.main.viewmodels.*
import com.example.wavemap.ui.settings.SettingsActivity
import com.example.wavemap.ui.share.ShareActivity
import com.example.wavemap.utilities.Permissions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private class SamplerPermissionsHandler(
        val permissions : Array<String>,
        val permissions_dialog : DialogFragment,
        val checks_before_measure : () -> DialogFragment?
    )

    private val view_model : MainViewModel by viewModels()

    private lateinit var map_fragment : WaveHeatMapFragment
    private var successful_init = false
    private lateinit var permissions_check_and_init : ActivityResultLauncher<Array<String>>
    private lateinit var permissions_check_and_measure_current : ActivityResultLauncher<Array<String>>

    private lateinit var measure_spinner : Spinner
    private lateinit var fab_start_measure : FloatingActionButton
    private lateinit var fab_query : FloatingActionButton

    private val BUNDLE_MAP_FRAGMENT = "map-fragment"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        measure_spinner = findViewById(R.id.spinner_sampler)
        fab_start_measure = findViewById(R.id.btn_scan)
        fab_query = findViewById(R.id.btn_query)

        map_fragment = if (savedInstanceState != null && supportFragmentManager.getFragment(savedInstanceState, BUNDLE_MAP_FRAGMENT) != null) {
            supportFragmentManager.getFragment(savedInstanceState, BUNDLE_MAP_FRAGMENT) as WaveHeatMapFragment
        } else {
            WaveHeatMapFragment()
        }
        map_fragment.changeViewModel(view_model.curr_sampler.view_model)

        permissions_check_and_init = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Checks if the minimum required permissions are granted
            if (!Permissions.minimumRequired.all{ permission -> ContextCompat.checkSelfPermission(baseContext, permission) == PackageManager.PERMISSION_GRANTED }) {
                MissingMinimumPermissionsDialog().show(supportFragmentManager, MissingMinimumPermissionsDialog.TAG)
            }
            else if (!successful_init) {
                init()
            }
        }

        permissions_check_and_measure_current = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { is_granted: Map<String, Boolean> ->
            if (!is_granted.values.all{ granted -> granted }) {
                val dialog = getSamplerPermissionsHandler(view_model.curr_sampler.view_model.sampler).permissions_dialog
                dialog.show(supportFragmentManager, OpenAppSettingsDialog.TAG)
            }
            else {
                view_model.userTriggeredMeasure(view_model.curr_sampler)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        permissions_check_and_init.launch(Permissions.allRequired)
    }

    override fun onResume() {
        super.onResume()

        try {
            view_model.curr_sampler.view_model.loadSettingsPreferences() // In case settings changed
            map_fragment.refreshMap()
            view_model.startPeriodicScan()
            BackgroundScanService.stop(this)
        }
        catch (err : Exception) {
            Log.e("resume", "$err")
        }
    }

    override fun onPause() {
        super.onPause()

        try {
            view_model.stopPeriodicScan()
            if (supportFragmentManager.findFragmentByTag(MeasureFilterDialog.TAG) != null) {
                (supportFragmentManager.findFragmentByTag(MeasureFilterDialog.TAG) as MeasureFilterDialog).dismiss()
            }
            if (supportFragmentManager.findFragmentByTag(MissingMinimumPermissionsDialog.TAG) != null) {
                (supportFragmentManager.findFragmentByTag(MissingMinimumPermissionsDialog.TAG) as MissingMinimumPermissionsDialog).dismiss()
            }
            BackgroundScanService.start(this)
        }
        catch (err : Exception) {
            Log.e("pause", "$err")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (successful_init) {
            supportFragmentManager.putFragment(outState, BUNDLE_MAP_FRAGMENT, map_fragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.menu_options, menu)
        menu.findItem(R.id.menu_settings).intent = Intent(this, SettingsActivity::class.java)
        menu.findItem(R.id.menu_share).intent = Intent(this, ShareActivity::class.java)

        return true
    }


    /**
     * Init
     * */

    private fun init() {
        supportFragmentManager.commit {
            replace(R.id.fragment_container_map, map_fragment)
        }

        initSamplerSelector()
        initNewMeasureFAB()
        initFilterFAB()
        initTileChangeListener()

        view_model.startPeriodicScan()

        successful_init = true
    }

    private fun initSamplerSelector() {
        measure_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, view_model.available_samplers.map{ it.label })
        measure_spinner.setSelection(view_model.curr_sampler_index, false)
        measure_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                view_model.changeSampler(position)
                map_fragment.changeViewModel(view_model.curr_sampler.view_model)

                fab_query.visibility = if (view_model.curr_sampler.view_model is QueryableMeasureViewModel) View.VISIBLE else View.INVISIBLE
                // Disable measure fab if the sampler is already measuring
                if (view_model.curr_sampler.currently_measuring.value == true) { disableMeasureFab() }
                else { enableMeasureFab() }
            }
        }
    }

    private fun initNewMeasureFAB() {
        fab_start_measure.setOnClickListener {
            userTriggeredMeasure()
            view_model.resetPeriodicScan()
        }

        // Handle fab disabling and re-enabling
        view_model.available_samplers.forEach { sampler_handler: MainViewModel.SamplerHandler ->
            sampler_handler.currently_measuring.observe(this) { is_measuring: Boolean ->
                if (view_model.curr_sampler.view_model == sampler_handler.view_model) {
                    if (is_measuring) {
                        disableMeasureFab()
                    } else {
                        enableMeasureFab()
                    }
                }
            }
        }

        // Map refresh handling
        view_model.should_refresh_map.observe(this) { should_update: Boolean ->
            if (should_update) {
                map_fragment.refreshMap()
            }
        }

        // Error handling
        view_model.user_measure_error.observe(this) { error: Exception? ->
            if (error == null) { return@observe }
            Log.e("measure", "User triggered measure: $error")
            Toast.makeText(baseContext, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
            enableMeasureFab()
        }
    }

    private fun initFilterFAB() {
        fab_query.setOnClickListener {
            val sampler = view_model.curr_sampler
            if (sampler.view_model !is QueryableMeasureViewModel) { return@setOnClickListener }

            lifecycleScope.launch(Dispatchers.IO) {
                val queries = view_model.getSamplerQueries(sampler)
                val items : ArrayList<CharSequence> = queries.map{ it.first }.toCollection(ArrayList())

                MeasureFilterDialog(items){ index ->
                    sampler.view_model.changeQuery(queries[index].second)
                    map_fragment.refreshMap()
                }.show(supportFragmentManager, MeasureFilterDialog.TAG)
            }
        }
    }

    private fun initTileChangeListener() {
        map_fragment.current_tile.observe(this) {
            view_model.tileChangeMeasure()
        }
    }


    /**
     * Measure
     * */

    private fun getSamplerPermissionsHandler(sampler_viewmodel: WaveSampler) : SamplerPermissionsHandler {
        return when (sampler_viewmodel) {
            is WiFiSampler -> SamplerPermissionsHandler(
                Permissions.wifi, MissingWiFiPermissionsDialog()) {
                    if (WiFiSampler.isWiFiEnabled(baseContext)) null else WiFiNotEnabledDialog()
                }
            is LTESampler -> SamplerPermissionsHandler(Permissions.lte, MissingLTEPermissionsDialog()) { null }
            is NoiseSampler -> SamplerPermissionsHandler(Permissions.noise, MissingNoisePermissionsDialog()) { null }
            is BluetoothSampler -> SamplerPermissionsHandler(Permissions.bluetooth, MissingBluetoothPermissionsDialog()) {
                    if (BluetoothSampler.isBluetoothEnabled(baseContext)) null else BluetoothNotEnabledDialog()
                }
            else -> throw RuntimeException("Unknown sampler")
        }
    }

    private fun userTriggeredMeasure() {
        val sampler_permissions = getSamplerPermissionsHandler(view_model.curr_sampler.view_model.sampler)
        val to_show_dialog = sampler_permissions.checks_before_measure()

        if (to_show_dialog == null) {
            permissions_check_and_measure_current.launch(sampler_permissions.permissions)
        } else {
            to_show_dialog.show(supportFragmentManager, OpenAppSettingsDialog.TAG)
        }
    }



    /**
     * Measure FAB
     * */

    private fun disableMeasureFab() {
        fab_start_measure.apply{
            isClickable = false
            backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
            setImageResource(R.drawable.loading)
            drawable.setTint(Color.LTGRAY)
        }
        val animation = fab_start_measure.drawable as AnimationDrawable
        animation.start()
    }

    private fun enableMeasureFab() {
        val primary_color = TypedValue()
        theme.resolveAttribute(androidx.transition.R.attr.colorPrimary, primary_color, true)
        fab_start_measure.apply {
            isClickable = true
            backgroundTintList = ColorStateList.valueOf(primary_color.data)
            setImageResource(android.R.drawable.ic_input_add)
        }
    }

}