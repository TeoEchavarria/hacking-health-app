package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.SensorSample
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.SensorBatch
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.SensorBatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for writing sensor batches to the database
 * Processes sensor data received from the watch
 */
class SensorBatchWriterWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository: SensorBatchRepository by lazy {
        SensorBatchRepository(context)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get sensor data from input
            val sensorType = inputData.getString(KEY_SENSOR_TYPE) ?: return@withContext Result.failure()
            val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
            val samplesJson = inputData.getString(KEY_SAMPLES_JSON) ?: return@withContext Result.failure()
            
            // Parse samples (expect JSON array from input)
            // For simplicity, assume samples are already parsed objects passed via WorkManager
            // In real implementation, deserialize from JSON
            
            // Create sensor batch
            val batch = SensorBatch(
                timestamp = timestamp,
                sensorType = sensorType,
                samples = emptyList(), // Will be populated from actual data
                uploaded = false
            )
            
            // Save to database
            repository.saveBatch(batch)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to write sensor batch", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SensorBatchWriter"
        const val KEY_SENSOR_TYPE = "sensor_type"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_SAMPLES_JSON = "samples_json"
    }
}
