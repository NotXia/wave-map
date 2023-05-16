package com.example.wavemap.measures.samplers

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import com.example.wavemap.db.BSSIDTable
import com.example.wavemap.db.MeasureTable
import com.example.wavemap.db.MeasureType
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveMeasure
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.LocationUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Integer.min
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class BluetoothSampler : WaveSampler {
    private val context : Context
    private val device_name : String?
    private val db : WaveDatabase

    constructor(context: Context, device_name: String?, db: WaveDatabase) {
        this.context = context
        this.device_name = device_name
        this.db = db
    }

    override suspend fun sample() : List<WaveMeasure> = suspendCoroutine { cont ->
        /**
         * Measures are received from a BroadcastReceiver.
         * When nothing is sent for a while, the sampling ends.
         * */
        if ( ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ) {
            return@suspendCoroutine cont.resumeWithException( SecurityException("Missing BLUETOOTH_SCAN permission") )
        }
        if ( ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ) {
            return@suspendCoroutine cont.resumeWithException( SecurityException("Missing BLUETOOTH_CONNECT permissions") )
        }
        if ( ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return@suspendCoroutine cont.resumeWithException( SecurityException("Missing ACCESS_FINE_LOCATION permissions") )
        }

        var bt_list = mutableListOf<MeasureTable>()
        val timestamp = System.currentTimeMillis()

        lateinit var bt_receiver : BroadcastReceiver
        val handler = Handler(Looper.getMainLooper())
        val endScan = Runnable {
            try {
                context.unregisterReceiver(bt_receiver)
                return@Runnable cont.resume( bt_list )
            }
            catch (err : IllegalArgumentException) { /* Already unregistered */ }
        }

        bt_receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        handler.removeCallbacks(endScan)

                        GlobalScope.launch {
                            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                            if (device == null) { return@launch }
                            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                            val current_location: LatLng = LocationUtils.getCurrent(context) // Require location at each measure for more accuracy

                            bt_list.add( MeasureTable(0, MeasureType.BLUETOOTH,
                                rssi.toDouble(),
                                timestamp, current_location.latitude, current_location.longitude,
                                device.address ?: "")
                            )
                            db.bssidDAO().insert( BSSIDTable(device.address, if (device.name != null) device.name else "") )

                            handler.postDelayed(endScan, 5000)
                        }
                    }
                }
            }
        }
        registerReceiver(context, bt_receiver, IntentFilter(BluetoothDevice.ACTION_FOUND), RECEIVER_EXPORTED)

        val bt_manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bt_manager.adapter.startDiscovery()
    }

    override suspend fun store(measures: List<WaveMeasure>) : Unit {
        for (measure in measures) {
            db.measureDAO().insert( MeasureTable(0, MeasureType.BLUETOOTH, measure.value, measure.timestamp, measure.latitude, measure.longitude, measure.info) )
        }
    }

    override suspend fun retrieve(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?) : List<WaveMeasure> {
        var measures : List<WaveMeasure> =
            db.measureDAO().get(MeasureType.BLUETOOTH, top_left_corner.latitude, top_left_corner.longitude, bottom_right_corner.latitude, bottom_right_corner.longitude, -1)

        if (device_name == null) {
            val measures_by_timestamp = measures.groupBy { it.timestamp }
            val timestamps = measures_by_timestamp.keys.toList().sortedDescending()
            measures = mutableListOf()

            // Gets the highest measure for each timestamp (up to limit)
            for (i in 0 until min(limit ?: timestamps.size, timestamps.size)) {
                measures.add( measures_by_timestamp[timestamps[i]]!!.maxBy{ it.value } )
            }
        }
        else {
            measures = measures.filter { m -> m.info == device_name }

            if (limit != null) { measures = measures.subList(0, min(limit, measures.size)) }
        }

        return measures
    }
}