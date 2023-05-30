package com.example.wavemap.services


import android.Manifest
import android.app.*
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
import com.example.wavemap.measures.samplers.NoiseSampler
import com.example.wavemap.measures.samplers.WiFiSampler
import com.example.wavemap.utilities.Constants
import com.google.android.gms.location.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class BackgroundScanService : Service() {

    companion object {
        fun start(activity: Activity) {
            val pref_manager = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)

            if ( ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                !pref_manager.getBoolean("background_scan", false)) {
                return
            }
            activity.startService(Intent(activity.applicationContext, BackgroundScanService::class.java))
        }

        fun stop(activity: Activity) {
            activity.stopService(Intent(activity.applicationContext, BackgroundScanService::class.java))
        }
    }

    private lateinit var location_callback : LocationCallback
    private lateinit var fused_location_provider : FusedLocationProviderClient
    private lateinit var samplers : Array<WaveSampler>
    private var first_location = true

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
                if (locationResult.lastLocation == null) { return }
                if (first_location) { first_location = false; return } // Ignore the first location since it will be very close to when the app was still in foreground

                samplers.forEach {sampler ->
                    GlobalScope.launch {
                        try {
                            sampler.sampleAndStore()
                        }
                        catch (err: Exception) {
                            Log.e("sampler", "Background sampling: $err")
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        if ( ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            throw SecurityException("Missing ACCESS_FINE_LOCATION permission")
        }

        first_location = true

        val options = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000).apply {
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setMinUpdateDistanceMeters(10.0f)
        }.build()

        fused_location_provider = LocationServices.getFusedLocationProviderClient(applicationContext)
        fused_location_provider.requestLocationUpdates(options, location_callback, Looper.myLooper())
    }

    private fun stopLocationUpdates() {
        try {
            fused_location_provider.removeLocationUpdates(location_callback)
        }
        catch (err : RuntimeException) {

        }
    }
}