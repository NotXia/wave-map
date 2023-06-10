package com.example.wavemap.measures.samplers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.wavemap.db.*
import com.example.wavemap.measures.WaveMeasure
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.LocationUtils
import com.example.wavemap.utilities.Permissions
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.lang.Integer.min
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class WiFiSampler(
    private val context: Context,
    private val bssid: String?,
    private val db: WaveDatabase
) : WaveSampler() {

    companion object {
        fun isWiFiEnabled(context: Context) : Boolean {
            if ( !Permissions.check(context, Permissions.wifi) ) { return false }
            val manager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager? ?: return false
            return manager.isWifiEnabled
        }
    }

    // Measures the current connected Wi-Fi and tries to start a complete scan
    override suspend fun sample() : List<WaveMeasure> = suspendCoroutine { cont ->
        if ( !Permissions.check(context, Permissions.wifi) ) { return@suspendCoroutine cont.resumeWithException( SecurityException("Missing Wi-Fi permissions") ) }

        GlobalScope.launch {
            var measures = listOf<WaveMeasure>()
            val timestamp = System.currentTimeMillis()

            val connected_wifi = sampleCurrentlyConnectedWiFi(timestamp)
            if (connected_wifi != null) { measures = listOf(connected_wifi) }

            try {
                measures = measures + sampleWithWiFiScanner(timestamp)
            }
            catch (err : RuntimeException) { /* Cannot start Wi-Fi scan */ }

            if (measures.isEmpty() && connected_wifi == null) {
                cont.resumeWithException( RuntimeException("Cannot scan Wi-Fi") )
            }
            else {
                cont.resume( measures )
            }
        }
    }

    private fun createWiFiName(ssid: String, bssid: String, frequency: Int) : String {
        val frequency_str = if (frequency >= 4900) "5 GHz" else "2.4 GHz"
        val ssid_normalized = ssid.replace("^\"|\"$".toRegex(), "").trim()

        return if (ssid_normalized.isNotEmpty()) "$ssid_normalized ($frequency_str)" else bssid
    }

    private suspend fun sampleCurrentlyConnectedWiFi(timestamp: Long) : WaveMeasure? = suspendCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
            val connectivity_manager : ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val timeout_handler = Handler(Looper.getMainLooper())
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    timeout_handler.removeCallbacksAndMessages(null)
                    connectivity_manager.unregisterNetworkCallback(this)

                    GlobalScope.launch {
                        val wifi_info = networkCapabilities.transportInfo as WifiInfo?
                            ?: return@launch cont.resume( null )
                        val current_location: LatLng = LocationUtils.getCurrent(context)

                        db.bssidDAO().insert( BSSIDTable(wifi_info.bssid, createWiFiName(wifi_info.ssid, wifi_info.bssid, wifi_info.frequency), BSSIDType.WIFI) )
                        try {
                            cont.resume(
                                MeasureTable(MeasureType.WIFI, wifi_info.rssi.toDouble(), timestamp, current_location.latitude, current_location.longitude, wifi_info.bssid)
                            )
                        } catch (err : Exception) { /* Just in case */ }
                    }
                }
            }

            connectivity_manager.registerNetworkCallback(request, callback)
            timeout_handler.postDelayed(Runnable {
                try {
                    connectivity_manager.unregisterNetworkCallback(callback)
                    cont.resume(null)
                } catch (err : Exception) { /* Just in case */ }
            }, 3000)
        } else {
            GlobalScope.launch {
                val wifi_info = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
                val current_location: LatLng = LocationUtils.getCurrent(context)

                db.bssidDAO().insert( BSSIDTable(wifi_info.bssid, createWiFiName(wifi_info.ssid, wifi_info.bssid, wifi_info.frequency), BSSIDType.WIFI) )
                cont.resume(
                    MeasureTable(MeasureType.WIFI, wifi_info.rssi.toDouble(), timestamp, current_location.latitude, current_location.longitude, wifi_info.bssid)
                )
            }
        }

    }

    private suspend fun sampleWithWiFiScanner(timestamp: Long) : List<WaveMeasure> = suspendCoroutine { cont ->
        val wifi_manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifi_receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                try { context.unregisterReceiver(this) } catch (err : Exception) { /* Just in case*/ }

                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (!success) { return cont.resumeWithException( RuntimeException("Wi-Fi scan failed") ) }

                GlobalScope.launch {
                    val current_location: LatLng = LocationUtils.getCurrent(context)
                    val results = wifi_manager.scanResults
                    val wifi_list = mutableListOf<MeasureTable>()

                    for (wifi in results) {
                        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) wifi.wifiSsid.toString().drop(1).dropLast(1).trim() else wifi.SSID

                        wifi_list.add( MeasureTable(MeasureType.WIFI, wifi.level.toDouble(), timestamp, current_location.latitude, current_location.longitude, wifi.BSSID) )
                        db.bssidDAO().insert( BSSIDTable(wifi.BSSID, createWiFiName(ssid, wifi.BSSID, wifi.frequency), BSSIDType.WIFI) )
                    }

                    cont.resume( wifi_list )
                }
            }
        }

        val intent_filter = IntentFilter()
        intent_filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifi_receiver, intent_filter)

        /*
        * https://developer.android.com/guide/topics/connectivity/wifi-scan
        * Deprecated in API level 28
        * Each foreground app can scan four times in a 2-minute period.
        * All background apps combined can scan one time in a 30-minute period.
        * */
        val success = wifi_manager.startScan()
        if (!success) {
            context.unregisterReceiver(wifi_receiver)
            cont.resumeWithException(RuntimeException("Cannot scan Wi-Fi"))
        }
    }

    override suspend fun store(measures: List<WaveMeasure>) {
        for (measure in measures) {
            db.measureDAO().insert( MeasureTable(MeasureType.WIFI, measure.value, measure.timestamp, measure.latitude, measure.longitude, measure.info, measure.shared) )
        }
    }

    override suspend fun retrieve(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?, get_shared: Boolean) : List<WaveMeasure> {
        if (bssid == null) {
            return (
                db.measureDAO().get(MeasureType.WIFI, top_left_corner.latitude, top_left_corner.longitude, bottom_right_corner.latitude, bottom_right_corner.longitude, limit ?: -1, get_shared)
            )
        }
        else {
            var measures = db.measureDAO().get(MeasureType.WIFI, top_left_corner.latitude, top_left_corner.longitude, bottom_right_corner.latitude, bottom_right_corner.longitude, -1, get_shared)
            measures = measures.filter{ m -> m.info == bssid }
            if (limit != null) { measures = measures.subList(0, min(limit, measures.size)) }
            return measures
        }
    }
}