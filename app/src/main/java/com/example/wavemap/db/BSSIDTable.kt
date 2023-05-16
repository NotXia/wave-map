package com.example.wavemap.db

import androidx.room.*

@Entity(tableName="bssids", indices=[Index(value = ["bssid"])])
data class BSSIDTable (
    @PrimaryKey()                   val bssid : String,
    @ColumnInfo(name = "ssid")      val ssid: String
)

@Dao
interface BSSIDDAO {
    @Query("SELECT ssid FROM bssids WHERE bssid = :bssid")
    fun get(bssid : String) : String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bssid_entry: BSSIDTable)
}