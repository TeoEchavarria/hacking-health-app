package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.HealthMetricsRequest
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that syncs health data to backend every 15 minutes.
 * 
 * This ensures data reaches the backend even if immediate sync fails.
 * Runs with network constraint to only sync when connected.
 */
class HealthSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "HealthSyncWorker"
        private const val WORK_NAME = "health_sync_periodic"
        
        /**
         * Schedule the periodic health sync worker.
         * Runs every 15 minutes when network is available.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            
            Log.i(TAG, "📅 Health sync worker scheduled (every 15 min)")
            
            // Also trigger an immediate sync on first run
            triggerNow(context)
        }
        
        /**
         * Trigger an immediate sync (one-time).
         */
        fun triggerNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<HealthSyncWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueue(request)
            Log.i(TAG, "🚀 Immediate health sync triggered")
        }
        
        /**
         * Cancel the periodic worker.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "❌ Health sync worker cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "⏰ Starting periodic health sync...")
        
        return try {
            val token = TokenManager.getToken()
            val userId = TokenManager.getUserIdFromToken()
            
            if (token == null || userId == null) {
                Log.w(TAG, "Not authenticated, skipping sync")
                return Result.success()
            }
            
            val repo = WatchHealthIngestionRepository(applicationContext)
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Get latest data from local DB
            val latestData = repo.getLatestDataForSync(today)
            
            if (latestData == null || !latestData.hasData) {
                Log.i(TAG, "No data to sync for today ($today)")
                return Result.success()
            }
            
            Log.i(TAG, "Syncing data for $today: steps=${latestData.steps}, sleep=${latestData.sleepMinutes}min, hr_avg=${latestData.avgHeartRate}")
            
            val request = HealthMetricsRequest(
                userId = userId,
                date = today,
                steps = latestData.steps,
                sleepMinutes = latestData.sleepMinutes,
                avgHeartRate = latestData.avgHeartRate,
                minHeartRate = latestData.minHeartRate,
                maxHeartRate = latestData.maxHeartRate,
                syncTimestamp = System.currentTimeMillis()
            )
            
            val api = RetrofitClient.healthMetricsApiService
            val response = api.uploadHealthMetrics("Bearer $token", request)
            
            if (response.success) {
                Log.i(TAG, "✅ Periodic sync success: ${response.metricsStored} metrics stored")
                Result.success()
            } else {
                Log.w(TAG, "❌ Sync API returned error: ${response.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync error: ${e.message}", e)
            Result.retry()
        }
    }
}
