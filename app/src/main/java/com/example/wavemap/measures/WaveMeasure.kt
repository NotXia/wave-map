package com.example.wavemap.measures

interface WaveMeasure {
    val value: Double
    val timestamp: Long
    val latitude: Double
    val longitude: Double
}