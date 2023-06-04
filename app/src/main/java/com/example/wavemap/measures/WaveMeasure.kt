package com.example.wavemap.measures

interface WaveMeasure {
    val author: String
    val value: Double
    val timestamp: Long
    val latitude: Double
    val longitude: Double
    val info: String        // Additional information
}