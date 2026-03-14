package com.samsung.android.health.sdk.sample.healthdiary.data.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.WatchHealthDao
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.*
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
            
            // Also update individual metric tables for easier querying
            watchHealthDao.insertSteps(
                WatchStepsEntity(
                    date = summary.date,
                    steps = summary.steps
                )
            )
            
            if (summary.sleepMinutes != null) {
                watchHealthDao.insertSleep(
                    WatchSleepEntity(
                        date = summary.date,
                        sleepMinutes = summary.sleepMinutes
                    )
                )
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
            }
            
            Log.d(TAG, "Daily summary stored successfully")
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
