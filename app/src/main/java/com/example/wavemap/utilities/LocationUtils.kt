package com.example.wavemap.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LocationUtils {
    companion object {

        suspend fun getCurrent(context: Context) : LatLng = suspendCoroutine { cont ->
            if ( ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                return@suspendCoroutine cont.resumeWithException( SecurityException("Missing ACCESS_FINE_LOCATION permissions") )
            }

            var fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
                if (location != null) {
                    cont.resume( LatLng(location.latitude, location.longitude) )
                }
                else {
                    cont.resumeWithException( RuntimeException("Cannot retrieve location") )
                }
            }
            .addOnCanceledListener { cont.resumeWithException( RuntimeException("Cannot retrieve location") ) }
        }

    }
}