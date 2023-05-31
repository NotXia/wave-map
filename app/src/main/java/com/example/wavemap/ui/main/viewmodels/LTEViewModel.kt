package com.example.wavemap.ui.main.viewmodels

import android.app.Application
import androidx.room.Room
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.measures.samplers.LTESampler
import com.example.wavemap.utilities.Constants

class LTEViewModel(application : Application) : MeasureViewModel(application) {
    override lateinit var sampler : WaveSampler
    override val preferences_prefix: String = "lte"
    override val default_scale: Pair<Double, Double> = Pair(
        Constants.LTE_DEFAULT_RANGE_BAD,
        Constants.LTE_DEFAULT_RANGE_GOOD
    )
    override val measure_unit = "dBm"
    private val db : WaveDatabase

    init {
        db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, Constants.DATABASE_NAME).build()
        sampler = LTESampler(application.applicationContext, db)
        loadSettingsPreferences()
    }
}