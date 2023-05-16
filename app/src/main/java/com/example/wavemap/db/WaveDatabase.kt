package com.example.wavemap.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities=[MeasureTable::class, BSSIDTable::class], version=1)
abstract class WaveDatabase : RoomDatabase() {
    abstract fun measureDAO() : MeasureDAO
    abstract fun bssidDAO() : BSSIDDAO
}
