package com.example.wavemap.utilities

import android.content.Context
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.round

class Misc {
    companion object {
        fun isBatteryOptimizationOn(context: Context) : Boolean {
            val power_manager = ContextCompat.getSystemService(context, PowerManager::class.java)
                ?: return false

            return !power_manager.isIgnoringBatteryOptimizations(context.packageName)
        }

        /* Scales a value of an interval to another */
        fun scaleToInterval(value: Double, source_range: Pair<Double, Double>, target_range: Pair<Double, Double>) : Double {
            var real_source = source_range
            var real_value = value

            // Range extremes have to be inverted and value rescaled
            if (source_range.first > source_range.second) {
                real_value = if (value > (source_range.first - source_range.second) / 2) {
                    value + (source_range.first - source_range.second) - 2 * (value - source_range.second)
                }
                else {
                    value - (source_range.first - source_range.second) + 2 * (source_range.first - value)
                }
                real_source = Pair(source_range.second, source_range.first)
            }

            if (real_value <= real_source.first) { return target_range.first }
            if (real_value >= real_source.second) { return target_range.second }

            val source_distance = (real_source.second - real_source.first)
            val target_distance = (target_range.second - target_range.first)
            return ( ( ((real_value - real_source.first) * target_distance) / source_distance ) + target_range.first )
        }

        /* Scales a value from a continuous interval to a discrete interval */
        fun scaleToRange(value: Double, range: Pair<Double, Double>, range_size: Int) : Double {
            if (range_size == 1) { return range.second }

            val range_value : Double = round(abs(range.second - range.first) / (range_size-1))
            var out = value + range_value/2
            out -= out % range_value

            if (out <= range.first) { return range.first }
            if (out >= range.second) { return range.second }
            return out
        }

    }
}