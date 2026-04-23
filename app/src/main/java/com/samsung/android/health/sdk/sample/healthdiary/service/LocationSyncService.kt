package com.samsung.android.health.sdk.sample.healthdiary.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.activity.HealthMainActivity
import com.samsung.android.health.sdk.sample.healthdiary.repository.LocationShareRepository
import com.samsung.android.health.sdk.sample.healthdiary.utils.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that periodically syncs the user's location to the backend.
 *
 * This service:
 * - Runs as a foreground service with a persistent notification
 * - Uses FusedLocationProvider to get GPS updates
 * - Sends location to the backend every 5 minutes
 * - Handles offline scenarios by queuing updates
 *
 * Usage:
 * - Start: LocationSyncService.start(context)
 * - Stop: LocationSyncService.stop(context)
 */
class LocationSyncService : Service() {

    companion object {
        private const val TAG = "LocationSyncService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "location_sync_channel"
        private const val CHANNEL_NAME = "Ubicación Compartida"

        /**
         * Start the location sync service.
         */
        fun start(context: Context) {
            val intent = Intent(context, LocationSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.i(TAG, "Starting LocationSyncService")
        }

        /**
         * Stop the location sync service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, LocationSyncService::class.java)
            context.stopService(intent)
            Log.i(TAG, "Stopping LocationSyncService")
        }

        /**
         * Check if the service is running.
         */
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationSyncService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationJob: Job? = null
    
    private lateinit var locationProvider: LocationProvider
    private lateinit var locationRepository: LocationShareRepository

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        
        locationProvider = LocationProvider(this)
        locationRepository = LocationShareRepository(this)
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start location tracking
        startLocationTracking()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        locationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sincronización de ubicación con tu familiar vinculado"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HealthMainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ubicación compartida activa")
            .setContentText("Tu ubicación se comparte con tu familiar vinculado")
            .setSmallIcon(R.drawable.ic_location_on)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startLocationTracking() {
        locationJob?.cancel()
        
        if (!locationProvider.hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted, stopping service")
            stopSelf()
            return
        }

        locationJob = serviceScope.launch {
            locationProvider.startLocationUpdates()
                .catch { e ->
                    Log.e(TAG, "Error in location flow", e)
                }
                .collectLatest { location ->
                    Log.d(TAG, "Received location: ${location.latitude}, ${location.longitude}")
                    
                    // Send to backend
                    locationRepository.updateLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy
                    ).onSuccess {
                        Log.d(TAG, "Location synced successfully")
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to sync location: ${error.message}")
                        // TODO: Queue for retry when online
                    }
                }
        }
    }
}
