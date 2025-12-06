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
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sensorDataDao = AppDatabase.getDatabase(context).sensorDataDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
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
                        deviceId = entity.deviceId,
                        timestamp = entity.timestamp,
                        x = entity.x,
                        y = entity.y,
                        z = entity.z
                    )
                }
                val request = SensorBatchRequest(records)

                try {
                    // 3. Call Backend
                    Log.d(TAG, "Uploading batch of ${dataToUpload.size} items...")
                    val response = RetrofitClient.syncApiService.uploadSensorData(request)
                    val latency = System.currentTimeMillis() - startTime

                    if (response.isSuccessful) {
                        // 4. Delete uploaded items
                        val ids = dataToUpload.map { it.id }
                        sensorDataDao.deleteByIds(ids)
                        
                        val remaining = sensorDataDao.getUnsyncedCount()
                        val logMsg = "batch_size=${dataToUpload.size}, range=[$firstTimestamp..$lastTimestamp], attempt=$runAttemptCount, code=${response.code()}, latency=${latency}ms, result=SUCCESS, remaining=$remaining"
                        Log.d("ACCEL_PHONE_TO_API", logMsg)
                        Log.d(TAG, "Batch upload success, remaining unsent: $remaining")
                        ConnectionLogManager.log(LogType.SUCCESS, "ACCEL_PHONE_TO_API", logMsg)
                    } else {
                        val remaining = sensorDataDao.getUnsyncedCount()
                        val logMsg = "batch_size=${dataToUpload.size}, range=[$firstTimestamp..$lastTimestamp], attempt=$runAttemptCount, code=${response.code()}, latency=${latency}ms, result=FAILURE, remaining=$remaining"
                        Log.d("ACCEL_PHONE_TO_API", logMsg)
                        Log.e(TAG, "Upload failed with code: ${response.code()}")
                        ConnectionLogManager.log(LogType.ERROR, "ACCEL_PHONE_TO_API", logMsg)
                        return@withContext Result.retry()
                    }
                } catch (e: Exception) {
                    val latency = System.currentTimeMillis() - startTime
                    // Just to be safe, wrap getUnsyncedCount
                    val remaining = try { sensorDataDao.getUnsyncedCount() } catch (ex: Exception) { -1 }
                    
                    val logMsg = "batch_size=${dataToUpload.size}, range=[$firstTimestamp..$lastTimestamp], attempt=$runAttemptCount, code=-1, latency=${latency}ms, result=FAILURE_EXCEPTION, remaining=$remaining"
                    Log.d("ACCEL_PHONE_TO_API", logMsg)
                    Log.e(TAG, "Upload exception", e)
                    ConnectionLogManager.log(LogType.ERROR, "ACCEL_PHONE_TO_API", logMsg)
                    return@withContext Result.retry()
                }
            }
            
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
