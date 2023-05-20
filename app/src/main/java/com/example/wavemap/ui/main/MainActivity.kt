package com.example.wavemap.ui.main

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.wavemap.R
import com.example.wavemap.ui.main.viewmodels.*
import com.example.wavemap.ui.settings.SettingsActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private lateinit var curr_model : MeasureViewModel
    private lateinit var map_fragment : WaveHeatMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wifi_model : WiFiViewModel by viewModels()
        val lte_model : LTEViewModel by viewModels()
        val noise_model : NoiseViewModel by viewModels()
        val bluetooth_model : BluetoothViewModel by viewModels()

        val spinner_options = arrayOf(
            Pair(resources.getString(R.string.wifi), wifi_model),
            Pair(resources.getString(R.string.lte), lte_model),
            Pair(resources.getString(R.string.noise), noise_model),
            Pair(resources.getString(R.string.bluetooth), bluetooth_model)
        )

        val measure_spinner : Spinner = findViewById(R.id.spinner_sampler)
        val fab_start_measure : FloatingActionButton = findViewById(R.id.btn_scan)
        val fab_query : FloatingActionButton = findViewById(R.id.btn_query)

        curr_model = wifi_model
        map_fragment = WaveHeatMapFragment(curr_model)


        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { is_granted: Map<String, Boolean> ->
            if (!is_granted.values.all{ granted -> granted }) {
                // TODO error handling
                return@registerForActivityResult
            }

            supportFragmentManager.commit {
                replace(R.id.fragment_container_map, map_fragment)
            }
        }.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )


        // Measure type spinner
        measure_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spinner_options.map{ it.first })
        measure_spinner.setSelection(0, false) // Selects the first element of the spinner (before adding the listener)
        measure_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                curr_model = spinner_options[position].second
                map_fragment.changeViewModel(curr_model)

                if (curr_model is QueryableMeasureViewModel) {
                    fab_query.visibility = View.VISIBLE
                }
                else {
                    fab_query.visibility = View.INVISIBLE
                }
            }
        }

        // New measurement
        fab_start_measure.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    curr_model.measure()
                    withContext(Dispatchers.Main) { map_fragment.refreshMap() }
                } catch (err: Exception) {
                    withContext(Dispatchers.Main) {
                        // TODO Error handling
                        Toast.makeText(baseContext, ":(", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
    }

    override fun onResume() {
        super.onResume()

        try {
            curr_model.loadSettingsPreferences()
            map_fragment.refreshMap()
        }
        catch (err : Exception) {
            // TODO: Handle error
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.menu_options, menu)
        menu.findItem(R.id.menu_settings).intent = Intent(this, SettingsActivity::class.java)

        return true
    }

}