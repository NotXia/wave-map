package com.example.wavemap

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.wavemap.viewmodels.NoiseViewModel
import com.example.wavemap.viewmodels.WiFiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val model: WiFiViewModel by viewModels()
        val model: NoiseViewModel by viewModels()

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { is_granted: Map<String, Boolean> ->
            if (is_granted.values.all{ granted -> granted }) {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container_map, WaveHeatMapFragment(model))
                }
            } else {
                // TODO error handling
            }
        }.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO))


        findViewById<Button>(R.id.btn_scan).setOnClickListener(View.OnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    model.measure()
                }
                catch (err : Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(baseContext, ":(", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

}