package com.example.wavemap.services


import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.measures.samplers.BluetoothSampler
import com.example.wavemap.measures.samplers.LTESampler
import com.example.wavemap.measures.samplers.WiFiSampler
import com.example.wavemap.utilities.Constants
import com.google.android.gms.location.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.example.wavemap.measures.samplers.NoiseSampler
import com.example.wavemap.notifications.BackgroundScanningNotification
import com.example.wavemap.notifications.UncoveredAreaNotification
import com.example.wavemap.utilities.LocationUtils.Companion.metersToLatitudeOffset
import com.example.wavemap.utilities.LocationUtils.Companion.metersToLongitudeOffset
import com.example.wavemap.utilities.Permissions
import com.google.android.gms.maps.model.LatLng


class BackgroundScanService : Service() {

    companion object {
        private const val EXTRA_BACKGROUND_SCAN = "wavemap.background_scan"
        private const val EXTRA_UNCOVERED_AREA = "wavemap.uncovered_area"

        fun start(activity: Activity) {
            if (!Permissions.check(activity.applicationContext, Permissions.background_gps) || !needToStartService(activity.applicationContext)) {
                return
            }
            forceStart(activity)
        }

        fun stop(activity: Activity) {
            if (needToStartService(activity.applicationContext)) {
                return
            }
            forceStop(activity)
        }

        fun needToStartService(context: Context) : Boolean {
            val pref_manager = PreferenceManager.getDefaultSharedPreferences(context)
            return pref_manager.getBoolean("background_scan", false) || pref_manager.getBoolean("notify_uncovered_area", false)
        }

        fun forceStart(activity: Activity) {
            val pref_manager = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
            val intent = Intent(activity.applicationContext, BackgroundScanService::class.java)
            intent.putExtra(EXTRA_BACKGROUND_SCAN, pref_manager.getBoolean("background_scan", false))
            intent.putExtra(EXTRA_UNCOVERED_AREA, pref_manager.getBoolean("notify_uncovered_area", false))
            activity.startService(intent)
        }

        fun forceStop(activity: Activity) {
            activity.stopService(Intent(activity.applicationContext, BackgroundScanService::class.java))
        }
    }


    private lateinit var location_callback : LocationCallback
    private lateinit var fused_location_provider : FusedLocationProviderClient
    private var first_location = true

    private lateinit var samplers : Array<WaveSampler>
    private val tile_size_meters = 10.0

    private val UNCOVERED_NOTIFICATION_ID = 1
    private val FOREGROUND_NOTIFICATION_ID = 2

    private var should_notify_uncovered = false
    private var should_background_scan = false

    private val RECENT_MEASURE_TOLERANCE = 1000*60*60*24


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val db = Room.databaseBuilder(applicationContext, WaveDatabase::class.java, Constants.DATABASE_NAME).build()
        samplers = arrayOf(
            WiFiSampler(applicationContext, null, db),
            LTESampler(applicationContext, db),
            NoiseSampler(applicationContext, db),
            BluetoothSampler(applicationContext, null, db)
        )

        location_callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!needToStartService(applicationContext)) { return stopSelf() }
                if (locationResult.lastLocation == null) { return }
                if (first_location) { first_location = false; return } // Ignore the first location since it will be very close to when the app was still in foreground

                val location = locationResult.lastLocation!!

                // Uncovered area notification
                if (should_notify_uncovered) {
                    GlobalScope.launch {
                        samplers.forEach { sampler ->
                            try {
                                val measure = sampler.retrieve(
                                    LatLng(location.latitude, location.longitude),
                                    LatLng(location.latitude-metersToLatitudeOffset(tile_size_meters), location.longitude+metersToLongitudeOffset(tile_size_meters, location.latitude)),
                                    1
                                )

                                if (measure.isEmpty() || System.currentTimeMillis() - measure[0].timestamp > RECENT_MEASURE_TOLERANCE) {
                                    UncoveredAreaNotification.send(applicationContext, UNCOVERED_NOTIFICATION_ID)
                                    return@forEach
                                }
                            }
                            catch (err: Exception) {
                                Log.e("sampler", "Background retrieve: $err")
                            }
                        }
                    }
                }

                // New measures
                if (should_background_scan) {
                    samplers.forEach { sampler ->
                        GlobalScope.launch {
                            try {
                                sampler.sampleAndStore()
                            } catch (err: Exception) {
                                Log.e("sampler", "Background sampling: $err")
                            }
                        }
                    }
                }

            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            should_background_scan = intent.getBooleanExtra(EXTRA_BACKGROUND_SCAN, false)
            should_notify_uncovered = intent.getBooleanExtra(EXTRA_UNCOVERED_AREA, false)
        }
        handleForegroundSwitch()

        try {
            startLocationUpdates()
        }
        catch (err : Exception) {
            Log.e("service", "Cannot start location retriever")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        if ( ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            throw SecurityException("Missing ACCESS_FINE_LOCATION permission")
        }
        if ( ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            throw SecurityException("Missing ACCESS_BACKGROUND_LOCATION permission")
        }

        first_location = true

        val options = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000).apply {
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setMinUpdateDistanceMeters(tile_size_meters.toFloat())
        }.build()

        fused_location_provider = LocationServices.getFusedLocationProviderClient(applicationContext)
        fused_location_provider.requestLocationUpdates(options, location_callback, Looper.myLooper())
    }

    private fun stopLocationUpdates() {
        try {
            fused_location_provider.removeLocationUpdates(location_callback)
        }
        catch (err : RuntimeException) { /* Empty */ }
    }

    private fun handleForegroundSwitch() {
        if (should_background_scan) {
            startForeground(FOREGROUND_NOTIFICATION_ID, BackgroundScanningNotification.build(applicationContext))
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

}