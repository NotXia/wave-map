package com.example.wavemap.services


import android.Manifest
import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import com.example.wavemap.R
import com.example.wavemap.utilities.LocationUtils.Companion.metersToLatitudeOffset
import com.example.wavemap.utilities.LocationUtils.Companion.metersToLongitudeOffset
import com.google.android.gms.maps.model.LatLng


class BackgroundScanService : Service() {

    companion object {
        fun start(activity: Activity) {
            if (ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                !needToStartService(activity)) {
                return
            }

            activity.startService(Intent(activity.applicationContext, BackgroundScanService::class.java))
        }

        fun stop(activity: Activity) {
            activity.stopService(Intent(activity.applicationContext, BackgroundScanService::class.java))
        }

        fun needToStartService(activity: Activity) : Boolean {
            val pref_manager = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
            return pref_manager.getBoolean("background_scan", false) || pref_manager.getBoolean("notify_uncovered_area", false)
        }
    }

    private lateinit var location_callback : LocationCallback
    private lateinit var fused_location_provider : FusedLocationProviderClient
    private var first_location = true

    private lateinit var pref_manager : SharedPreferences

    private lateinit var samplers : Array<WaveSampler>
    private val tile_size_meters = 10.0

    private var last_uncovered_area_notification_time : Long = 0
    private val NOTIFICATION_DELAY_MS = 30000

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val db = Room.databaseBuilder(applicationContext, WaveDatabase::class.java, Constants.DATABASE_NAME).build()
        samplers = arrayOf(
            WiFiSampler(applicationContext, null, db),
            LTESampler(applicationContext, db),
//            NoiseSampler(applicationContext, db),
            BluetoothSampler(applicationContext, null, db)
        )
        pref_manager = PreferenceManager.getDefaultSharedPreferences(applicationContext)


        location_callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.lastLocation == null) { return }
                if (first_location) { first_location = false; return } // Ignore the first location since it will be very close to when the app was still in foreground

                val location = locationResult.lastLocation!!

                // Uncovered area notification
                if (pref_manager.getBoolean("notify_uncovered_area", false)) {
                    if (System.currentTimeMillis() - last_uncovered_area_notification_time < NOTIFICATION_DELAY_MS) {
                        return // To avoid too pedantic notifications
                    }

                    GlobalScope.launch {
                        samplers.forEach { sampler ->
                            try {
                                val measure = sampler.retrieve(
                                    LatLng(location.latitude, location.longitude),
                                    LatLng(location.latitude-metersToLatitudeOffset(tile_size_meters), location.longitude+metersToLongitudeOffset(tile_size_meters, location.latitude)),
                                    1
                                )

                                if (measure.isEmpty()) {
                                    sendUncoveredAreaNotification()
                                    last_uncovered_area_notification_time = System.currentTimeMillis()
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
                if (pref_manager.getBoolean("background_scan", false)) {
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

    private fun sendUncoveredAreaNotification() {
        if ( ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Missing POST_NOTIFICATIONS permission")
        }

        lateinit var notification_builder : NotificationCompat.Builder

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel_id = "ch_uncovered_area"
            val channel_name: CharSequence = getString(R.string.notification_uncovered_area)
            val channel_desc = getString(R.string.notification_uncovered_area_desc)
            val channel = NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = channel_desc
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            notification_builder = NotificationCompat.Builder(applicationContext, channel_id)

        } else {
            notification_builder = NotificationCompat.Builder(applicationContext)
        }

        notification_builder.setContentTitle(getString(R.string.notification_uncovered_area)).setContentText(getString(R.string.notification_uncovered_area_text))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        NotificationManagerCompat.from(applicationContext).notify(1, notification_builder.build())
    }
}