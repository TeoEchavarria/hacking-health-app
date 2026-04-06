package com.samsung.android.health.sdk.sample.healthdiary.data.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.HealthMetricsRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.HeartRateSampleApi
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.WatchHealthDao
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.*
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.HealthDailySummary
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.HeartRateSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for ingesting and storing health data received directly from the watch.
 * 
 * This is the PRIMARY source of truth for health metrics:
 * - Steps (daily)
 * - Heart rate (individual samples)
 * - Sleep (daily duration)
 * 
 * Samsung Health on the phone is SUPPLEMENTARY only and does not flow through this repository.
 */
class WatchHealthIngestionRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "WatchHealthIngestion"
    }
    
    private val watchHealthDao: WatchHealthDao by lazy {
        AppDatabase.getDatabase(context).watchHealthDao()
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ============ INGESTION FROM WATCH ============
    
    /**
     * Ingest a daily health summary received from the watch.
     * This is the primary sync path for daily aggregated data.
     */
    suspend fun ingestDailySummary(summary: HealthDailySummary) {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Ingesting daily summary: date=${summary.date}, steps=${summary.steps}, hr_samples=${summary.heartRateSamples.size}")
            Log.d(TAG, "DB_WRITE_START: Persisting daily summary and decomposed entities")
            
            try {
                // Store the daily summary
                val summaryEntity = WatchDailySummaryEntity(
                    date = summary.date,
                    steps = summary.steps,
                    sleepMinutes = summary.sleepMinutes,
                    avgHeartRate = summary.avgHeartRate,
                    minHeartRate = summary.minHeartRate,
                    maxHeartRate = summary.maxHeartRate,
                    heartRateSampleCount = summary.heartRateSamples.size,
                    heartRateSamplesJson = json.encodeToString(summary.heartRateSamples),
                    syncTimestamp = summary.syncTimestamp
                )
                watchHealthDao.insertDailySummary(summaryEntity)
                Log.d(TAG, "DB_WRITE_SUCCESS: Daily summary entity inserted for date=${summary.date}")
                
                // Also update individual metric tables for easier querying
                val stepsEntity = WatchStepsEntity(
                    date = summary.date,
                    steps = summary.steps
                )
                watchHealthDao.insertSteps(stepsEntity)
                Log.i(TAG, "DB_WRITE_SUCCESS: Steps entity inserted: date=${summary.date}, steps=${summary.steps}")
                Log.i(TAG, "[STEPS][PHONE][PERSISTED] date=${summary.date}, value=${summary.steps}, table=watch_steps")
                
                if (summary.steps == 0) {
                    Log.w(TAG, "DB_WRITE_WARNING: Steps value is ZERO for date=${summary.date}")
                }
                
                if (summary.sleepMinutes != null) {
                    val sleepEntity = WatchSleepEntity(
                        date = summary.date,
                        sleepMinutes = summary.sleepMinutes
                    )
                    watchHealthDao.insertSleep(sleepEntity)
                    Log.i(TAG, "DB_WRITE_SUCCESS: Sleep entity inserted: date=${summary.date}, minutes=${summary.sleepMinutes}")
                    Log.i(TAG, "[SLEEP][PHONE][PERSISTED] date=${summary.date}, value=${summary.sleepMinutes}min, table=watch_sleep")
                } else {
                    Log.w(TAG, "DB_WRITE_SKIPPED: Sleep is NULL for date=${summary.date} - no sleep entity created")
                    Log.w(TAG, "[SLEEP][PHONE][PERSISTED] date=${summary.date}, value=NULL, reason=no_sleep_data")
                }
                
                // Store individual heart rate samples
                val hrEntities = summary.heartRateSamples.map { sample ->
                    WatchHeartRateEntity(
                        bpm = sample.bpm,
                        measurementTimestamp = sample.timestamp,
                        accuracy = sample.accuracy.name
                    )
                }
                if (hrEntities.isNotEmpty()) {
                    watchHealthDao.insertHeartRates(hrEntities)
                    Log.i(TAG, "DB_WRITE_SUCCESS: ${hrEntities.size} heart rate entities inserted")
                    Log.i(TAG, "[HEART_RATE][PHONE][PERSISTED] sample_count=${hrEntities.size}, table=watch_heart_rate")
                } else {
                    Log.w(TAG, "DB_WRITE_SKIPPED: No heart rate samples to insert")
                    Log.w(TAG, "[HEART_RATE][PHONE][PERSISTED] sample_count=0, reason=no_samples")
                }
                
                // Database verification - query row counts to prove persistence
                val stepsRowCount = watchHealthDao.getStepsRowCount()
                val sleepRowCount = watchHealthDao.getSleepRowCount()
                val hrRowCount = watchHealthDao.getHeartRateRowCount()
                Log.d(TAG, "[DIAGNOSTIC][DB] Persistence verified - Steps rows: $stepsRowCount, Sleep rows: $sleepRowCount, HR rows: $hrRowCount")
                
                Log.d(TAG, "Daily summary stored successfully")
                
                // === IMMEDIATE SYNC TO BACKEND ===
                try {
                    uploadToBackend(summary)
                } catch (e: Exception) {
                    Log.w(TAG, "Backend sync failed (will retry via periodic worker): ${e.message}")
                    // Data is already in local DB, will be synced by HealthSyncWorker
                }
            } catch (e: Exception) {
                Log.e(TAG, "DB_WRITE_ERROR: Failed to persist daily summary", e)
                Log.e(TAG, "DB_WRITE_ERROR: ${e.javaClass.simpleName}: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * Upload health data to backend API (immediate sync).
     */
    private suspend fun uploadToBackend(summary: HealthDailySummary) {
        val token = TokenManager.getToken()
        val userId = TokenManager.getUserIdFromToken()
        
        if (token == null || userId == null) {
            Log.w(TAG, "BACKEND_SYNC: Skipped - not authenticated (token=$token, userId=$userId)")
            return
        }
        
        Log.i(TAG, "BACKEND_SYNC: Starting upload for date=${summary.date}")
        
        val request = HealthMetricsRequest(
            userId = userId,
            date = summary.date,
            steps = if (summary.steps > 0) summary.steps else null,
            sleepMinutes = summary.sleepMinutes,
            heartRateSamples = summary.heartRateSamples.map { 
                HeartRateSampleApi(it.bpm, it.timestamp, it.accuracy.name) 
            }.takeIf { it.isNotEmpty() },
            avgHeartRate = summary.avgHeartRate,
            minHeartRate = summary.minHeartRate,
            maxHeartRate = summary.maxHeartRate,
            syncTimestamp = summary.syncTimestamp
        )
        
        val api = RetrofitClient.healthMetricsApiService
        val response = api.uploadHealthMetrics("Bearer $token", request)
        
        if (response.success) {
            Log.i(TAG, "BACKEND_SYNC: ✅ Success - ${response.metricsStored} metrics stored")
        } else {
            Log.w(TAG, "BACKEND_SYNC: ❌ Failed - ${response.message}")
            throw Exception(response.message)
        }
    }
    
    /**
     * Ingest heart rate samples batch received from the watch.
     * Used for incremental HR updates between daily summaries.
     */
    suspend fun ingestHeartRateSamples(samples: List<HeartRateSample>) {
        withContext(Dispatchers.IO) {
            if (samples.isEmpty()) return@withContext
            
            Log.d(TAG, "Ingesting ${samples.size} heart rate samples")
            
            val entities = samples.map { sample ->
                WatchHeartRateEntity(
                    bpm = sample.bpm,
                    measurementTimestamp = sample.timestamp,
                    accuracy = sample.accuracy.name
                )
            }
            watchHealthDao.insertHeartRates(entities)
            
            // Trigger sync to backend (debounced via WorkManager)
            com.samsung.android.health.sdk.sample.healthdiary.worker.HealthSyncWorker.triggerNow(context)
        }
    }
    
    /**
     * Ingest a single step update (real-time).
     */
    suspend fun ingestStepsUpdate(date: String, steps: Int) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Ingesting steps update: date=$date, steps=$steps")
            watchHealthDao.insertSteps(
                WatchStepsEntity(date = date, steps = steps)
            )
        }
    }
    
    /**
     * Ingest a sleep update.
     */
    suspend fun ingestSleepUpdate(date: String, sleepMinutes: Int) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Ingesting sleep update: date=$date, minutes=$sleepMinutes")
            watchHealthDao.insertSleep(
                WatchSleepEntity(date = date, sleepMinutes = sleepMinutes)
            )
        }
    }
    
    // ============ QUERY METHODS ============
    
    /**
     * Get today's step count from watch data.
     */
    suspend fun getTodaySteps(date: String): Int? {
        return watchHealthDao.getStepsForDate(date)?.steps
    }
    
    fun getTodayStepsFlow(date: String): Flow<WatchStepsEntity?> {
        return watchHealthDao.getStepsForDateFlow(date)
    }
    
    /**
     * Get the latest heart rate reading from watch.
     */
    suspend fun getLatestHeartRate(): WatchHeartRateEntity? {
        return watchHealthDao.getLatestHeartRate()
    }
    
    fun getLatestHeartRateFlow(): Flow<WatchHeartRateEntity?> {
        return watchHealthDao.getLatestHeartRateFlow()
    }
    
    /**
     * Get today's sleep duration from watch data.
     */
    suspend fun getTodaySleep(date: String): Int? {
        return watchHealthDao.getSleepForDate(date)?.sleepMinutes
    }
    
    fun getTodaySleepFlow(date: String): Flow<WatchSleepEntity?> {
        return watchHealthDao.getSleepForDateFlow(date)
    }
    
    /**
     * Get the daily summary for a specific date.
     */
    suspend fun getDailySummary(date: String): WatchDailySummaryEntity? {
        return watchHealthDao.getDailySummary(date)
    }
    
    fun getDailySummaryFlow(date: String): Flow<WatchDailySummaryEntity?> {
        return watchHealthDao.getDailySummaryFlow(date)
    }
    
    /**
     * Get recent daily summaries for display.
     */
    suspend fun getRecentDailySummaries(limit: Int = 7): List<WatchDailySummaryEntity> {
        return watchHealthDao.getRecentDailySummaries(limit)
    }
    
    // ============ HISTORICAL DATA FOR CHARTS ============
    
    /**
     * Get historical steps data for charting.
     */
    suspend fun getStepsHistory(days: Int = 30): List<WatchStepsEntity> {
        return withContext(Dispatchers.IO) {
            watchHealthDao.getRecentSteps(limit = days)
        }
    }
    
    /**
     * Get historical sleep data for charting.
     */
    suspend fun getSleepHistory(days: Int = 30): List<WatchSleepEntity> {
        return withContext(Dispatchers.IO) {
            watchHealthDao.getRecentSleep(limit = days)
        }
    }
    
    /**
     * Get historical heart rate data for charting.
     * Returns daily summaries which include aggregated HR stats.
     */
    suspend fun getHeartRateHistory(days: Int = 30): List<WatchDailySummaryEntity> {
        return withContext(Dispatchers.IO) {
            watchHealthDao.getRecentDailySummaries(limit = days)
        }
    }
    
    /**
     * Get all heart rate samples for a specific date (for detailed view).
     */
    suspend fun getHeartRatesForDate(date: String): List<WatchHeartRateEntity> {
        return withContext(Dispatchers.IO) {
            val summary = watchHealthDao.getDailySummary(date)
            if (summary != null && !summary.heartRateSamplesJson.isNullOrEmpty()) {
                try {
                    json.decodeFromString<List<HeartRateSample>>(summary.heartRateSamplesJson!!).map {
                        WatchHeartRateEntity(
                            bpm = it.bpm,
                            measurementTimestamp = it.timestamp,
                            accuracy = it.accuracy.name
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse HR samples from JSON", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
    
    // ============ SYNC TO BACKEND ============
    
    /**
     * Get data that hasn't been synced to backend yet.
     */
    suspend fun getUnsyncedData(): UnsyncedWatchData {
        return UnsyncedWatchData(
            steps = watchHealthDao.getUnsyncedSteps(),
            heartRates = watchHealthDao.getUnsyncedHeartRates(),
            sleep = watchHealthDao.getUnsyncedSleep(),
            dailySummaries = watchHealthDao.getUnsyncedDailySummaries()
        )
    }
    
    /**
     * Mark data as synced to backend.
     */
    suspend fun markAsSynced(
        stepIds: List<Long> = emptyList(),
        heartRateIds: List<Long> = emptyList(),
        sleepIds: List<Long> = emptyList(),
        summaryDates: List<String> = emptyList()
    ) {
        withContext(Dispatchers.IO) {
            if (stepIds.isNotEmpty()) {
                watchHealthDao.markStepsSynced(stepIds)
            }
            if (heartRateIds.isNotEmpty()) {
                watchHealthDao.markHeartRatesSynced(heartRateIds)
            }
            if (sleepIds.isNotEmpty()) {
                watchHealthDao.markSleepSynced(sleepIds)
            }
            if (summaryDates.isNotEmpty()) {
                watchHealthDao.markDailySummariesSynced(summaryDates)
            }
        }
    }
    
    // ============ CLEANUP ============
    
    /**
     * Delete old heart rate samples to save storage.
     * Keeps daily summaries intact for historical reference.
     */
    suspend fun cleanupOldHeartRates(olderThanDays: Int = 7) {
        withContext(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            watchHealthDao.deleteOldHeartRates(cutoff)
            Log.d(TAG, "Cleaned up heart rate samples older than $olderThanDays days")
        }
    }
    
    // ============ DIAGNOSTICS ============
    
    /**
     * Get database diagnostics for health data tables.
     */
    suspend fun getDatabaseDiagnostics(): HealthDatabaseDiagnostics {
        return withContext(Dispatchers.IO) {
            HealthDatabaseDiagnostics(
                stepsRowCount = watchHealthDao.getStepsRowCount(),
                sleepRowCount = watchHealthDao.getSleepRowCount(),
                heartRateRowCount = watchHealthDao.getHeartRateRowCount(),
                latestStepsDate = watchHealthDao.getLatestStepsDate(),
                latestSleepDate = watchHealthDao.getLatestSleepDate(),
                latestHeartRateTimestamp = watchHealthDao.getLatestHeartRateTimestamp()
            )
        }
    }
    
    /**
     * Get latest data for today for periodic backend sync.
     * Returns a simplified summary that can be uploaded to backend.
     */
    suspend fun getLatestDataForSync(date: String): SyncableHealthData? {
        return withContext(Dispatchers.IO) {
            val steps = watchHealthDao.getStepsForDate(date)
            val sleep = watchHealthDao.getSleepForDate(date)
            val dailySummary = watchHealthDao.getDailySummary(date)
            
            if (steps == null && sleep == null && dailySummary == null) {
                return@withContext null
            }
            
            SyncableHealthData(
                date = date,
                steps = steps?.steps,
                sleepMinutes = sleep?.sleepMinutes,
                avgHeartRate = dailySummary?.avgHeartRate,
                minHeartRate = dailySummary?.minHeartRate,
                maxHeartRate = dailySummary?.maxHeartRate
            )
        }
    }
}

/**
 * Simplified health data ready for backend sync.
 */
data class SyncableHealthData(
    val date: String,
    val steps: Int?,
    val sleepMinutes: Int?,
    val avgHeartRate: Int?,
    val minHeartRate: Int?,
    val maxHeartRate: Int?
) {
    val hasData: Boolean
        get() = steps != null || sleepMinutes != null || avgHeartRate != null
}

/**
 * Container for unsynced watch data.
 */
data class UnsyncedWatchData(
    val steps: List<WatchStepsEntity>,
    val heartRates: List<WatchHeartRateEntity>,
    val sleep: List<WatchSleepEntity>,
    val dailySummaries: List<WatchDailySummaryEntity>
) {
    val isEmpty: Boolean
        get() = steps.isEmpty() && heartRates.isEmpty() && sleep.isEmpty() && dailySummaries.isEmpty()
    
    val totalCount: Int
        get() = steps.size + heartRates.size + sleep.size + dailySummaries.size
}

/**
 * Database diagnostics for health data tables.
 */
data class HealthDatabaseDiagnostics(
    val stepsRowCount: Int,
    val sleepRowCount: Int,
    val heartRateRowCount: Int,
    val latestStepsDate: String?,
    val latestSleepDate: String?,
    val latestHeartRateTimestamp: Long?
)
