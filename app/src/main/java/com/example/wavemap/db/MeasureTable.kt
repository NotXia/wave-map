package com.example.wavemap.db

import androidx.room.*
import com.example.wavemap.measures.WaveMeasure

enum class MeasureType {
    WIFI, NOISE, LTE
}

@Entity(tableName="measures", indices=[Index(value = ["type"])])
data class MeasureTable (
    @PrimaryKey(autoGenerate = true)    val id : Int,
    @ColumnInfo(name = "type")          val type: MeasureType,
    @ColumnInfo(name = "value")         override val value: Double,
    @ColumnInfo(name = "timestamp")     override val timestamp: Long,
    @ColumnInfo(name = "latitude")      override val latitude: Double,
    @ColumnInfo(name = "longitude")     override val longitude: Double,
) : WaveMeasure

@Dao
interface MeasureDAO {
    @Query(
        "SELECT * FROM measures " +
        "WHERE type = :type AND " +
        "      (longitude BETWEEN :bot_right_lon AND :top_left_lon) AND " +
        "      ( " +
        "         ( " +
        "            ( (:top_left_lat >= 0 AND :bot_right_lat >= 0) OR (:top_left_lat < 0 AND :bot_right_lat < 0) OR (:top_left_lat < 0 AND :bot_right_lat >= 0) ) AND " +
        "            (latitude BETWEEN :top_left_lat AND :bot_right_lat) " +
        "         ) OR " +
        "         (" + // Latitude at wrap-up point (somewhere in the Pacific Ocean)
        "            :top_left_lat > 0 AND :bot_right_lat < 0 AND " +
        "            ( " +
        "               (latitude BETWEEN :top_left_lat AND 180) OR " +
        "               (latitude BETWEEN -180 AND :bot_right_lat) " +
        "             ) " +
        "          ) " +
        "      )"
    )
    fun get(type: MeasureType, top_left_lat: Double, top_left_lon: Double, bot_right_lat: Double, bot_right_lon: Double) : List<MeasureTable>

    @Query(
        "SELECT * FROM measures " +
        "WHERE type = :type AND " +
        "      (longitude BETWEEN :bot_right_lon AND :top_left_lon) AND " +
        "      ( " +
        "         ( " +
        "            ( (:top_left_lat >= 0 AND :bot_right_lat >= 0) OR (:top_left_lat < 0 AND :bot_right_lat < 0) OR (:top_left_lat < 0 AND :bot_right_lat >= 0) ) AND " +
        "            (latitude BETWEEN :top_left_lat AND :bot_right_lat) " +
        "         ) OR " +
        "         (" + // Latitude at wrap-up point (somewhere in the Pacific Ocean)
        "            :top_left_lat > 0 AND :bot_right_lat < 0 AND " +
        "            ( " +
        "               (latitude BETWEEN :top_left_lat AND 180) OR " +
        "               (latitude BETWEEN -180 AND :bot_right_lat) " +
        "             ) " +
        "          ) " +
        "      ) " +
        "LIMIT :limit"
    )
    fun get(type: MeasureType, top_left_lat: Double, top_left_lon: Double, bot_right_lat: Double, bot_right_lon: Double, limit: Int) : List<MeasureTable>

    @Insert
    fun insert(measure: MeasureTable)
}