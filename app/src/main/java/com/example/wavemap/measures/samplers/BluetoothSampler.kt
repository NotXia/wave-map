package com.example.wavemap.measures.samplers

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import com.example.wavemap.db.*
import com.example.wavemap.measures.WaveMeasure
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.LocationUtils
import com.example.wavemap.utilities.Permissions
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class BluetoothSampler(
    private val context: Context,
    private val device_name: String?,
    private val db: WaveDatabase
) : WaveSampler() {

    companion object {
        fun isBluetoothEnabled(context: Context) : Boolean {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager? ?: return false
            return manager.adapter.isEnabled
        }
    }

    // Measures with a complete scan and by checking paired devices
    override suspend fun sample() : List<WaveMeasure> {
        if ( !Permissions.check(context, Permissions.bluetooth) ) { throw SecurityException("Missing bluetooth permissions") }

        val timestamp = System.currentTimeMillis()
        return sampleWithDiscovery(timestamp) + sampleConnected(timestamp)
    }

    @SuppressLint("MissingPermission")
    private suspend fun sampleConnected(timestamp: Long) : List<WaveMeasure> = suspendCoroutine { cont ->
        val bt_manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            ?: return@suspendCoroutine cont.resumeWithException( RuntimeException("Cannot get BluetoothManager") )
        val pairedDevices: Set<BluetoothDevice> = bt_manager.adapter.bondedDevices
        val bt_list = mutableListOf<MeasureTable>()

        suspend fun getRSSI(device: BluetoothDevice) : Int? = suspendCoroutine { cont ->
            device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                    super.onReadRemoteRssi(gatt, rssi, status)
                    cont.resume(rssi)
                }

                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> gatt?.readRemoteRssi()
                        else -> cont.resume(null)
                    }
                }
            })
        }

        GlobalScope.launch {
            pairedDevices.forEach { device ->
                val rssi = getRSSI(device)

                if (rssi != null) {
                    val current_location: LatLng = LocationUtils.getCurrent(context)

                    bt_list.add(
                        MeasureTable(MeasureType.BLUETOOTH, rssi.toDouble(), timestamp, current_location.latitude, current_location.longitude, device.address ?: "")
                    )
                    db.bssidDAO().insert( BSSIDTable(device.address, if (device.name != null) device.name else device.address, BSSIDType.BLUETOOTH) )
                }
            }

            cont.resume(bt_list)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun sampleWithDiscovery(timestamp: Long) : List<WaveMeasure> = suspendCoroutine { cont ->
        val bt_list = mutableListOf<MeasureTable>()
        val bt_receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        context.unregisterReceiver(this)
                        return cont.resume( bt_list )
                    }

                    BluetoothDevice.ACTION_FOUND -> GlobalScope.launch {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device == null) { return@launch }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        val current_location: LatLng = LocationUtils.getCurrent(context) // Request location for each measure for more accuracy

                        bt_list.add(
                            MeasureTable(MeasureType.BLUETOOTH, rssi.toDouble(), timestamp, current_location.latitude, current_location.longitude, device.address ?: "")
                        )
                        db.bssidDAO().insert( BSSIDTable(device.address, if (device.name != null) device.name else device.address, BSSIDType.BLUETOOTH) )
                    }
                }
            }
        }

        val bt_manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val intent_filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        intent_filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(context, bt_receiver, intent_filter, RECEIVER_EXPORTED)
        bt_manager.adapter.startDiscovery()
    }

    override suspend fun store(measures: List<WaveMeasure>) {
        for (measure in measures) {
            db.measureDAO().insert( MeasureTable(MeasureType.BLUETOOTH, measure.value, measure.timestamp, measure.latitude, measure.longitude, measure.info, measure.shared) )
        }
    }

    override suspend fun retrieve(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?, get_shared: Boolean) : List<WaveMeasure> {
        if (device_name == null) {
            return (
                db.measureDAO().get(MeasureType.BLUETOOTH, top_left_corner.latitude, top_left_corner.longitude, bottom_right_corner.latitude, bottom_right_corner.longitude, limit ?: -1, get_shared)
            )
        }
        else {
            var measures = db.measureDAO().get(MeasureType.BLUETOOTH, top_left_corner.latitude, top_left_corner.longitude, bottom_right_corner.latitude, bottom_right_corner.longitude, -1, get_shared)
            measures = measures.filter{ m -> m.info == device_name }
            if (limit != null) { measures = measures.subList(0, Integer.min(limit, measures.size)) }
            return measures
        }
    }
}