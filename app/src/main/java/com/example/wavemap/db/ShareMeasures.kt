package com.example.wavemap.db

import com.example.wavemap.utilities.Constants
import com.google.gson.Gson
import kotlin.math.abs

class ShareMeasures {

    data class ExportFormat(
        val db_version: Int,
        val timestamp: Long,
        val measures : List<MeasureTable>,
        val bssids : List<BSSIDTable>
    )

    companion object {
        fun export(db: WaveDatabase) : String {
            val measures = db.measureDAO().dump()
            val bssids = db.bssidDAO().dump()

            return Gson().toJson(ExportFormat(
                db.openHelper.writableDatabase.version,
                System.currentTimeMillis(),
                measures,
                bssids
            ))
        }


        fun parse(import_data: String) : ExportFormat {
            return Gson().fromJson(import_data, ExportFormat::class.java)
        }

        fun import(db: WaveDatabase, data: String) { import(db, parse(data)) }

        fun import(db: WaveDatabase, data: ExportFormat) {
            if (data.db_version != db.openHelper.writableDatabase.version) { throw RuntimeException("DB version mismatch") }

            // Imports measures
            data.measures.forEach {
                val nearest_timestamp = db.measureDAO().getNearestTimestamp(it.timestamp)

                // Measures with a timestamp close to an existing measure are changed to have the same timestamp
                if (abs(nearest_timestamp - it.timestamp) <= Constants.SHARED_MERGE_TOLERANCE) {
                    db.measureDAO().insert( MeasureTable(AuthorType.SHARE, it.type, it.value, nearest_timestamp, it.latitude, it.longitude, it.info) )
                } else {
                    db.measureDAO().insert( MeasureTable(AuthorType.SHARE, it.type, it.value, it.timestamp, it.latitude, it.longitude, it.info) )
                }
            }

            // Imports BSSIDs
            data.bssids.forEach {
                db.bssidDAO().insert( it )
            }
        }
    }

}