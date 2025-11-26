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

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sensorDataDao = AppDatabase.getDatabase(context).sensorDataDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dataToUpload = sensorDataDao.getAll()
            if (dataToUpload.isEmpty()) {
                return@withContext Result.success()
            }

            Log.d(TAG, "Uploading ${dataToUpload.size} items")

            // TODO: Implement actual API call here using RetrofitClient
            // For now, we simulate success and delete local data
            // val response = RetrofitClient.apiService.uploadData(dataToUpload)
            
            // Simulate upload delay
            // delay(1000)

            // On success, delete uploaded items
            val ids = dataToUpload.map { it.id }
            sensorDataDao.deleteByIds(ids)
            
            Log.d(TAG, "Upload success, deleted local data")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadWorker"
    }
}
