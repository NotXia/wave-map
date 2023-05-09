package com.example.wavemap.ui.main

import android.app.Application
import androidx.room.Room
import com.example.wavemap.R
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.measures.samplers.WiFiSampler

class WiFiViewModel(application : Application) : MeasureViewModel(application) {
    override lateinit var sampler : WaveSampler
    override val preferences_prefix: String = "wifi"
    override val default_scale: Pair<Double, Double> = Pair(
        getApplication<Application>().resources.getString(R.string.wifi_default_range_bad).toDouble(),
        getApplication<Application>().resources.getString(R.string.wifi_default_range_good).toDouble()
    )
    private val db : WaveDatabase

    init {
        db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, "wave").build()
        sampler = WiFiSampler(application.applicationContext, null, db)
        loadSettingsPreferences()
    }

    fun changeBSSID(bssid : String?) {
        sampler = WiFiSampler(getApplication<Application>().applicationContext, bssid, db)
    }
}