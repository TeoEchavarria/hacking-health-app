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
import com.samsung.android.health.sdk.sample.healthdiary.utils.TelemetryLogger
import java.io.IOException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sensorDataDao = AppDatabase.getDatabase(context).sensorDataDao()

    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Initialize health monitor
        UploadHealthMonitor.initialize(applicationContext)
        UploadHealthMonitor.recordAttempt()
        
        try {
            Log.d(TAG, "UPLOAD_WORK_EXECUTED attempt=$runAttemptCount")
            
            // Update pending count for diagnostics
            val pendingCount = try { sensorDataDao.getUnsyncedCount() } catch (e: Exception) { -1 }
            if (pendingCount >= 0) {
                UploadHealthMonitor.updatePendingCount(pendingCount)
            }
            
            // Log upload attempt to UI
            TelemetryLogger.log(
                "PHONE",
                "Upload Started",
                "[${getTimestamp()}] Starting upload attempt #$runAttemptCount. Pending: $pendingCount records."
            )
            
            // Check network connectivity first
            if (!isNetworkAvailable()) {
                Log.w(TAG, "Upload skipped: No internet connectivity. Pending records: $pendingCount")
                UploadHealthMonitor.recordFailure("NO_INTERNET")
                TelemetryLogger.log(
                    "PHONE",
                    "No Internet",
                    "[${getTimestamp()}] No internet connection. $pendingCount records stored for later retry."
                )
                UploadScheduler.scheduleNext(applicationContext)
                return@withContext Result.retry()
            }
            
            // No token check - auth disabled for dev/testing

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
                    // 3. Call Backend (no auth required - dev/testing mode)
                    Log.d(TAG, "Uploading batch of ${dataToUpload.size} items... (Attempt $runAttemptCount)")
                    
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
                        
                        // Log success to UI
                        TelemetryLogger.log(
                            "API",
                            "Upload Success",
                            "[${getTimestamp()}] Uploaded ${dataToUpload.size} records. Response: ${response.code()}. Latency: ${latency}ms. Remaining: $remaining"
                        )
                    } else {
                        val remaining = sensorDataDao.getUnsyncedCount()
                        
                        // Simplified error handling - only retry on server errors (5xx)
                        val errorReason = when (response.code()) {
                            in 500..599 -> "SERVER_ERROR"
                            else -> "API_ERROR_${response.code()}"
                        }
                        
                        val logMsg = "batch_size=${dataToUpload.size}, range=[$firstTimestamp..$lastTimestamp], attempt=$runAttemptCount, code=${response.code()}, latency=${latency}ms, result=FAILURE ($errorReason), remaining=$remaining"
                        Log.d("ACCEL_PHONE_TO_API", logMsg)
                        Log.e(TAG, "Upload failed: $errorReason")
                        ConnectionLogManager.log(LogType.ERROR, "ACCEL_PHONE_TO_API", logMsg)
                        UploadHealthMonitor.recordFailure(errorReason)
                        
                        // Log failure to UI
                        TelemetryLogger.log(
                            "API",
                            "Upload Failed",
                            "[${getTimestamp()}] API error: $errorReason (HTTP ${response.code()}). $remaining records pending. Will retry."
                        )
                        
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
                    
                    // Log network error to UI
                    val errorMsg = if (e is IOException) {
                        "Network error: ${e.message}. $remaining records stored for later retry."
                    } else {
                        "Exception: ${e.javaClass.simpleName} - ${e.message}. $remaining records pending."
                    }
                    TelemetryLogger.log(
                        "API",
                        "Upload Error",
                        "[${getTimestamp()}] $errorMsg"
                    )
                    
                    // CRITICAL: Always schedule next even on retry to prevent chain break
                    UploadScheduler.scheduleNext(applicationContext)
                    return@withContext Result.retry()
                }
            }
            
            // Log total uploaded in this run
            if (totalUploaded > 0) {
                Log.d(TAG, "UPLOAD_RUN_COMPLETE: uploaded $totalUploaded records total")
                TelemetryLogger.log(
                    "API",
                    "Upload Complete",
                    "[${getTimestamp()}] Successfully uploaded $totalUploaded records in this run."
                )
            } else {
                val pendingNow = try { sensorDataDao.getUnsyncedCount() } catch (e: Exception) { 0 }
                if (pendingNow == 0) {
                    TelemetryLogger.log(
                        "PHONE",
                        "No Data",
                        "[${getTimestamp()}] No pending records to upload."
                    )
                }
            }
            
            // Schedule next run (loop)
            UploadScheduler.scheduleNext(applicationContext)
            
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Worker critical failure", e)
            UploadHealthMonitor.recordFailure("CRITICAL: ${e.javaClass.simpleName}")
            TelemetryLogger.log(
                "PHONE",
                "Critical Error",
                "[${getTimestamp()}] Worker critical failure: ${e.javaClass.simpleName} - ${e.message}"
            )
            // CRITICAL: Always schedule next even on critical failure
            UploadScheduler.scheduleNext(applicationContext)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadWorker"
    }
}
