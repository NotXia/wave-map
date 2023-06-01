package com.example.wavemap.measures.samplers

import android.Manifest
import android.content.Context
import android.content.Context.TELEPHONY_SERVICE
import android.content.pm.PackageManager
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.wavemap.db.MeasureTable
import com.example.wavemap.db.MeasureType
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveMeasure
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.LocationUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class LTESampler : WaveSampler {
    private val context : Context
    private val db : WaveDatabase

    constructor(context: Context, db: WaveDatabase) {
        this.context = context
        this.db = db
    }

    override suspend fun sample() : List<WaveMeasure> = suspendCoroutine { cont ->
        if ( ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return@suspendCoroutine cont.resumeWithException( SecurityException("Missing ACCESS_FINE_LOCATION permissions") )
        }

        val telephony_manager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager?
            ?: return@suspendCoroutine cont.resumeWithException( RuntimeException("Cannot get TelephonyManager") )

        val lte_info : CellInfoLte? = telephony_manager.allCellInfo.find { cell_info -> cell_info is CellInfoLte }  as CellInfoLte?
            ?: return@suspendCoroutine cont.resumeWithException( RuntimeException("Cannot get LTE data") )

        GlobalScope.launch() {
            val current_location: LatLng = LocationUtils.getCurrent(context)

            return@launch cont.resume( listOf(
                MeasureTable(0, MeasureType.LTE,
                lte_info!!.cellSignalStrength.dbm.toDouble(),
                System.currentTimeMillis(),
                current_location.latitude, current_location.longitude, "")
            ) )
        }

    }

    override suspend fun store(measures: List<WaveMeasure>) : Unit {
        for (measure in measures) {
            db.measureDAO().insert( MeasureTable(0, MeasureType.LTE, measure.value, measure.timestamp, measure.latitude, measure.longitude, measure.info) )
        }
    }

    override suspend fun retrieve(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?) : List<WaveMeasure> {
        return db.measureDAO().get(
            MeasureType.LTE,
            top_left_corner.latitude,
            top_left_corner.longitude,
            bottom_right_corner.latitude,
            bottom_right_corner.longitude,
            limit ?: -1
        )
    }
}