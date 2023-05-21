package com.example.wavemap.measures.samplers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.wavemap.db.*
import com.example.wavemap.exceptions.MeasureException
import com.example.wavemap.measures.WaveMeasure
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.LocationUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.lang.Integer.min
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

    @RequiresApi(Build.VERSION_CODES.S)
    override suspend fun sample() : List<WaveMeasure> = suspendCoroutine { cont ->
        GlobalScope.launch {
            var measures = listOf<WaveMeasure>()

            val connected_wifi = sampleCurrentlyConnectedWiFi()
            if (connected_wifi != null) { measures = measures + listOf(connected_wifi) }

            try {
                measures = measures + sampleWithWiFiScanner()
            }
            catch (err : MeasureException) { /* Cannot start wifi scan */ }

            if (measures.isEmpty() && connected_wifi == null) {
                cont.resumeWithException( MeasureException("Cannot scan Wi-Fi") )
            }
            else {
                cont.resume( measures )
            }
        }
    }

    private fun createWiFiName(ssid: String, bssid: String, frequency: Int) : String {
        val frequency_str = if (frequency >= 4900) "5 GHz" else "2.4 GHz"
        val ssid_normalized = ssid.replace("^\"|\"$".toRegex(), "")

        return if (ssid_normalized.isNotEmpty()) {
            "${ssid_normalized} (${frequency_str})"
        } else {
            "${bssid}"
        }
    }

    private suspend fun sampleCurrentlyConnectedWiFi() : WaveMeasure? = suspendCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
            val connectivity_manager : ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val timeout_handler = Handler(Looper.getMainLooper())

            val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    timeout_handler.removeCallbacksAndMessages(null)
                    connectivity_manager.unregisterNetworkCallback(this)

                    GlobalScope.launch {
                        val wifi_info = networkCapabilities.transportInfo as WifiInfo
                        val current_location: LatLng = LocationUtils.getCurrent(context)
                        val timestamp = System.currentTimeMillis()

                        db.bssidDAO().insert( BSSIDTable(wifi_info.bssid, createWiFiName(wifi_info.ssid, wifi_info.bssid, wifi_info.frequency), BSSIDType.WIFI) )
                        cont.resume(
                            MeasureTable(0, MeasureType.WIFI, wifi_info.rssi.toDouble(), timestamp, current_location.latitude, current_location.longitude, wifi_info.bssid)
                        )
                    }
                }
            }

            connectivity_manager.requestNetwork(request, callback)
            connectivity_manager.registerNetworkCallback(request, callback)
            timeout_handler.postDelayed(Runnable {
                try { connectivity_manager.unregisterNetworkCallback(callback) } catch (err : Exception) { /* Just in case */ }
                try { cont.resume(null) } catch (err : Exception) { /* Just in case */ }
            }, 3000)
        } else {
            GlobalScope.launch {
                val wifi_info = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
                val current_location: LatLng = LocationUtils.getCurrent(context)
                val timestamp = System.currentTimeMillis()

                db.bssidDAO().insert( BSSIDTable(wifi_info.bssid, createWiFiName(wifi_info.ssid, wifi_info.bssid, wifi_info.frequency), BSSIDType.WIFI) )
                cont.resume(
                    MeasureTable(0, MeasureType.WIFI, wifi_info.rssi.toDouble(), timestamp, current_location.latitude, current_location.longitude, wifi_info.bssid)
                )
            }
        }

    }

    private suspend fun sampleWithWiFiScanner() : List<WaveMeasure> = suspendCoroutine { cont ->
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (!success) { return }
                context.unregisterReceiver(this)

                GlobalScope.launch {
                    if ( ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                        return@launch cont.resumeWithException( SecurityException("Missing ACCESS_FINE_LOCATION permissions") )
                    }

                    val current_location: LatLng = LocationUtils.getCurrent(context)
                    val results = wifiManager.scanResults
                    var wifi_list = mutableListOf<MeasureTable>()
                    val timestamp = System.currentTimeMillis()

                    for (wifi in results) {
                        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) wifi.wifiSsid.toString().drop(1).dropLast(1).trim() else wifi.SSID

                        wifi_list.add( MeasureTable(0, MeasureType.WIFI, wifi.level.toDouble(), timestamp, current_location.latitude, current_location.longitude, wifi.BSSID) )
                        db.bssidDAO().insert(
                            BSSIDTable(wifi.BSSID, createWiFiName(ssid, wifi.BSSID, wifi.frequency), BSSIDType.WIFI)
                        )
                    }

                    cont.resume( wifi_list )
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

    override suspend fun store(measures: List<WaveMeasure>) : Unit {
        for (measure in measures) {
            db.measureDAO().insert( MeasureTable(0, MeasureType.WIFI, measure.value, measure.timestamp, measure.latitude, measure.longitude, measure.info) )
        }
    }

    override suspend fun retrieve(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?) : List<WaveMeasure> {
        var measures : List<WaveMeasure> =
            db.measureDAO().get(MeasureType.WIFI, top_left_corner.latitude, top_left_corner.longitude, bottom_right_corner.latitude, bottom_right_corner.longitude, -1)

        if (bssid == null) {
            val measures_by_timestamp = measures.groupBy { it.timestamp }
            val timestamps = measures_by_timestamp.keys.toList().sortedDescending()
            measures = mutableListOf()

            // Gets the highest measure for each timestamp (up to limit)
            for (i in 0 until min(limit ?: timestamps.size, timestamps.size)) {
                measures.add( measures_by_timestamp[timestamps[i]]!!.maxBy{ it.value } )
            }
        }
        else {
            measures = measures.filter { m -> m.info == bssid }

            if (limit != null) { measures = measures.subList(0, min(limit, measures.size)) }
        }

        return measures
    }
}