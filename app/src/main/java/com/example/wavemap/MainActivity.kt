package com.example.wavemap

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WiFiSampler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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


        findViewById<Button>(R.id.btn_scan).setOnClickListener(View.OnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                model.measure()
            }
        })
    }

}