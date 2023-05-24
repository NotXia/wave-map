package com.example.wavemap.ui.main.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.Constants
import com.google.android.gms.maps.model.LatLng

abstract class MeasureViewModel(application : Application) : AndroidViewModel(application) {
    abstract var sampler : WaveSampler
    abstract val preferences_prefix : String
    abstract val default_scale : Pair<Double, Double>

    var values_scale : Pair<Double, Double>? = null
    var range_size : Int = Constants.RANGE_SIZE_DEFAULT
    var limit : Int? = null

    suspend fun measure() {
        sampler?.sampleAndStore()
    }

    suspend fun averageOf(top_left_corner: LatLng, bottom_right_corner: LatLng) : Double? {
        return sampler?.average(top_left_corner, bottom_right_corner, limit)
    }

    fun loadSettingsPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(getApplication())

        values_scale = Pair(
            preferences.getString("${preferences_prefix}_range_bad", "${default_scale.first}")!!.toDouble(),
            preferences.getString("${preferences_prefix}_range_good", "${default_scale.second}")!!.toDouble()
        )
        limit = preferences.getString("${preferences_prefix}_past_limit", "1")!!.toInt()
        range_size = preferences.getString("${preferences_prefix}_range_size", "${Constants.RANGE_SIZE_DEFAULT}")!!.toInt()
    }
}
