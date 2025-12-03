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

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sensorDataDao = AppDatabase.getDatabase(context).sensorDataDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch unsynced data
            val dataToUpload = sensorDataDao.getUnsynced()
            if (dataToUpload.isEmpty()) {
                return@withContext Result.success()
            }

            Log.d(TAG, "Uploading ${dataToUpload.size} items")

            // 2. Map to API request format
            val records = dataToUpload.map { entity ->
                val values = entity.values.split(",").map { it.toFloat() }
                SensorRecordDto(
                    deviceId = entity.deviceId,
                    timestamp = entity.timestamp,
                    x = values.getOrElse(0) { 0f },
                    y = values.getOrElse(1) { 0f },
                    z = values.getOrElse(2) { 0f }
                )
            }
            val request = SensorBatchRequest(records)

            // 3. Call Backend
            val response = RetrofitClient.syncApiService.uploadSensorData(request)

            if (response.isSuccessful) {
                Log.d(TAG, "Upload success, deleting local data")
                // 4. Delete uploaded items
                val ids = dataToUpload.map { it.id }
                sensorDataDao.deleteByIds(ids)
                Result.success()
            } else {
                Log.e(TAG, "Upload failed with code: ${response.code()}")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadWorker"
    }
}
