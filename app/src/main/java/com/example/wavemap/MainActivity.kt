package com.example.wavemap

import android.Manifest
import android.os.Bundle
import android.text.TextUtils.replace
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WiFiSampler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(applicationContext, WaveDatabase::class.java, "wave").build()
        var wifi = WiFiSampler(applicationContext, null, db)

        val model: MeasureViewModel by viewModels()
        model.sampler = wifi
        model.values_scale = Pair(-90.0, -30.0)
        model.limit = 5


        registerForActivityResult(ActivityResultContracts.RequestPermission()) { is_granted: Boolean ->
            if (is_granted) {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container_map, WaveHeatMapFragment())
                }
            } else {
                // TODO error handling
            }
        }.launch(Manifest.permission.ACCESS_FINE_LOCATION)


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