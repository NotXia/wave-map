package com.example.wavemap.ui.main

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.wavemap.R
import com.example.wavemap.ui.main.viewmodels.MeasureViewModel
import com.example.wavemap.utilities.LocationUtils
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*
import kotlin.math.*


class WaveHeatMapFragment(private var view_model : MeasureViewModel) : Fragment() {

    private lateinit var google_map : GoogleMap

    private var tile_length_meters = 500.0
    private lateinit var center : LatLng
    private lateinit var top_left_center : LatLng

    fun changeViewModel(view_model : MeasureViewModel) {
        this.view_model = view_model
        refreshMap()
    }

    fun refreshMap() {
        if (!this::google_map.isInitialized) { return }
        google_map.clear()
        fillWithTiles()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.wave_map) as SupportMapFragment?
        mapFragment?.getMapAsync { google_map ->
            this.google_map = google_map

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                initMap()
            }
        }
    }


    private fun initMap() {
        if ( ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            throw SecurityException("Missing ACCESS_FINE_LOCATION permission")
        }

        initLocationRetriever()

        google_map.isMyLocationEnabled = true;
        GlobalScope.launch {
            var current_location : LatLng? = null

            // Location service may still not have the current position
            for (i in 0..20) {
                try {
                    current_location = LocationUtils.getCurrent(activity as Activity)
                    break
                }
                catch (err: RuntimeException) { delay(500) }
            }
            if (current_location == null) { throw RuntimeException("Cannot retrieve location") }

            withContext(Dispatchers.Main) {
                google_map.animateCamera(CameraUpdateFactory.newLatLngZoom(current_location, 18f))
                center = current_location

                google_map.setOnCameraIdleListener {
                    updateTilesLength()
                    refreshMap()
                }

                view_model.last_measure_time.observe(viewLifecycleOwner, Observer<Long> { _ ->
                    refreshMap()
                })
            }
        }
    }

    private fun initLocationRetriever() {
        if ( ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            throw SecurityException("Missing ACCESS_FINE_LOCATION permission")
        }

        LocationServices.getFusedLocationProviderClient(requireActivity()).requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200).apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build(),
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    // TODO update heat map
                }
            },
            null
        )
    }

    private fun metersToLatitudeOffset(meters: Double) : Double {
        val degrees_per_1_meter = 1.0/111320.0
        return meters * degrees_per_1_meter
    }

    private fun metersToLongitudeOffset(meters: Double, latitude: Double) : Double {
        // val degrees_per_1_meter = 1.0/(40075000.0 * cos(latitude) / 360)
        val degrees_per_1_meter = 1.0/(40075000.0 * cos(0.0) / 360)
        return meters * degrees_per_1_meter
    }

    private fun scaleToRange(value: Double, source_range: Pair<Double, Double>, target_range: Pair<Double, Double>) : Double {
        var real_source = source_range
        var real_value = value

        // Range extremes have to be inverted and value rescaled
        if (source_range.first > source_range.second) {
            real_value = if (value > (source_range.first - source_range.second) / 2) {
                value + (source_range.first - source_range.second) - 2 * (value - source_range.second)
            }
            else {
                value - (source_range.first - source_range.second) + 2 * (source_range.first - value)
            }
            real_source = Pair(source_range.second, source_range.first)
        }

        if (real_value <= real_source.first) { return target_range.first }
        if (real_value >= real_source.second) { return target_range.second }

        val source_distance = (real_source.second - real_source.first)
        val target_distance = (target_range.second - target_range.first)
        return ( ( ((real_value - real_source.first) * target_distance) / source_distance ) + target_range.first )
    }

    private fun drawTile(top_left_corner: LatLng) {
        val top_right_corner = LatLng(top_left_corner.latitude, top_left_corner.longitude+metersToLongitudeOffset(tile_length_meters, top_left_corner.latitude))
        val bottom_right_corner = LatLng(top_left_corner.latitude-metersToLatitudeOffset(tile_length_meters), top_left_corner.longitude+metersToLongitudeOffset(tile_length_meters, top_left_corner.latitude))
        val bottom_left_corner = LatLng(top_left_corner.latitude-metersToLatitudeOffset(tile_length_meters), top_left_corner.longitude)

        lifecycleScope.launch(Dispatchers.IO) {
            val tile_average : Double? = view_model.averageOf(top_left_corner, bottom_right_corner)
            if (view_model.values_scale == null) { return@launch }

            var color = ColorUtils.setAlphaComponent(0, 0)
            if (tile_average != null) {
                val hue = scaleToRange(tile_average!!, view_model.values_scale!!, Pair(0.0, 150.0)) // 0 -> Red | 150 -> Green
                color = ColorUtils.setAlphaComponent(ColorUtils.HSLToColor(floatArrayOf(hue.toFloat(), 1f, 0.6f)), 100);
            }

            withContext(Dispatchers.Main) {
                google_map.addPolygon(
                    PolygonOptions()
                        .clickable(false)
                        .fillColor(color)
                        .strokeWidth(1.2f)
                        .strokeColor(Color.argb(50, 0, 0, 0))
                        .add( top_left_corner,  top_right_corner,  bottom_right_corner,  bottom_left_corner )
                )
            }
        }
    }

    /**
     * Fills the screen with tiles offsetted from the center
     * */
    private fun fillWithTiles() {
        // TODO handle wrap up zone
        val top_left_visible = LatLng(google_map.projection.visibleRegion.latLngBounds.northeast.latitude, google_map.projection.visibleRegion.latLngBounds.southwest.longitude)
        val bottom_right_visible = LatLng(google_map.projection.visibleRegion.latLngBounds.southwest.latitude, google_map.projection.visibleRegion.latLngBounds.northeast.longitude)

        // Difference from the current top-left corner and the center
        val lat_diff_to_center = top_left_visible.latitude - top_left_center.latitude
        val lon_diff_to_center = top_left_visible.longitude - top_left_center.longitude
        // Number of tiles to skip with respect to latitude and longitude (with one extra tile for tolerance)
        var lat_offset_times = ceil( (lat_diff_to_center / metersToLatitudeOffset(tile_length_meters)) + 1 )
        var lon_offset_times = floor( (lon_diff_to_center / metersToLongitudeOffset(tile_length_meters, top_left_visible.latitude)) - 1 )

        // Top-left corner from which begin the generation of tiles
        val real_top_left_corner = LatLng(
            top_left_center.latitude + (metersToLatitudeOffset(tile_length_meters * lat_offset_times)),
            top_left_center.longitude + (metersToLongitudeOffset(tile_length_meters * lon_offset_times, top_left_visible.latitude))
        )

        // Fill the screen with tiles
        var current_tile = real_top_left_corner
        while (current_tile.latitude >= bottom_right_visible.latitude) {
            while (current_tile.longitude <= bottom_right_visible.longitude) {
                drawTile(current_tile)
                current_tile = LatLng( current_tile.latitude, current_tile.longitude + metersToLongitudeOffset(tile_length_meters, current_tile.latitude) )
            }
            current_tile = LatLng( current_tile.latitude - metersToLatitudeOffset(tile_length_meters), real_top_left_corner.longitude )
        }
    }

    private fun updateTilesLength() {
        val zoom_level = google_map.cameraPosition.zoom

        // Selects new tile length
        if (zoom_level < 5) {
            tile_length_meters = 500000.0
        }
        else if (5 <= zoom_level && zoom_level < 21) {
            tile_length_meters = 5.0 * (2.0).pow(20.0 - round(zoom_level) + 1.0)
        }
        else {
            tile_length_meters = 2.0
        }

        // Realigns top-left corner of the center tile
        top_left_center = LatLng(
            center.latitude + metersToLatitudeOffset(tile_length_meters/2),
            center.longitude - metersToLongitudeOffset(tile_length_meters/2, center.latitude + metersToLatitudeOffset(tile_length_meters/2))
        )
    }
}