package com.samsung.android.health.sdk.sample.healthdiary.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper for FusedLocationProviderClient that provides location access
 * via Kotlin coroutines and Flow.
 *
 * Handles permission checking and provides both one-shot and continuous
 * location updates.
 */
class LocationProvider(private val context: Context) {

    companion object {
        private const val TAG = "LocationProvider"
        
        // Update intervals for continuous tracking
        const val UPDATE_INTERVAL_MS = 5 * 60 * 1000L  // 5 minutes
        const val FASTEST_INTERVAL_MS = 1 * 60 * 1000L // 1 minute minimum
        
        // Update intervals for active map viewing
        const val ACTIVE_UPDATE_INTERVAL_MS = 30 * 1000L  // 30 seconds
        const val ACTIVE_FASTEST_INTERVAL_MS = 15 * 1000L // 15 seconds
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check if location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if background location permission is granted.
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android Q
        }
    }

    /**
     * Get the last known location (quick, no GPS activation).
     *
     * @return Last known location or null if unavailable
     */
    suspend fun getLastLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    Log.d(TAG, "Last location: ${location?.latitude}, ${location?.longitude}")
                    continuation.resume(location)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting last location", exception)
                    continuation.resume(null)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last location", e)
            continuation.resume(null)
        }
    }

    /**
     * Get current location (activates GPS if needed).
     *
     * @param priority Location priority (default: high accuracy)
     * @return Current location
     * @throws Exception if location cannot be obtained
     */
    suspend fun getCurrentLocation(
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): Location = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resumeWithException(
                SecurityException("Location permission not granted")
            )
            return@suspendCancellableCoroutine
        }

        try {
            val locationRequest = LocationRequest.Builder(priority, 10000L)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000L)
                .setMaxUpdates(1)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    if (location != null) {
                        Log.d(TAG, "Current location: ${location.latitude}, ${location.longitude}")
                        continuation.resume(location)
                    } else {
                        continuation.resumeWithException(
                            Exception("Could not get current location")
                        )
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting current location", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Start continuous location updates.
     *
     * @param intervalMs Update interval in milliseconds
     * @param fastestIntervalMs Fastest update interval
     * @return Flow emitting location updates
     */
    fun startLocationUpdates(
        intervalMs: Long = UPDATE_INTERVAL_MS,
        fastestIntervalMs: Long = FASTEST_INTERVAL_MS
    ): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalMs
        )
            .setMinUpdateIntervalMillis(fastestIntervalMs)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude}")
                    trySend(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.i(TAG, "Started location updates with interval ${intervalMs}ms")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates", e)
            close(e)
            return@callbackFlow
        }

        awaitClose {
            Log.i(TAG, "Stopping location updates")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Start high-frequency location updates for active map viewing.
     *
     * @return Flow emitting location updates
     */
    fun startActiveLocationUpdates(): Flow<Location> = startLocationUpdates(
        intervalMs = ACTIVE_UPDATE_INTERVAL_MS,
        fastestIntervalMs = ACTIVE_FASTEST_INTERVAL_MS
    )
}
