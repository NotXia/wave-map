package com.example.wavemap.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.wavemap.R
import com.example.wavemap.ui.main.viewmodels.MeasureViewModel
import com.example.wavemap.utilities.Constants
import com.example.wavemap.utilities.Event
import com.example.wavemap.utilities.LocationUtils
import com.example.wavemap.utilities.LocationUtils.Companion.metersToLatitudeOffset
import com.example.wavemap.utilities.LocationUtils.Companion.metersToLongitudeOffset
import com.example.wavemap.utilities.Misc.Companion.scaleToInterval
import com.example.wavemap.utilities.Misc.Companion.scaleToRange
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


class WaveHeatMapFragment : Fragment() {

    private lateinit var pref_manager : SharedPreferences
    lateinit var view_model : MeasureViewModel
    private val is_initialized : Boolean
        get() = this::google_map.isInitialized && this::center.isInitialized && this::top_left_center.isInitialized && this::view_model.isInitialized

    private lateinit var google_map : GoogleMap
    private var tile_length_meters = 500.0
    private lateinit var center : LatLng
    private lateinit var top_left_center : LatLng
    private var current_zoom : Float = 18f
    private var drawn_tiles = mutableSetOf<LatLng>()

    private var map_update_job : Job? = null

    private var markers : MutableList<Marker> = mutableListOf() // Markers containing labels

    val current_tile: MutableLiveData<Event<LatLng>> = MutableLiveData(null)

    private val BUNDLE_CAMERA_LAT = "camera-lat"
    private val BUNDLE_CAMERA_LON = "camera-lon"
    private val BUNDLE_CAMERA_ZOOM = "camera-zoom"



    fun changeViewModel(view_model : MeasureViewModel) {
        this.view_model = view_model
        refreshMap()
    }

    fun refreshMap(need_clear: Boolean=true) {
        if (!is_initialized) { return }

        map_update_job?.cancel()
        map_update_job = lifecycleScope.launch(Dispatchers.IO) {
            if (need_clear) {
                withContext(Dispatchers.Main) { clearMap() }
            }
            fillScreenWithTiles()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_manager = PreferenceManager.getDefaultSharedPreferences(requireContext())

       (childFragmentManager.findFragmentById(R.id.wave_map) as SupportMapFragment).getMapAsync { google_map ->
            this.google_map = google_map

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                lifecycleScope.launch {
                    if (savedInstanceState != null) {
                        val restored_center = LatLng(savedInstanceState.getDouble(BUNDLE_CAMERA_LAT), savedInstanceState.getDouble(BUNDLE_CAMERA_LON))
                        current_zoom = savedInstanceState.getFloat(BUNDLE_CAMERA_ZOOM)
                        initMap(restored_center, current_zoom)
                    }
                    else {
                        initMap(null, current_zoom)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble(BUNDLE_CAMERA_LAT, google_map.cameraPosition.target.latitude)
        outState.putDouble(BUNDLE_CAMERA_LON, google_map.cameraPosition.target.longitude)
        outState.putFloat(BUNDLE_CAMERA_ZOOM, google_map.cameraPosition.zoom)
    }



    /**
     * Init
     * */

    private suspend fun initMap(to_center_coord: LatLng?=null, zoom_level: Float=18f) {
        if ( ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            throw SecurityException("Missing ACCESS_FINE_LOCATION permission")
        }

        initLocationRetriever()
        val current_location : LatLng = try {
            LocationUtils.getCurrent(requireActivity())
        } catch (err: Exception) {
            LatLng(0.0, 0.0)
        }

        withContext(Dispatchers.Main) {
            google_map.isMyLocationEnabled = true
            google_map.moveCamera( CameraUpdateFactory.newLatLngZoom(to_center_coord ?: current_location, zoom_level) )
            center = current_location
            updateTilesLength()

            google_map.setOnCameraMoveListener {
                // Removes labels when the tile size changes (to prevent the text to overflow from the tile)
                if (zoomToTileLength(google_map.cameraPosition.zoom) != tile_length_meters) {
                    markers.forEach { it.remove() }
                    markers = mutableListOf()
                }
            }

            google_map.setOnCameraIdleListener {
                if (google_map.cameraPosition.zoom != current_zoom) {
                    current_zoom = google_map.cameraPosition.zoom
                    clearMap()
                    updateTilesLength()
                }

                lifecycleScope.launch {
                    refreshMap(false)
                }
            }
        }
    }

    private suspend fun initLocationRetriever() : Unit = suspendCoroutine { cont ->
        if ( ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return@suspendCoroutine cont.resumeWithException( SecurityException("Missing ACCESS_FINE_LOCATION permission") )
        }

        lifecycleScope.launch {
            val location_options = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200).apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build()
            val location_callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (!is_initialized) { return } // In case a new location is available before initialization end
                    if (locationResult.lastLocation == null) { return }

                    val location = locationResult.lastLocation as Location
                    val tile = getReferenceTileContaining(LatLng(location.latitude, location.longitude))
                    if (current_tile.value?.peek() != tile) {
                        current_tile.postValue(Event(tile))
                    }
                }
            }
            val fused_location_provider = LocationServices.getFusedLocationProviderClient(requireActivity())

            try {
                LocationUtils.getCurrent(requireContext())
                // A location is already available, there is no need to wait for a LocationResult, setup for location updates and exit immediately
                fused_location_provider.requestLocationUpdates(location_options, location_callback, Looper.getMainLooper())
                cont.resume(Unit)
            }
            catch (err: RuntimeException) {
                // A location is not available, wait for a LocationResult
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



    /**
     * Drawing
     * */

    private fun clearMap() {
        drawn_tiles = mutableSetOf()
        google_map.clear()
    }

    private suspend fun drawTile(top_left_corner: LatLng) {
        // Checks if the tile is already drawn
        if (drawn_tiles.find{ abs(it.latitude-top_left_corner.latitude) < 1e-8 && abs(it.longitude-top_left_corner.longitude) < 1e-8 } != null) { return }
        if (view_model.values_scale == null) { return }

        val top_right_corner = LatLng(top_left_corner.latitude, top_left_corner.longitude+metersToLongitudeOffset(tile_length_meters, top_left_corner.latitude))
        val bottom_right_corner = LatLng(top_left_corner.latitude-metersToLatitudeOffset(tile_length_meters), top_left_corner.longitude+metersToLongitudeOffset(tile_length_meters, top_left_corner.latitude))
        val bottom_left_corner = LatLng(top_left_corner.latitude-metersToLatitudeOffset(tile_length_meters), top_left_corner.longitude)

        val tile_average : Double? = view_model.averageOf(top_left_corner, bottom_right_corner)
        var color = ColorUtils.setAlphaComponent(0, 0)

        if (tile_average != null) {
            var hue = scaleToInterval(tile_average, view_model.values_scale!!, Constants.HUE_MEASURE_RANGE)
            hue = scaleToRange(hue, Constants.HUE_MEASURE_RANGE, view_model.color_range_size)
            color = ColorUtils.setAlphaComponent(ColorUtils.HSLToColor(floatArrayOf(hue.toFloat(), 1f, 0.6f)), 100)
        }

        withContext(Dispatchers.Main) {
           google_map.addPolygon(
                PolygonOptions()
                    .clickable(false)
                    .fillColor(color)
                    .strokeWidth(1.5f)
                    .strokeColor(Color.argb(50, 0, 0, 0))
                    .add(top_left_corner, top_right_corner, bottom_right_corner, bottom_left_corner)
            )
            drawn_tiles.add(top_left_corner)

            // Adds a label with the value to the tile
            if (tile_average != null && pref_manager.getBoolean("show_tile_label", true)) {
                drawTileLabel(top_left_corner, "${tile_average.toInt()} ${view_model.measure_unit}")
            }
        }
    }

    private fun drawTileLabel(top_left_corner: LatLng, text: String) {
        val text_paint = Paint()
        text_paint.textSize = 35f
        var width = text_paint.measureText(text)
        var height = text_paint.textSize
        val top_right_corner = LatLng(top_left_corner.latitude, top_left_corner.longitude+metersToLongitudeOffset(tile_length_meters, top_left_corner.latitude))
        val tile_size_px = google_map.projection.toScreenLocation(top_right_corner).x - google_map.projection.toScreenLocation(top_left_corner).x

        // Reduces font size if there is not enough space
        while (tile_size_px < width && text_paint.textSize > 5f) {
            text_paint.textSize -= 2f
            width = text_paint.measureText(text)
            height = text_paint.textSize
        }

        val image = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(text, 0f, canvas.height.toFloat(), text_paint)

        val marker = google_map.addMarker(
            MarkerOptions()
                .position(top_right_corner)
                .icon(BitmapDescriptorFactory.fromBitmap(image))
                .anchor(1f, 0f)
        )
        if (marker != null) { markers.add(marker) }
    }

    private suspend fun fillScreenWithTiles() {
        lateinit var top_left_visible: LatLng
        lateinit  var bottom_right_visible: LatLng

        withContext(Dispatchers.Main) {
            top_left_visible = getReferenceTileContaining(
                LatLng(google_map.projection.visibleRegion.latLngBounds.northeast.latitude, google_map.projection.visibleRegion.latLngBounds.southwest.longitude)
            )
            bottom_right_visible = LatLng(google_map.projection.visibleRegion.latLngBounds.southwest.latitude, google_map.projection.visibleRegion.latLngBounds.northeast.longitude)
        }

        if (top_left_visible.longitude > bottom_right_visible.longitude) { // Wrap-up area (Pacific Ocean)
            val left_side_bottom = LatLng(bottom_right_visible.latitude, 179.9)
            val right_side_top = getReferenceTileContaining(
                LatLng(top_left_visible.latitude, left_side_bottom.longitude + metersToLongitudeOffset(tile_length_meters, top_left_visible.latitude))
            )

            fillAreaWithTiles(top_left_visible, left_side_bottom)
            fillAreaWithTiles(right_side_top, bottom_right_visible)
        }
        else {
            fillAreaWithTiles(top_left_visible, bottom_right_visible)
        }
    }

    private suspend fun fillAreaWithTiles(top_left: LatLng, bottom_right: LatLng) {
        var current_tile = top_left

        // Fill the screen with tiles
        while (current_tile.latitude >= bottom_right.latitude) {
            while (current_tile.longitude <= bottom_right.longitude) {
                drawTile(current_tile)

                val new_longitude = current_tile.longitude + metersToLongitudeOffset(tile_length_meters, current_tile.latitude)
                current_tile = LatLng(current_tile.latitude, new_longitude)

                // Wrap up handling (checks if the longitude distance is greater than a normal tile size with a 20% tolerance)
                if (abs(new_longitude - current_tile.longitude) > metersToLongitudeOffset(tile_length_meters, current_tile.latitude)*1.2) {
                    break
                }
            }
            current_tile = LatLng( current_tile.latitude - metersToLatitudeOffset(tile_length_meters), top_left.longitude )
        }
    }

    private fun zoomToTileLength(zoom : Float) : Double {
        return when {
            zoom < 5 -> 1000000.0
            zoom < 21 -> 5.0 * (2.0).pow(21.0 - round(zoom))
            else -> 2.0
        }
    }

    private fun updateTilesLength() {
        // Selects new tile length
        tile_length_meters = zoomToTileLength(google_map.cameraPosition.zoom)

        // Realigns top-left corner of the center tile
        top_left_center = LatLng(
            center.latitude + metersToLatitudeOffset(tile_length_meters/2),
            center.longitude - metersToLongitudeOffset(tile_length_meters/2, center.latitude + metersToLatitudeOffset(tile_length_meters/2))
        )
    }

    private fun getReferenceTileContaining(position: LatLng) : LatLng {
        // Difference from the current top-left corner and the center tile
        val lat_diff_to_center = position.latitude - top_left_center.latitude
        val lon_diff_to_center = position.longitude - top_left_center.longitude

        // Number of tiles to skip from the center tile
        val lat_offset_times = ceil( (lat_diff_to_center / metersToLatitudeOffset(tile_length_meters)) )
        val lon_offset_times = floor( (lon_diff_to_center / metersToLongitudeOffset(tile_length_meters, position.latitude)) )

        // Top-left corner from which begin the generation of tiles
        return LatLng(
            top_left_center.latitude + (metersToLatitudeOffset(tile_length_meters * lat_offset_times)),
            top_left_center.longitude + (metersToLongitudeOffset(tile_length_meters * lon_offset_times, position.latitude))
        )
    }
}