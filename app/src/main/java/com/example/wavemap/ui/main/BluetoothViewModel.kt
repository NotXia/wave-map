package com.example.wavemap.ui.main

import android.app.Application
import androidx.room.Room
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.measures.samplers.BluetoothSampler
import com.example.wavemap.measures.samplers.WiFiSampler

class BluetoothViewModel(application : Application) : MeasureViewModel(application) {
    override lateinit var sampler : WaveSampler
    private val db : WaveDatabase

    init {
        db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, "wave").build()
        sampler = BluetoothSampler(application.applicationContext, null, db)
        values_scale = Pair(-90.0, -10.0)
        limit = 1
    }

    fun changeDeviceName(device_name : String?) {
        sampler = WiFiSampler(getApplication<Application>().applicationContext, device_name, db)
    }
}