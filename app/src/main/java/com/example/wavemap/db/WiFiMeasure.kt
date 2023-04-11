package com.example.wavemap.db

import androidx.room.*
import com.example.wavemap.measures.WaveMeasure

@Entity(tableName="wifi")
data class WiFiMeasure (
    @PrimaryKey(autoGenerate = true)    val id : Int,
    @ColumnInfo(name = "value")         override val value: Double,
    @ColumnInfo(name = "timestamp")     override val timestamp: Long,
    @ColumnInfo(name = "latitude")      override val latitude: Double,
    @ColumnInfo(name = "longitude")     override val longitude: Double
) : WaveMeasure

@Dao
interface WiFiMeasureDAO {
    @Query("SELECT * FROM wifi " +
            "WHERE :top_left_lat <= latitude AND latitude <= :bot_right_lat AND" +
            "      :bot_right_lon <= longitude AND longitude <= :top_left_lon")
    fun get(top_left_lat: Double, top_left_lon: Double, bot_right_lat: Double, bot_right_lon: Double) : List<WiFiMeasure>

    @Query("SELECT * FROM wifi " +
            "WHERE :top_left_lat <= latitude AND latitude <= :bot_right_lat AND " +
            "      :bot_right_lon <= longitude AND longitude <= :top_left_lon " +
            "LIMIT :limit")
    fun get(top_left_lat: Double, top_left_lon: Double, bot_right_lat: Double, bot_right_lon: Double, limit: Int) : List<WiFiMeasure>

    @Insert
    fun insert(measure: WiFiMeasure)
}