package com.example.wavemap.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities=[WiFiMeasure::class], version=1)
abstract class WaveDatabase : RoomDatabase() {
    abstract fun wifiDao() : WiFiMeasureDAO
}
