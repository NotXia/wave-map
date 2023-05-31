package com.example.wavemap.ui.main.viewmodels

import android.app.Application
import androidx.room.Room
import com.example.wavemap.db.BSSIDType
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.measures.samplers.WiFiSampler
import com.example.wavemap.utilities.Constants

class WiFiViewModel(application : Application) : QueryableMeasureViewModel(application) {
    override lateinit var sampler : WaveSampler
    override val preferences_prefix: String = "wifi"
    override val default_scale: Pair<Double, Double> = Pair(
        Constants.WIFI_DEFAULT_RANGE_BAD,
        Constants.WIFI_DEFAULT_RANGE_GOOD
    )
    override val measure_unit = "dBm"
    private val db : WaveDatabase

    init {
        db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, Constants.DATABASE_NAME).build()
        sampler = WiFiSampler(application.applicationContext, null, db)
        loadSettingsPreferences()
    }

    override fun changeQuery(new_query: String?) {
        sampler = WiFiSampler(getApplication<Application>().applicationContext, new_query, db)
    }

    override fun listQueries() : List<Pair<String, String>> {
        val wifi_list = db.bssidDAO().getList(BSSIDType.WIFI)
        return wifi_list.map { Pair(it.ssid, it.bssid) }
    }
}