package com.musa.azanmy.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ZoneLocator {

    @SuppressLint("MissingPermission")
    suspend fun detectNearestZone(context: Context): Result<JakimZone> {
        return try {
            val location = getSingleLocation(context)
                ?: return Result.failure(IllegalStateException("Lokasi tidak dapat dikesan. Sila pastikan GPS aktif atau pilih zon secara manual."))

            val nearest = findClosestZone(location.latitude, location.longitude)
            Result.success(nearest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getSingleLocation(context: Context): Location? {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        val current = withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }
        if (current != null) return current

        val lastFused = withTimeoutOrNull(3_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }
        if (lastFused != null) return lastFused

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            null
        }
    }

    fun findClosestZone(lat: Double, lon: Double): JakimZone {
        return JakimZoneData.allZones.minByOrNull { zone ->
            haversineKm(lat, lon, zone.lat, zone.lon)
        } ?: JakimZoneData.findByCode("WLY01")
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
