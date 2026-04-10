package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SyncCompleteRequest
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import java.util.concurrent.TimeUnit

/**
 * Worker that polls for pending sync requests from caregivers.
 * 
 * Checks every 1 minute if there's a pending sync request.
 * If found, triggers HealthSyncWorker and marks the request complete.
 * 
 * Uses chained OneTimeWork to achieve < 15 minute intervals
 * (WorkManager minimum for PeriodicWork is 15 min).
 */
class SyncPollingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncPollingWorker"
        private const val WORK_NAME = "sync_polling"
        private const val POLL_INTERVAL_MINUTES = 1L
        
        /**
         * Start polling for sync requests.
         * Runs every 1 minute when network is available.
         */
        fun start(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<SyncPollingWorker>()
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()
            
            WorkManager.getInstance(context).enqueue(request)
            Log.i(TAG, "🔄 Sync polling started")
        }
        
        /**
         * Stop polling for sync requests.
         */
        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_NAME)
            Log.i(TAG, "❌ Sync polling stopped")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔍 Checking for pending sync requests...")
        
        try {
            val token = TokenManager.getToken()
            
            if (token == null) {
                Log.w(TAG, "Not authenticated, skipping poll")
                scheduleNext()
                return Result.success()
            }
            
            // Check for pending sync requests
            val api = RetrofitClient.patientHealthApiService
            val response = api.getPendingSyncRequest("Bearer $token")
            
            if (response.isSuccessful) {
                val pendingSync = response.body()
                
                if (pendingSync?.hasPending == true) {
                    Log.i(TAG, "📥 Pending sync request found: ${pendingSync.requestId}")
                    
                    // Trigger immediate sync
                    HealthSyncWorker.triggerNow(applicationContext)
                    
                    // Mark as complete
                    pendingSync.requestId?.let { requestId ->
                        completeSyncRequest(token, requestId)
                    }
                } else {
                    Log.d(TAG, "No pending sync requests")
                }
            } else {
                Log.w(TAG, "Failed to check pending sync: ${response.code()}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking pending sync: ${e.message}")
        }
        
        // Schedule next poll
        scheduleNext()
        
        return Result.success()
    }
    
    private suspend fun completeSyncRequest(token: String, requestId: String) {
        try {
            val api = RetrofitClient.patientHealthApiService
            val request = SyncCompleteRequest(requestId = requestId, metricsSynced = 0)
            val response = api.completeSyncRequest("Bearer $token", request)
            
            if (response.isSuccessful) {
                Log.i(TAG, "✅ Sync request completed: $requestId")
            } else {
                Log.w(TAG, "Failed to complete sync request: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error completing sync request: ${e.message}")
        }
    }
    
    private fun scheduleNext() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val nextRequest = OneTimeWorkRequestBuilder<SyncPollingWorker>()
            .setConstraints(constraints)
            .setInitialDelay(POLL_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build()
        
        WorkManager.getInstance(applicationContext).enqueue(nextRequest)
        Log.d(TAG, "📆 Next poll scheduled in $POLL_INTERVAL_MINUTES minute(s)")
    }
}
