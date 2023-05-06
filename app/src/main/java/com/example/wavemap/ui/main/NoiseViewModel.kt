package com.example.wavemap.ui.main

import android.app.Application
import androidx.room.Room
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.samplers.NoiseSampler
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.measures.samplers.WiFiSampler

class NoiseViewModel(application : Application) : MeasureViewModel(application) {
    override lateinit var sampler : WaveSampler
    private val db : WaveDatabase

    init {
        db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, "wave").build()
        sampler = NoiseSampler(application.applicationContext, db)
        values_scale = Pair(130.0, 10.0)
        limit = 1
    }

    fun changeBSSID(bssid : String?) {
        sampler = WiFiSampler(getApplication<Application>().applicationContext, bssid, db)
    }
}