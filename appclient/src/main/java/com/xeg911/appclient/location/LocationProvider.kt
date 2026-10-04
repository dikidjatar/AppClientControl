package com.xeg911.appclient.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.permission.PermissionChecker
import com.xeg911.shared.data.model.LocationSnapshot
import com.xeg911.shared.data.model.LocationSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over FusedLocationProviderClient. Callers must check
 * [hasForegroundPermission] first; nothing here requests permissions.
 */
@Singleton
class LocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val locationManager by lazy { context.getSystemService(LocationManager::class.java) }

    fun hasForegroundPermission(): Boolean =
        permissionChecker.isGranted(AppPermission.ACCESS_FINE_LOCATION) ||
                permissionChecker.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasBackgroundPermission(): Boolean =
        permissionChecker.isGranted(AppPermission.ACCESS_BACKGROUND_LOCATION)

    fun isLocationEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)

    /**
     * Continuous updates at [intervalMs], the callback is removed when the collector cancels.
     */
    @SuppressLint("MissingPermission")
    fun updates(intervalMs: Long): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    /**
     * Fresh single fix, falling back to the last known location.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = runCatching {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_FIX_AGE_MS)
            .setDurationMillis(FIX_TIMEOUT_MS)
            .build()
        client.getCurrentLocation(request, null).await() ?: client.lastLocation.await()
    }.getOrNull()

    private companion object {
        const val MAX_FIX_AGE_MS = 10_000L
        const val FIX_TIMEOUT_MS = 20_000L
    }
}

fun Location.toSnapshot(source: LocationSource, requestId: Long = 0L) = LocationSnapshot(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy else 0f,
    altitudeMeters = if (hasAltitude()) altitude else 0.0,
    speedMps = if (hasSpeed()) speed else 0f,
    bearingDegrees = if (hasBearing()) bearing else 0f,
    provider = provider.orEmpty(),
    source = source,
    requestId = requestId,
    fixedAt = time,
    capturedAt = System.currentTimeMillis(),
)
