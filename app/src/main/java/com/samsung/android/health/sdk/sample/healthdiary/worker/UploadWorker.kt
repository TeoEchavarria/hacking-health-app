package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.SensorDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.samsung.android.health.sdk.sample.healthdiary.api.models.SensorBatchRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SensorRecordDto
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType
import java.io.IOException

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sensorDataDao = AppDatabase.getDatabase(context).sensorDataDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Initialize health monitor
        UploadHealthMonitor.initialize(applicationContext)
        UploadHealthMonitor.recordAttempt()
        
        try {
            Log.d(TAG, "UPLOAD_WORK_EXECUTED attempt=$runAttemptCount")
            TokenManager.initialize(applicationContext)
            
            // Update pending count for diagnostics
            val pendingCount = try { sensorDataDao.getUnsyncedCount() } catch (e: Exception) { -1 }
            if (pendingCount >= 0) {
                UploadHealthMonitor.updatePendingCount(pendingCount)
            }
            
            // Check token validity before starting
            if (!TokenManager.hasToken()) {
                 Log.e(TAG, "Upload skipped: No token available. Pending records: $pendingCount")
                 UploadHealthMonitor.recordFailure("NO_TOKEN")
                 // CRITICAL: Always schedule next even on retry to prevent chain break
                 UploadScheduler.scheduleNext(applicationContext)
                 return@withContext Result.retry()
            }

            val batchSize = 250
            var totalUploaded = 0
            
            while (true) {
                // 1. Fetch batch
                val dataToUpload = sensorDataDao.getUnsyncedBatch(batchSize)
                if (dataToUpload.isEmpty()) {
                    break
                }

                val startTime = System.currentTimeMillis()
                val firstTimestamp = dataToUpload.first().timestamp
                val lastTimestamp = dataToUpload.last().timestamp

                // 2. Map to API request format
                val records = dataToUpload.map { entity ->
                    SensorRecordDto(
                        timestamp = entity.timestamp,
                        x = entity.x,
                        y = entity.y,
                        z = entity.z
                    )
                }
                val request = SensorBatchRequest(records)

                try {
                    // 3. Call Backend
                    Log.d(TAG, "Uploading batch of ${dataToUpload.size} items... (Attempt $runAttemptCount)")
                    
                    // Note: RetrofitClient.authInterceptor will handle proactive refresh if needed
                    val response = RetrofitClient.syncApiService.uploadSensorData(request)
                    val latency = System.currentTimeMillis() - startTime

                    if (response.isSuccessful) {
                        // 4. Delete uploaded items ONLY on success
                        val ids = dataToUpload.map { it.id }
                        sensorDataDao.deleteByIds(ids)
                        totalUploaded += dataToUpload.size
                        
                        val remaining = sensorDataDao.getUnsyncedCount()
                        UploadHealthMonitor.updatePendingCount(remaining)
                        UploadHealthMonitor.recordSuccess(dataToUpload.size)
                        
                        val logMsg = "batch_size=${dataToUpload.size}, range=[$firstTimestamp..$lastTimestamp], attempt=$runAttemptCount, code=${response.code()}, latency=${latency}ms, result=SUCCESS, remaining=$remaining"
                        Log.d("ACCEL_PHONE_TO_API", logMsg)
                        Log.d(TAG, "Batch upload success, remaining unsent: $remaining")
                        ConnectionLogManager.log(LogType.SUCCESS, "ACCEL_PHONE_TO_API", logMsg)
                    } else {
                        val remaining = sensorDataDao.getUnsyncedCount()
                        
                        val errorReason = when (response.code()) {
                            401, 403 -> "TOKEN_INVALID"
                            in 500..599 -> "SERVER_ERROR"
                            else -> "API_ERROR_${response.code()}"
                        }
                        
                        val logMsg = "batch_size=${dataToUpload.size}, range=[$firstTimestamp..$lastTimestamp], attempt=$runAttemptCount, code=${response.code()}, latency=${latency}ms, result=FAILURE ($errorReason), remaining=$remaining"
                        Log.d("ACCEL_PHONE_TO_API", logMsg)
                        Log.e(TAG, "Upload failed: $errorReason")
                        ConnectionLogManager.log(LogType.ERROR, "ACCEL_PHONE_TO_API", logMsg)
                        UploadHealthMonitor.recordFailure(errorReason)
                        
                        // CRITICAL: Always schedule next even on retry to prevent chain break
                        UploadScheduler.scheduleNext(applicationContext)
                        return@withContext Result.retry()
                    }
                } catch (e: Exception) {
                    val latency = System.currentTimeMillis() - startTime
                    val remaining = try { sensorDataDao.getUnsyncedCount() } catch (ex: Exception) { -1 }
                    
                    val errorReason = if (e is IOException) "NETWORK_ERROR" else "EXCEPTION: ${e.javaClass.simpleName}"
                    
                    val logMsg = "batch_size=${dataToUpload.size}, range=[$firstTimestamp..$lastTimestamp], attempt=$runAttemptCount, code=-1, latency=${latency}ms, result=FAILURE ($errorReason), remaining=$remaining"
                    Log.d("ACCEL_PHONE_TO_API", logMsg)
                    Log.e(TAG, "Upload exception: $errorReason", e)
                    ConnectionLogManager.log(LogType.ERROR, "ACCEL_PHONE_TO_API", logMsg)
                    UploadHealthMonitor.recordFailure(errorReason)
                    
                    // CRITICAL: Always schedule next even on retry to prevent chain break
                    UploadScheduler.scheduleNext(applicationContext)
                    return@withContext Result.retry()
                }
            }
            
            // Log total uploaded in this run
            if (totalUploaded > 0) {
                Log.d(TAG, "UPLOAD_RUN_COMPLETE: uploaded $totalUploaded records total")
            }
            
            // Schedule next run (loop)
            UploadScheduler.scheduleNext(applicationContext)
            
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Worker critical failure", e)
            UploadHealthMonitor.recordFailure("CRITICAL: ${e.javaClass.simpleName}")
            // CRITICAL: Always schedule next even on critical failure
            UploadScheduler.scheduleNext(applicationContext)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadWorker"
    }
}
