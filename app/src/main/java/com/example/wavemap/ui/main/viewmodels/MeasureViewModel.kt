package com.example.wavemap.ui.main.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.Constants
import com.google.android.gms.maps.model.LatLng

abstract class MeasureViewModel(application : Application) : AndroidViewModel(application) {
    abstract var sampler : WaveSampler
    abstract val preferences_prefix : String
    abstract val default_scale : Pair<Double, Double>
    abstract val measure_unit : String

    var values_scale : Pair<Double, Double>? = null
    var color_range_size : Int = Constants.RANGE_SIZE_DEFAULT
    var limit : Int? = null
    var get_shared : Boolean = true

    suspend fun measure() {
        sampler.sampleAndStore()
    }

    suspend fun averageOf(top_left_corner: LatLng, bottom_right_corner: LatLng) : Double? {
        // The average of a tile is obtained as the average of its sub-tiles
        val latitude_step = (top_left_corner.latitude - bottom_right_corner.latitude) / Constants.TILE_AVERAGE_STEPS
        val longitude_step = (bottom_right_corner.longitude - top_left_corner.longitude) / Constants.TILE_AVERAGE_STEPS
        val measures = mutableListOf<Double>()
        var curr_top_left = top_left_corner

        for (i in 0 until Constants.TILE_AVERAGE_STEPS) {
            for (j in 0 until Constants.TILE_AVERAGE_STEPS) {
                val tile_average = sampler.average(
                    curr_top_left,
                    LatLng(curr_top_left.latitude-latitude_step, curr_top_left.longitude+longitude_step),
                    limit,
                    get_shared
                )
                if (tile_average != null) { measures.add(tile_average) }

                curr_top_left = LatLng(curr_top_left.latitude, curr_top_left.longitude+longitude_step)
            }

            curr_top_left = LatLng(curr_top_left.latitude-latitude_step, top_left_corner.longitude)
        }

        return if (measures.isEmpty()) null else measures.average()
    }

    fun loadSettingsPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(getApplication())

        values_scale = Pair(
            preferences.getString("${preferences_prefix}_range_bad", "${default_scale.first}")!!.toDouble(),
            preferences.getString("${preferences_prefix}_range_good", "${default_scale.second}")!!.toDouble()
        )
        limit = preferences.getString("${preferences_prefix}_past_limit", "1")!!.toInt()
        color_range_size = preferences.getString("${preferences_prefix}_range_size", "${Constants.RANGE_SIZE_DEFAULT}")!!.toInt()
        get_shared = preferences.getBoolean("use_shared", true)
    }
}
