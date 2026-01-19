package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig

import androidx.work.BackoffPolicy
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.utils.TelemetryLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages upload scheduling with resilience against silent stalls.
 * 
 * Uses a dual-scheduler approach:
 * 1. OneTimeWork for responsive uploads (chains after each batch)
 * 2. PeriodicWork as a safety net to recover from broken chains
 */
object UploadScheduler {

    private const val UNIQUE_WORK_NAME = "ACCEL_UPLOAD_WORK"
    private const val PERIODIC_WORK_NAME = "ACCEL_UPLOAD_PERIODIC_SAFETY"
    private const val TAG = "UploadScheduler"

    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Schedule the next upload attempt.
     * Called after receiving data or after successful upload.
     */
    fun scheduleNext(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 3 minutes for all builds
        val delaySeconds = 3L * 60L

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
            
        Log.d(TAG, "UPLOAD_WORK_SCHEDULED delay=${delaySeconds}s")
        UploadHealthMonitor.recordScheduled()
        
        // Log scheduling to UI
        TelemetryLogger.log(
            "PHONE",
            "Upload Scheduled",
            "[${getTimestamp()}] Next upload scheduled in 3 minutes."
        )

        // KEEP ensures that if a job is already scheduled (waiting out its delay), 
        // we don't reset the timer. We just let the existing one run.
        // Once it runs and finishes, it will schedule the next one.
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
    
    /**
     * Schedule a periodic safety net that runs every 15 minutes.
     * This ensures uploads resume even if the OneTimeWork chain breaks.
     * Should be called once at app startup.
     */
    fun ensurePeriodicSafetyNet(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Minimum periodic interval is 15 minutes
        val periodicRequest = PeriodicWorkRequestBuilder<UploadWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
        
        Log.d(TAG, "PERIODIC_SAFETY_NET_ENSURED")
    }
    
    /**
     * Force an immediate upload attempt, bypassing the delay.
     * Use for watchdog recovery or manual retry.
     */
    fun forceImmediateUpload(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            // No delay - run immediately when constraints are met
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        // REPLACE to force immediate execution
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        
        Log.d(TAG, "UPLOAD_FORCED_IMMEDIATE")
        TelemetryLogger.log(
            "PHONE",
            "Force Upload",
            "[${getTimestamp()}] Forced immediate upload triggered."
        )
    }

    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
