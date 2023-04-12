package com.example.wavemap.measures

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.db.MeasureTable
import com.example.wavemap.db.MeasureType
import com.example.wavemap.exceptions.MeasureException
import com.google.android.gms.maps.model.LatLng
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class WiFiSampler : WaveSampler {
    private val context : Context
    private val bssid : String?
    private val db : WaveDatabase

    constructor(context: Context, bssid: String?, db: WaveDatabase) {
        this.context = context
        this.bssid = bssid
        this.db = db
    }

    override suspend fun sample(): WaveMeasure = suspendCoroutine { cont ->
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)

                if (success) {
                    if ( ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    val results = wifiManager.scanResults
                    var wifi_data : ScanResult? = if (bssid == null) results.maxByOrNull{ it.level } else results.firstOrNull{ it.BSSID == bssid }
                    val wifi_level = wifi_data?.level?.toDouble() ?: 0.0

                    context.unregisterReceiver(this)
                    cont.resume( MeasureTable(0, MeasureType.WIFI, wifi_level, 1L, 1.0, 1.0) )
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        /*
        * https://developer.android.com/guide/topics/connectivity/wifi-scan
        * Deprecated in API level 28
        * Each foreground app can scan four times in a 2-minute period.
        * All background apps combined can scan one time in a 30-minute period.
        * */
        val success = wifiManager.startScan()
        if (!success) {
            context.unregisterReceiver(wifiScanReceiver)
            cont.resumeWithException(MeasureException())
        }
    }

    override suspend fun store(measure: WaveMeasure) : Unit {
        db.measureDAO().insert( MeasureTable(0, MeasureType.WIFI, measure.value, measure.timestamp, measure.latitude, measure.longitude) )
    }

    override suspend fun retrieve(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?) : List<WaveMeasure> {
        val db_measures : List<MeasureTable> = if (limit != null) {
            db.measureDAO().get(MeasureType.WIFI, top_left_corner.latitude, top_left_corner.longitude, bottom_right_corner.latitude, bottom_right_corner.longitude, limit)
        } else {
            db.measureDAO().get(MeasureType.WIFI, top_left_corner.latitude, top_left_corner.longitude, bottom_right_corner.latitude, bottom_right_corner.longitude)
        }

        return db_measures
    }
}