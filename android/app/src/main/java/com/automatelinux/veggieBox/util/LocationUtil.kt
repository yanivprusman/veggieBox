package com.automatelinux.veggieBox.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.widget.Toast

object LocationUtil {
    /**
     * One-shot current location. Tries last-known first (instant), then asks for a
     * single fresh fix. Caller must already hold a location permission.
     */
    @SuppressLint("MissingPermission")
    fun requestOneShot(ctx: Context, onResult: (lat: Double, lon: Double) -> Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val last: Location? =
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (last != null) {
            onResult(last.latitude, last.longitude)
            return
        }

        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            Toast.makeText(ctx, "המיקום כבוי — הפעילו GPS", Toast.LENGTH_SHORT).show()
            return
        }

        @Suppress("DEPRECATION")
        lm.requestSingleUpdate(
            provider,
            { loc -> onResult(loc.latitude, loc.longitude) },
            Looper.getMainLooper(),
        )
    }
}
