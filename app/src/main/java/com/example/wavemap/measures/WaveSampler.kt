package com.example.wavemap.measures

import com.google.android.gms.maps.model.LatLng

abstract class WaveSampler {
    /**
     * Measures the current value(s)
     * */
    abstract suspend fun sample() : List<WaveMeasure>

    /**
     * Saves some measures
     * */
    abstract suspend fun store(measures: List<WaveMeasure>)

    /**
     * Retrieves the measurements in a square area defined by the coordinates of its top-left and bottom-right corner
     * @param top_left_corner           Coordinates of the top-left corner of the square
     * @param bottom_right_corner       Coordinates of the bottom-right corner of the square
     * @param limit                     Number of past measurements to consider (all by default)
     * */
    abstract suspend fun retrieve(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?=null) : List<WaveMeasure>

    /**
     * Retrieves the average of the measurements in a square area defined by the coordinates of its top-left and bottom-right corner
     * @param top_left_corner           Coordinates of the top-left corner of the square
     * @param bottom_right_corner       Coordinates of the bottom-right corner of the square
     * @param limit                     Number of past measurements to consider (all by default)
     * */
    suspend fun average(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?=null) : Double? {
        val measurers = retrieve(top_left_corner, bottom_right_corner, limit)

        return if (measurers.isEmpty()) null else measurers.map { m -> m.value }.average()
    }

    suspend fun sampleAndStore() : List<WaveMeasure> {
        var measure : List<WaveMeasure> = sample()
        store(measure)

        return measure
    }
}