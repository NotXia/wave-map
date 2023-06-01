package com.example.wavemap.utilities

import android.content.Context
import android.os.PowerManager
import androidx.core.content.ContextCompat

class Misc {
    companion object {
        fun isBatteryOptimizationOn(context: Context) : Boolean {
            val power_manager = ContextCompat.getSystemService(context, PowerManager::class.java)
                ?: return false

            return !power_manager.isIgnoringBatteryOptimizations(context.packageName)
        }
    }
}