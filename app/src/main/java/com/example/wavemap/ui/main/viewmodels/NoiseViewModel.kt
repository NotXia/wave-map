package com.example.wavemap.ui.main.viewmodels

import android.app.Application
import androidx.room.Room
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.samplers.NoiseSampler
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.Constants

class NoiseViewModel(application : Application) : MeasureViewModel(application) {
    override lateinit var sampler : WaveSampler
    override val preferences_prefix: String = "noise"
    override val default_scale: Pair<Double, Double> = Pair(
        Constants.NOISE_DEFAULT_RANGE_BAD,
        Constants.NOISE_DEFAULT_RANGE_GOOD
    )
    override val measure_unit = "dB"
    private val db : WaveDatabase

    init {
        db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, Constants.DATABASE_NAME).build()
        sampler = NoiseSampler(application.applicationContext, db)
        loadSettingsPreferences()
    }
}