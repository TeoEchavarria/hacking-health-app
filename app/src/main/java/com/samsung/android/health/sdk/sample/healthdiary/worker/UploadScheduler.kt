package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig

import androidx.work.BackoffPolicy
import android.util.Log

object UploadScheduler {

    private const val UNIQUE_WORK_NAME = "ACCEL_UPLOAD_WORK"
    private const val TAG = "UploadScheduler"

    fun scheduleNext(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Dev: 10 seconds, Prod: 5 minutes
        val isDev = BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "debug"
        val delaySeconds = if (isDev) 10L else 5L * 60L

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
            
        Log.d(TAG, "UPLOAD_WORK_SCHEDULED")

        // KEEP ensures that if a job is already scheduled (waiting out its delay), 
        // we don't reset the timer. We just let the existing one run.
        // Once it runs and finishes, it will schedule the next one.
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
