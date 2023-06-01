package com.example.wavemap.utilities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

class Permissions {
    companion object {

        val gps : Array<String>
            get() = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        val wifi : Array<String>
            get() = gps

        val lte : Array<String>
            get() = gps

        val noise : Array<String>
            get() = arrayOf(
                Manifest.permission.RECORD_AUDIO
            )

        val bluetooth : Array<String>
            get() = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
                else -> arrayOf()
        }

        val notification : Array<String>
            get() = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                else -> arrayOf()
            }

        val requiredAtInit : Array<String>
            get() = gps + wifi + lte + noise + bluetooth + notification

        val minimumRequired : Array<String>
            get() = gps


        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }
    }

}