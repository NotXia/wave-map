package com.example.wavemap.utilities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

class Permissions {
    companion object {

        fun check(context: Context, permissions: Array<String>) : Boolean {
            return permissions.all{ permission -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED }
        }

        val gps : Array<String>
            get() = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        val background_gps : Array<String>
            get() = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                else -> arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            }

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

        val disk : Array<String>
            get() = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                else -> arrayOf()
            }

        val allRequired : Array<String>
            get() = gps + wifi + lte + noise + bluetooth + notification

        val minimumRequired : Array<String>
            get() = gps


        fun openAppSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }

        fun openWiFiSettings(context: Context) {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            context.startActivity(intent)
        }

        fun openBluetoothSettings(context: Context) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            context.startActivity(intent)
        }
    }

}