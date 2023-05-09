package com.example.wavemap.ui.main

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.wavemap.R
import com.example.wavemap.ui.main.viewmodels.*
import com.example.wavemap.ui.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var curr_model : MeasureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wifi_model : WiFiViewModel by viewModels()
        val lte_model : LTEViewModel by viewModels()
        val noise_model : NoiseViewModel by viewModels()
        val bluetooth_model : BluetoothViewModel by viewModels()
        curr_model = wifi_model

        val spinner_options = arrayOf(
            Pair(resources.getString(R.string.wifi), wifi_model),
            Pair(resources.getString(R.string.lte), lte_model),
            Pair(resources.getString(R.string.noise), noise_model),
            Pair(resources.getString(R.string.bluetooth), bluetooth_model)
        )

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { is_granted: Map<String, Boolean> ->
            if (is_granted.values.all{ granted -> granted }) {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container_map, WaveHeatMapFragment(curr_model))
                }
            } else {
                // TODO error handling
            }
        }.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_SCAN
            )
        )


        val spinner : Spinner = findViewById(R.id.spinner_sampler)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spinner_options.map{ it.first })
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                curr_model = spinner_options[position].second
                (supportFragmentManager.findFragmentById(R.id.fragment_container_map) as WaveHeatMapFragment?)?.changeViewModel(curr_model)
            }
        }

        findViewById<View>(R.id.btn_scan).setOnClickListener(View.OnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    curr_model.measure()
                    withContext(Dispatchers.Main) {
                        (supportFragmentManager.findFragmentById(R.id.fragment_container_map) as WaveHeatMapFragment?)?.refreshMap()
                    }
                }
                catch (err : Exception) {
                    withContext(Dispatchers.Main) {
                        // TODO Error handling
                        Toast.makeText(baseContext, ":(", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        curr_model.loadSettingsPreferences()
        (supportFragmentManager.findFragmentById(R.id.fragment_container_map) as WaveHeatMapFragment?)?.refreshMap()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.menu_options, menu)
        menu.findItem(R.id.menu_settings).intent = Intent(this, SettingsActivity::class.java)

        return true
    }
}