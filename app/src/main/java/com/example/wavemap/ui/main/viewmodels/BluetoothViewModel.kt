package com.example.wavemap.ui.main.viewmodels

import android.app.Application
import androidx.room.Room
import com.example.wavemap.R
import com.example.wavemap.db.BSSIDType
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.measures.samplers.BluetoothSampler
import com.example.wavemap.measures.samplers.WiFiSampler

class BluetoothViewModel(application : Application) : QueryableMeasureViewModel(application) {
    override lateinit var sampler : WaveSampler
    override val preferences_prefix: String = "bluetooth"
    override val default_scale: Pair<Double, Double> = Pair(
        getApplication<Application>().resources.getString(R.string.bluetooth_default_range_bad).toDouble(),
        getApplication<Application>().resources.getString(R.string.bluetooth_default_range_good).toDouble()
    )
    private val db : WaveDatabase

    init {
        db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, "wave").build()
        sampler = BluetoothSampler(application.applicationContext, null, db)
        loadSettingsPreferences()
    }

    override fun changeQuery(new_query: String?) {
        sampler = BluetoothSampler(getApplication<Application>().applicationContext, new_query, db)
    }

    override fun listQueries() : List<Pair<String, String>> {
        val bt_list = db.bssidDAO().getList(BSSIDType.BLUETOOTH)
        return bt_list.map { Pair(it.ssid, it.bssid) }
    }
}