package com.example.wavemap.ui.main

import android.Manifest
import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import com.example.wavemap.R
import com.example.wavemap.ui.main.viewmodels.*
import com.example.wavemap.ui.settings.SettingsActivity
import com.example.wavemap.services.BackgroundScanService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private class SamplerHandler(
        val view_model : MeasureViewModel,
        val label : String,
        var currently_measuring : Boolean = false // To prevent multiple measure requests
    )

    private lateinit var curr_model : MeasureViewModel
    private lateinit var map_fragment : WaveHeatMapFragment
    private lateinit var pref_manager : SharedPreferences


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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        available_samplers = arrayOf(
            SamplerHandler(wifi_model, resources.getString(R.string.wifi)),
            SamplerHandler(lte_model, resources.getString(R.string.lte)),
            SamplerHandler(noise_model, resources.getString(R.string.noise)),
            SamplerHandler(bluetooth_model, resources.getString(R.string.bluetooth)),
        )

        measure_spinner = findViewById(R.id.spinner_sampler)
        fab_start_measure = findViewById(R.id.btn_scan)
        fab_query = findViewById(R.id.btn_query)

        curr_model = wifi_model
        map_fragment = WaveHeatMapFragment(curr_model)

        pref_manager = PreferenceManager.getDefaultSharedPreferences(baseContext)

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { is_granted: Map<String, Boolean> ->
            if (!is_granted.values.all{ granted -> granted }) {
                // TODO error handling
                return@registerForActivityResult
            }

            supportFragmentManager.commit {
                replace(R.id.fragment_container_map, map_fragment)
            }

            // Measure type spinner
            measure_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, available_samplers.map{ it.label })
            measure_spinner.setSelection(0, false) // Selects the first element of the spinner (before adding the listener)
            measure_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) { }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    curr_model = available_samplers[position].view_model
                    map_fragment.changeViewModel(curr_model)

                    if (curr_model is QueryableMeasureViewModel) {
                        fab_query.visibility = View.VISIBLE
                    }
                    else {
                        fab_query.visibility = View.INVISIBLE
                    }

                    // Disable measure fab if the sampler is already measuring
                    if (available_samplers[position].currently_measuring) {
                        disableMeasureFab()
                    }
                    else {
                        enableMeasureFab()
                    }
                }
            }

            // New measurement fab
            fab_start_measure.setOnClickListener {
                userTriggeredMeasure()
                resetPeriodicScan()
            }

            // Filter measures
            fab_query.setOnClickListener {
                if (curr_model !is QueryableMeasureViewModel) { return@setOnClickListener }
                val queryable_model = curr_model as QueryableMeasureViewModel

                GlobalScope.launch {
                    val queries = ( listOf(Pair(getString(R.string.remove_filter), null)) + queryable_model.listQueries() )
                    val items : Array<CharSequence> = queries.map{ it.first }.toTypedArray()

                    withContext(Dispatchers.Main) {
                        val dialog_builder : AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)

                        dialog_builder.setTitle(getString(R.string.filter_measures))
                        dialog_builder.setItems(items) { _, index ->
                            queryable_model.changeQuery(queries[index].second)
                            map_fragment.refreshMap()
                        }
                        dialog_builder.create().show()
                    }
                }
            }

            // Listen for tile changes
            map_fragment.current_tile.observe(this) { new_tile ->
                if (pref_manager.getBoolean("tile_change_scan", false)) {
                    autoTriggeredMeasure()
                    resetPeriodicScan()
                }
            }

            startPeriodicScan()

        }.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

    }

    override fun onResume() {
        super.onResume()

        try {
            curr_model.loadSettingsPreferences()
            map_fragment.refreshMap()
            startPeriodicScan()
            BackgroundScanService.stop(this)
        }
        catch (err : Exception) {
            Log.e("resume", "$err")
            // TODO: Handle error
        }
    }

    override fun onPause() {
        super.onPause()
        stopPeriodicScan()
        BackgroundScanService.start(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.menu_options, menu)
        menu.findItem(R.id.menu_settings).intent = Intent(this, SettingsActivity::class.java)

        return true
    }

    private suspend fun measureAll() {
        available_samplers.forEach {
            // The sampler is already measuring
            if (it.currently_measuring) { return }

            GlobalScope.launch {
                it.currently_measuring = true

                if (curr_model == it.view_model) {
                    withContext(Dispatchers.Main) { disableMeasureFab() }
                }

                it.view_model.measure()

                // Execute only if, at the end of the measure, the selected sampler is still this one (user may have changed the sampler type in the meantime)
                if (curr_model == it.view_model) {
                    withContext(Dispatchers.Main) {
                        map_fragment.refreshMap()
                        enableMeasureFab()
                    }
                }

                it.currently_measuring = false
            }
        }
    }

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
        theme.resolveAttribute(androidx.transition.R.attr.colorPrimary, primary_color, true);
        fab_start_measure.backgroundTintList = ColorStateList.valueOf(primary_color.data)
        if (fab_start_measure_loading_anim != null) {
            fab_start_measure_loading_anim?.removeAllListeners()
            fab_start_measure_loading_anim?.cancel()
            fab_start_measure_loading_anim = null
        }
        fab_start_measure.setImageResource(android.R.drawable.ic_input_add)
    }


    private fun userTriggeredMeasure() {
        GlobalScope.launch {
            try {
                measureAll()
            } catch (err: Exception) {
                // TODO Error handling
                withContext(Dispatchers.Main) {
                    Toast.makeText(baseContext, ":(", Toast.LENGTH_SHORT).show()
                }
                Log.e("measure", "User triggered measure $err")
            }
        }
    }

    private fun autoTriggeredMeasure() {
        GlobalScope.launch {
            try {
                measureAll()
            } catch (err: Exception) {
                Log.e("measure", "Auto triggered measure $err")
            }
        }
    }

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