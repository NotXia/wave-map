package com.example.wavemap.db

import androidx.room.*
import com.example.wavemap.measures.WaveMeasure
import java.util.UUID

enum class MeasureType {
    WIFI, NOISE, LTE, BLUETOOTH
}

@Entity(
    tableName="measures",
    indices=[Index(value = ["type"])],
    primaryKeys = ["id"]
)
data class MeasureTable (
    @ColumnInfo(name = "id")            val id : String,
    @ColumnInfo(name = "type")          val type: MeasureType,
    @ColumnInfo(name = "value")         override val value: Double,
    @ColumnInfo(name = "timestamp")     override val timestamp: Long,
    @ColumnInfo(name = "latitude")      override val latitude: Double,
    @ColumnInfo(name = "longitude")     override val longitude: Double,
    @ColumnInfo(name = "info")          override val info: String,
    @ColumnInfo(name = "shared")        override val shared: Boolean
) : WaveMeasure {

    constructor(type: MeasureType, value: Double, timestamp: Long, latitude: Double, longitude: Double, info: String)
        : this(UUID.randomUUID().toString(), type, value, timestamp, latitude, longitude, info, false)

    constructor(type: MeasureType, value: Double, timestamp: Long, latitude: Double, longitude: Double, info: String, shared: Boolean)
        : this(UUID.randomUUID().toString(), type, value, timestamp, latitude, longitude, info, shared)
}

@Dao
interface MeasureDAO {
    companion object {
        private const val coordinate_is_inside_tile: String =
            "(" +
            "(latitude BETWEEN :bot_right_lat AND :top_left_lat) AND " +
            "      ( " +
            "         ( " +
            "            ( (:top_left_lon >= 0 AND :bot_right_lon >= 0) OR (:top_left_lon < 0 AND :bot_right_lon < 0) OR (:top_left_lon < 0 AND :bot_right_lon >= 0) ) AND " +
            "            (longitude BETWEEN :top_left_lon AND :bot_right_lon) " +
            "         ) OR " +
            "         (" + // Longitude at wrap-up point (somewhere in the Pacific Ocean)
            "            :top_left_lon > 0 AND :bot_right_lon < 0 AND " +
            "            ( " +
            "               (longitude BETWEEN :top_left_lon AND 180) OR " +
            "               (longitude BETWEEN -180 AND :bot_right_lon) " +
            "             ) " +
            "          ) " +
            "      )" +
            ")"
    }

    @Query(
        "SELECT * FROM measures " +
        "WHERE type = :type AND " +
        "      ( (:get_shared = 0 AND shared = 0) OR (:get_shared = 1) ) AND " +
        "      $coordinate_is_inside_tile AND" +
        "      timestamp IN (SELECT timestamp " +
        "                    FROM measures " +
        "                    WHERE type = :type AND" +
        "                          $coordinate_is_inside_tile" +
        "                    ORDER BY timestamp " +
        "                    DESC LIMIT :limit) "
    )
    /* Retrieves, for a given tile, the most recent measurements (measures with the same timestamp are all considered) */
    fun get(type: MeasureType, top_left_lat: Double, top_left_lon: Double, bot_right_lat: Double, bot_right_lon: Double, limit: Int, get_shared: Boolean) : List<MeasureTable>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(measure: MeasureTable)


    @Query(
        "SELECT timestamp FROM (" +
        "                           SELECT timestamp, ABS(timestamp - :timestamp) AS diff FROM measures" +
        "                           ORDER BY diff" +
        "                           LIMIT 1" +
        "                       )"
    )
    fun getNearestTimestamp(timestamp: Long) : Long

    @Query("SELECT * FROM measures")
    fun dump() : List<MeasureTable>
}