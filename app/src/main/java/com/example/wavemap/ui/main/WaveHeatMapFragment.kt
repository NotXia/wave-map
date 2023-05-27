package com.example.wavemap.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.*
import com.example.wavemap.utilities.Constants

class WaveHeatMapFragment(private var view_model : MeasureViewModel) : Fragment() {

    private lateinit var google_map : GoogleMap

    private var tile_length_meters = 500.0
    private lateinit var center : LatLng
    private lateinit var top_left_center : LatLng

    val current_tile: MutableLiveData<LatLng> by lazy {
        MutableLiveData<LatLng>()
    }

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

        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                initLocationRetriever()
                var current_location : LatLng = LocationUtils.getCurrent(activity as Activity)

                google_map.isMyLocationEnabled = true;
                google_map.moveCamera(CameraUpdateFactory.newLatLngZoom(current_location, 18f))
                center = current_location

                google_map.setOnCameraIdleListener {
                    updateTilesLength()
                    refreshMap()
                }
            }
        }
    }

    private suspend fun initLocationRetriever() : Unit = suspendCoroutine { cont ->
        GlobalScope.launch {
            if ( ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                return@launch cont.resumeWithException( SecurityException("Missing ACCESS_FINE_LOCATION permission") )
            }

            val location_options = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200).apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build()
            val location_callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (locationResult.lastLocation == null) { return }

                    val location = locationResult.lastLocation as Location
                    val tile = getReferenceTileContaining(LatLng(location.latitude, location.longitude))

                    if (current_tile.value != tile) {
                        current_tile.value = tile
                    }
                }
            }
            val fused_location_provider = LocationServices.getFusedLocationProviderClient(requireActivity())

            if (LocationUtils.getCurrent(activity as Activity) != null) { // A location is already available
                // There is no need to wait for a LocationResult, setup for location updates and exit immediately
                fused_location_provider.requestLocationUpdates(location_options, location_callback, Looper.getMainLooper())
                cont.resume(Unit)
            }
            else {
                // Since there is no location available, wait for a location before exiting
                fused_location_provider.requestLocationUpdates(
                    location_options,
                    object : LocationCallback() {
                        @SuppressLint("MissingPermission")
                        override fun onLocationResult(locationResult: LocationResult) {
                            // When a location is available, resets the location updates callback
                            fused_location_provider.removeLocationUpdates(this)
                            fused_location_provider.requestLocationUpdates(location_options, location_callback, Looper.getMainLooper())
                            cont.resume(Unit)
                        }
                    },
                    Looper.getMainLooper()
                )
            }
        }
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

    /* Scales the value of an interval to another */
    private fun scaleToInterval(value: Double, source_range: Pair<Double, Double>, target_range: Pair<Double, Double>) : Double {
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

    /* Scales a value in a discrete interval */
    private fun scaleToRange(value: Double, range: Pair<Double, Double>, range_size: Int) : Double {
        if (range_size == 1) { return range.second }

        val range_value : Double = round(abs(range.second - range.first) / (range_size-1))
        var out = value + range_value/2
        out -= out % range_value

        if (out <= range.first) { return range.first }
        if (out >= range.second) { return range.second }
        return out
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
                var hue = scaleToInterval(tile_average!!, view_model.values_scale!!, Constants.HUE_MEASURE_RANGE)
                hue = scaleToRange(hue, Constants.HUE_MEASURE_RANGE, view_model.range_size)
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

        val real_top_left_corner = getReferenceTileContaining(top_left_visible)

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

    private fun getReferenceTileContaining(position: LatLng) : LatLng {
        // Difference from the current top-left corner and the center
        val lat_diff_to_center = position.latitude - top_left_center.latitude
        val lon_diff_to_center = position.longitude - top_left_center.longitude

        // Number of tiles to skip with respect to latitude and longitude
        var lat_offset_times = ceil( (lat_diff_to_center / metersToLatitudeOffset(tile_length_meters)) )
        var lon_offset_times = floor( (lon_diff_to_center / metersToLongitudeOffset(tile_length_meters, position.latitude)) )

        // Top-left corner from which begin the generation of tiles
        return LatLng(
            top_left_center.latitude + (metersToLatitudeOffset(tile_length_meters * lat_offset_times)),
            top_left_center.longitude + (metersToLongitudeOffset(tile_length_meters * lon_offset_times, position.latitude))
        )
    }

    private fun updateTilesLength() {
        val zoom_level = google_map.cameraPosition.zoom

        // Selects new tile length
        if (zoom_level < 5) {
            tile_length_meters = 1000000.0
        }
        else if (zoom_level < 21) {
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