package com.example.wavemap.ui.main

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.animation.LinearInterpolator
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
import androidx.preference.PreferenceManager
import com.example.wavemap.R
import com.example.wavemap.dialogs.*
import com.example.wavemap.measures.samplers.BluetoothSampler
import com.example.wavemap.measures.samplers.WiFiSampler
import com.example.wavemap.services.BackgroundScanService
import com.example.wavemap.ui.main.viewmodels.*
import com.example.wavemap.ui.settings.SettingsActivity
import com.example.wavemap.utilities.Permissions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private class SamplerHandler(
        val view_model : MeasureViewModel,
        val label : String,
        val permissions : Array<String>,
        val permissions_dialog : OpenAppSettingsDialog,
        val checks_before_measure : () -> Boolean
    ) {
        var currently_measuring : Boolean = false // To prevent multiple measure requests
    }

    private lateinit var curr_sampler : SamplerHandler
    private lateinit var map_fragment : WaveHeatMapFragment
    private lateinit var pref_manager : SharedPreferences
    private var successful_init = false
    private lateinit var permissions_check_and_init : ActivityResultLauncher<Array<String>>
    private lateinit var permissions_check_and_measure_current : ActivityResultLauncher<Array<String>>

    private val wifi_model : WiFiViewModel by viewModels()
    private val lte_model : LTEViewModel by viewModels()
    private val noise_model : NoiseViewModel by viewModels()
    private val bluetooth_model : BluetoothViewModel by viewModels()
    private lateinit var available_samplers : Array<SamplerHandler>

    private lateinit var measure_spinner : Spinner
    private lateinit var fab_start_measure : FloatingActionButton
    private lateinit var fab_query : FloatingActionButton

    private var fab_start_measure_loading_anim : ValueAnimator? = null

    private var periodic_scan_handler : Handler = Handler(Looper.getMainLooper())

    private var to_dismiss_dialogs = mutableListOf<DialogFragment>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        available_samplers = arrayOf(
            SamplerHandler(wifi_model, resources.getString(R.string.wifi), Permissions.wifi, OpenAppSettingsDialog(R.string.missing_wifi_permission, R.string.missing_wifi_permission_desc)) {
                if (WiFiSampler.isWiFiEnabled(baseContext)) { true }
                else {
                    OpenWiFiSettingsDialog(R.string.wifi_not_enabled, null).show(supportFragmentManager, OpenWiFiSettingsDialog.TAG)
                    false
                }
            },
            SamplerHandler(lte_model, resources.getString(R.string.lte), Permissions.lte, OpenAppSettingsDialog(R.string.missing_lte_permission, R.string.missing_lte_permission_desc)) {
                true
            },
            SamplerHandler(noise_model, resources.getString(R.string.noise), Permissions.noise, OpenAppSettingsDialog(R.string.missing_noise_permission, R.string.missing_noise_permission_desc)) {
                true
            },
            SamplerHandler(bluetooth_model, resources.getString(R.string.bluetooth), Permissions.bluetooth, OpenAppSettingsDialog(R.string.missing_bluetooth_permission, R.string.missing_bluetooth_permission_desc)) {
                if (BluetoothSampler.isBluetoothEnabled(baseContext)) { true }
                else {
                    OpenBluetoothSettingsDialog(R.string.bluetooth_not_enabled, null).show(supportFragmentManager, OpenBluetoothSettingsDialog.TAG)
                    false
                }
            }
        )

        measure_spinner = findViewById(R.id.spinner_sampler)
        fab_start_measure = findViewById(R.id.btn_scan)
        fab_query = findViewById(R.id.btn_query)

        curr_sampler = available_samplers[0]
        map_fragment = WaveHeatMapFragment(curr_sampler.view_model)

        pref_manager = PreferenceManager.getDefaultSharedPreferences(baseContext)

        permissions_check_and_init = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Checks if the minimum required permissions are granted
            if (!Permissions.minimumRequired.all{ permission -> ContextCompat.checkSelfPermission(baseContext, permission) == PackageManager.PERMISSION_GRANTED }) {
                val dialog = MissingMinimumPermissionsDialog()
                to_dismiss_dialogs.add(dialog)
                dialog.show(supportFragmentManager, MissingMinimumPermissionsDialog.TAG)
            }
            else if (!successful_init) {
                init()
            }
        }

        permissions_check_and_measure_current = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { is_granted: Map<String, Boolean> ->
            if (!is_granted.values.all{ granted -> granted }) {
                val dialog = curr_sampler.permissions_dialog
                dialog.show(supportFragmentManager, OpenAppSettingsDialog.TAG)
            }
            else {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        measureOne(curr_sampler)
                    }
                    catch (err: Exception) {
                        Log.e("measure", "User triggered measure: $err")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(baseContext, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
            curr_sampler.view_model.loadSettingsPreferences()
            lifecycleScope.launch { map_fragment.refreshMap() }
            startPeriodicScan()
            BackgroundScanService.stop(this)
        }
        catch (err : Exception) {
            Log.e("resume", "$err")
        }
    }

    override fun onPause() {
        super.onPause()

        try {
            stopPeriodicScan()
            to_dismiss_dialogs.forEach { try { it.dismiss() } catch (err: Exception) { /* Empty */ } }
            BackgroundScanService.start(this)
        }
        catch (err : Exception) {
            Log.e("pause", "$err")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.menu_options, menu)
        menu.findItem(R.id.menu_settings).intent = Intent(this, SettingsActivity::class.java)

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

        startPeriodicScan()

        successful_init = true
    }

    private fun initSamplerSelector() {
        measure_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, available_samplers.map{ it.label })
        measure_spinner.setSelection(0, false) // Selects the first element of the spinner (before adding the listener)
        measure_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                curr_sampler = available_samplers[position]
                lifecycleScope.launch { map_fragment.changeViewModel(curr_sampler.view_model) }

                fab_query.visibility = if (curr_sampler.view_model is QueryableMeasureViewModel) View.VISIBLE else View.INVISIBLE

                // Disable measure fab if the sampler is already measuring
                if (available_samplers[position].currently_measuring) { disableMeasureFab() }
                else { enableMeasureFab() }
            }
        }
    }

    private fun initNewMeasureFAB() {
        fab_start_measure.setOnClickListener {
            userTriggeredMeasure()
            resetPeriodicScan()
        }
    }

    private fun initFilterFAB() {
        fab_query.setOnClickListener {
            if (curr_sampler.view_model !is QueryableMeasureViewModel) { return@setOnClickListener }
            val queryable_model = curr_sampler.view_model as QueryableMeasureViewModel

            lifecycleScope.launch(Dispatchers.IO) {
                val queries = listOf(Pair(getString(R.string.remove_filter), null)) + queryable_model.listQueries()
                val items : ArrayList<CharSequence> = queries.map{ it.first }.toCollection(ArrayList())

                withContext(Dispatchers.Main) {
                    MeasureFilterDialog(items){ index ->
                        lifecycleScope.launch {
                            queryable_model.changeQuery(queries[index].second)
                            map_fragment.refreshMap()
                        }
                    }.show(supportFragmentManager, MeasureFilterDialog.TAG)
                }
            }
        }
    }

    private fun initTileChangeListener() {
        map_fragment.current_tile.observe(this) {
            if (pref_manager.getBoolean("tile_change_scan", false)) {
                autoTriggeredMeasure()
                resetPeriodicScan()
            }
        }
    }


    /**
     * Measure
     * */

    private suspend fun measureOne(sampler: SamplerHandler) {
        if (sampler.currently_measuring) { return } // The sampler is already measuring
        sampler.currently_measuring = true
        var error : Exception? = null

        if (curr_sampler.view_model == sampler.view_model) {
            withContext(Dispatchers.Main) { disableMeasureFab() }
        }

        try {
            sampler.view_model.measure()
        }
        catch (err: Exception) { error = err }

        // Execute only if, at the end of the measure, the selected sampler is still this one (user may have changed the sampler in the meantime)
        if (curr_sampler.view_model == sampler.view_model) {
            withContext(Dispatchers.Main) {
                map_fragment.refreshMap()
                enableMeasureFab()
            }
        }

        sampler.currently_measuring = false

        if (error != null) { throw error }
    }

    private fun userTriggeredMeasure() {
        if (curr_sampler.checks_before_measure()) {
            lifecycleScope.launch(Dispatchers.IO) {
                permissions_check_and_measure_current.launch(curr_sampler.permissions)
            }
        }
    }

    private fun autoTriggeredMeasure() {
        available_samplers.forEach {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    measureOne(it)
                }
                catch (err: Exception) {
                    Log.e("measure", "Auto triggered measure (${it.view_model}) $err")
                }
            }
        }
    }


    /**
     * Measure FAB
     * */

    private fun disableMeasureFab() {
        fab_start_measure.isClickable = false
        fab_start_measure.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)

        // Loading animation handling
        val loading_res = intArrayOf(
            R.drawable.loading1,
            R.drawable.loading2,
            R.drawable.loading3,
            R.drawable.loading4,
            R.drawable.loading5,
            R.drawable.loading6,
            R.drawable.loading7,
        )
        fab_start_measure_loading_anim = ValueAnimator.ofInt(0, loading_res.size - 1).setDuration(500)
        fab_start_measure_loading_anim?.interpolator = LinearInterpolator()
        fab_start_measure_loading_anim?.addListener(object : AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                fab_start_measure_loading_anim?.start()
            }
        })
        fab_start_measure_loading_anim?.addUpdateListener { animation ->
            fab_start_measure.setImageResource( loading_res[animation.animatedValue as Int] )
        }
        fab_start_measure_loading_anim?.start()
    }

    private fun enableMeasureFab() {
        fab_start_measure.isClickable = true

        val primary_color = TypedValue()
        theme.resolveAttribute(androidx.transition.R.attr.colorPrimary, primary_color, true)
        fab_start_measure.backgroundTintList = ColorStateList.valueOf(primary_color.data)
        if (fab_start_measure_loading_anim != null) {
            fab_start_measure_loading_anim?.removeAllListeners()
            fab_start_measure_loading_anim?.cancel()
            fab_start_measure_loading_anim = null
        }
        fab_start_measure.setImageResource(android.R.drawable.ic_input_add)
    }



    /**
     * Foreground periodic measures
     * */

    private fun startPeriodicScan() {
        if (pref_manager.getBoolean("periodic_scan", false)) {
            val delay = pref_manager.getString("periodic_scan_interval", "60")!!.toLong()

            periodic_scan_handler.postDelayed({
                autoTriggeredMeasure()
                resetPeriodicScan()
            }, delay * 1000)
        }
    }

    private fun stopPeriodicScan() {
        periodic_scan_handler.removeCallbacksAndMessages(null)
    }

    private fun resetPeriodicScan() {
        stopPeriodicScan()
        startPeriodicScan()
    }
}