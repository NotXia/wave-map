package com.example.wavemap.utilities

class Constants {
    companion object {
        const val NOISE_DEFAULT_RANGE_BAD : Double = 130.0
        const val NOISE_DEFAULT_RANGE_GOOD : Double = 10.0

        const val WIFI_DEFAULT_RANGE_BAD : Double = -90.0
        const val WIFI_DEFAULT_RANGE_GOOD : Double = -30.0

        const val LTE_DEFAULT_RANGE_BAD : Double = -140.0
        const val LTE_DEFAULT_RANGE_GOOD : Double = -43.0

        const val BLUETOOTH_DEFAULT_RANGE_BAD : Double = -90.0
        const val BLUETOOTH_DEFAULT_RANGE_GOOD : Double = 10.0

        val HUE_MEASURE_RANGE = Pair(0.0, 150.0) // 0 -> Red | 150 -> Green

        const val RANGE_SIZE_DEFAULT : Int = 3

        const val DATABASE_NAME = "wave"

        const val TILE_AVERAGE_STEPS = 3

        const val SHARED_MERGE_TOLERANCE = 1000*60*10
    }
}