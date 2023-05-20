package com.example.wavemap.db

import androidx.room.*

enum class BSSIDType {
    WIFI, BLUETOOTH
}
@Entity(tableName="bssids")
data class BSSIDTable (
    @PrimaryKey()                   val bssid : String,
    @ColumnInfo(name = "ssid")      val ssid: String,
    @ColumnInfo(name = "type")      val type: BSSIDType
)

@Dao
interface BSSIDDAO {
    @Query("SELECT ssid FROM bssids WHERE bssid = :bssid")
    fun get(bssid : String) : String

    @Query("SELECT * FROM bssids WHERE type = :type ORDER BY ssid")
    fun getList(type : BSSIDType) : List<BSSIDTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bssid_entry: BSSIDTable)
}