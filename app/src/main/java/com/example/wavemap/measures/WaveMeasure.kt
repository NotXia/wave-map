package com.example.wavemap.measures

import androidx.room.ColumnInfo

interface WaveMeasure {
    val value: Double
    val timestamp: Long
    val latitude: Double
    val longitude: Double
    val info: String        // Additional information

}