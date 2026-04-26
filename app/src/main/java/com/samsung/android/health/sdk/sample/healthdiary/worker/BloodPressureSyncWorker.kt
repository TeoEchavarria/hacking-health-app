package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.SyncStatus
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import java.util.concurrent.TimeUnit

/**
 * Worker that syncs pending blood pressure readings to the backend.
 *
 * Uses the offline-first sync state machine:
 * PENDING/FAILED → SYNCING → SYNCED (or back to FAILED)
 *
 * Features:
 * - Idempotency keys prevent duplicate entries on retry
 * - Exponential backoff on network failures
 * - Processes readings in chronological order
 * - Updates local DB with server response (stage, severity, alert status)
 */
class BloodPressureSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "BPSyncWorker"
        private const val WORK_NAME = "bp_sync"
        
        /**
         * Schedule an immediate one-time sync.
         * Called after capturing a new BP reading.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<BloodPressureSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP, // Don't duplicate if already running
                request
            )
            
            Log.i(TAG, "📤 BP sync worker enqueued")
        }
        
        /**
         * Cancel any pending sync work.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "❌ BP sync worker cancelled")
        }
    }
    
    override suspend fun doWork(): Result {
        Log.i(TAG, "⏰ Starting BP sync...")
        
        val token = TokenManager.getToken()
        if (token == null) {
            Log.w(TAG, "Not authenticated, skipping sync")
            return Result.success()
        }
        
        val dao = AppDatabase.getDatabase(applicationContext).bloodPressureReadingDao()
        val apiService = RetrofitClient.bloodPressureApiService
        
        val pendingReadings = dao.getPendingReadings()
        
        if (pendingReadings.isEmpty()) {
            Log.i(TAG, "✅ No pending BP readings to sync")
            return Result.success()
        }
        
        Log.i(TAG, "📊 Found ${pendingReadings.size} pending BP readings")
        
        var successCount = 0
        var failCount = 0
        
        for (reading in pendingReadings) {
            val now = System.currentTimeMillis()
            
            // Mark as syncing
            dao.updateSyncStatus(
                key = reading.idempotencyKey,
                status = SyncStatus.SYNCING,
                timestamp = now
            )
            
            try {
                val response = apiService.uploadBloodPressure(
                    token = "Bearer $token",
                    idempotencyKey = reading.idempotencyKey,
                    request = reading.toRequest()
                )
                
                if (response.success) {
                    // Mark as synced with server response data
                    dao.updateSyncSuccess(
                        key = reading.idempotencyKey,
                        status = SyncStatus.SYNCED,
                        timestamp = System.currentTimeMillis(),
                        stage = response.stage,
                        severity = response.severity,
                        alertGenerated = response.alertGenerated
                    )
                    successCount++
                    Log.i(TAG, "✅ Synced reading ${reading.idempotencyKey}: ${response.stage}")
                } else {
                    // API returned failure
                    dao.updateSyncStatus(
                        key = reading.idempotencyKey,
                        status = SyncStatus.FAILED,
                        timestamp = System.currentTimeMillis(),
                        error = response.message ?: "API returned success=false"
                    )
                    failCount++
                    Log.w(TAG, "❌ API rejected reading ${reading.idempotencyKey}: ${response.message}")
                }
            } catch (e: Exception) {
                // Network or other error
                dao.updateSyncStatus(
                    key = reading.idempotencyKey,
                    status = SyncStatus.FAILED,
                    timestamp = System.currentTimeMillis(),
                    error = e.message ?: "Unknown error"
                )
                failCount++
                Log.e(TAG, "❌ Failed to sync reading ${reading.idempotencyKey}", e)
            }
        }
        
        Log.i(TAG, "📈 Sync complete: $successCount succeeded, $failCount failed")
        
        // Retry if there were failures
        return if (failCount > 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
