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
        try {
            Log.d(TAG, "UPLOAD_WORK_EXECUTED")
            TokenManager.initialize(applicationContext)
            
            // Check token validity before starting
            if (!TokenManager.hasToken()) {
                 val pending = try { sensorDataDao.getUnsyncedCount() } catch (e: Exception) { -1 }
                 Log.e(TAG, "Upload skipped: No token available. Pending records: $pending")
                 return@withContext Result.retry()
            }

            val batchSize = 250
            
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
                        
                        val remaining = sensorDataDao.getUnsyncedCount()
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
                        
                        // If token is invalid, we might want to back off longer, but WorkManager handles exponential backoff.
                        return@withContext Result.retry()
                    }
                } catch (e: Exception) {
                    val latency = System.currentTimeMillis() - startTime
                    val remaining = try { sensorDataDao.getUnsyncedCount() } catch (ex: Exception) { -1 }
                    
                    val errorReason = if (e is IOException) "NETWORK_ERROR" else "EXCEPTION"
                    
                    val logMsg = "batch_size=${dataToUpload.size}, range=[$firstTimestamp..$lastTimestamp], attempt=$runAttemptCount, code=-1, latency=${latency}ms, result=FAILURE ($errorReason), remaining=$remaining"
                    Log.d("ACCEL_PHONE_TO_API", logMsg)
                    Log.e(TAG, "Upload exception: $errorReason", e)
                    ConnectionLogManager.log(LogType.ERROR, "ACCEL_PHONE_TO_API", logMsg)
                    return@withContext Result.retry()
                }
            }
            
            // Schedule next run (loop)
            UploadScheduler.scheduleNext(applicationContext)
            
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Worker critical failure", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadWorker"
    }
}
