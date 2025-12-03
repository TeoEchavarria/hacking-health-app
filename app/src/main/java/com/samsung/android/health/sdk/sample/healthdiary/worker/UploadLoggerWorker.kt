package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.EntityType
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UploadLog
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UploadStatus
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.UploadLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for logging upload operations
 * Creates audit trail of sync activities
 */
class UploadLoggerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository: UploadLogRepository by lazy {
        UploadLogRepository(context)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get log data from input
            val entityType = inputData.getString(KEY_ENTITY_TYPE) ?: return@withContext Result.failure()
            val entityId = inputData.getLong(KEY_ENTITY_ID, -1L)
            val status = inputData.getString(KEY_STATUS) ?: return@withContext Result.failure()
            val endpoint = inputData.getString(KEY_ENDPOINT) ?: ""
            val responseCode = inputData.getInt(KEY_RESPONSE_CODE, -1).takeIf { it > 0 }
            val errorMessage = inputData.getString(KEY_ERROR_MESSAGE)
            val dataSizeBytes = inputData.getLong(KEY_DATA_SIZE_BYTES, -1L).takeIf { it > 0 }
            val durationMs = inputData.getLong(KEY_DURATION_MS, -1L).takeIf { it > 0 }
            
            if (entityId < 0) return@withContext Result.failure()
            
            // Create upload log
            val log = UploadLog(
                timestamp = System.currentTimeMillis(),
                entityType = EntityType.fromValue(entityType),
                entityId = entityId,
                status = UploadStatus.fromValue(status),
                endpoint = endpoint,
                responseCode = responseCode,
                errorMessage = errorMessage,
                dataSizeBytes = dataSizeBytes,
                durationMs = durationMs
            )
            
            // Save to database
            repository.logUpload(log)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to log upload", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadLogger"
        const val KEY_ENTITY_TYPE = "entity_type"
        const val KEY_ENTITY_ID = "entity_id"
        const val KEY_STATUS = "status"
        const val KEY_ENDPOINT = "endpoint"
        const val KEY_RESPONSE_CODE = "response_code"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_DATA_SIZE_BYTES = "data_size_bytes"
        const val KEY_DURATION_MS = "duration_ms"
    }
}
