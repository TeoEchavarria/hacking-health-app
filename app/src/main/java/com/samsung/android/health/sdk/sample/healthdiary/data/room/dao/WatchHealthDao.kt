package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.*
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for watch health data.
 * This DAO provides access to the PRIMARY source of truth for health metrics
 * received directly from the watch.
 */
@Dao
interface WatchHealthDao {
    
    // ============ STEPS ============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: WatchStepsEntity)
    
    @Query("SELECT * FROM watch_steps WHERE date = :date LIMIT 1")
    suspend fun getStepsForDate(date: String): WatchStepsEntity?
    
    @Query("SELECT * FROM watch_steps WHERE date = :date LIMIT 1")
    fun getStepsForDateFlow(date: String): Flow<WatchStepsEntity?>
    
    @Query("SELECT * FROM watch_steps ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSteps(limit: Int = 7): List<WatchStepsEntity>
    
    @Query("SELECT SUM(steps) FROM watch_steps WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalStepsInRange(startDate: String, endDate: String): Int?
    
    @Query("SELECT * FROM watch_steps WHERE syncedAt IS NULL")
    suspend fun getUnsyncedSteps(): List<WatchStepsEntity>
    
    @Query("UPDATE watch_steps SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markStepsSynced(ids: List<Long>, syncedAt: Long = System.currentTimeMillis())
    
    // ============ HEART RATE ============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(heartRate: WatchHeartRateEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRates(heartRates: List<WatchHeartRateEntity>)
    
    @Query("SELECT * FROM watch_heart_rate ORDER BY measurementTimestamp DESC LIMIT 1")
    suspend fun getLatestHeartRate(): WatchHeartRateEntity?
    
    @Query("SELECT * FROM watch_heart_rate ORDER BY measurementTimestamp DESC LIMIT 1")
    fun getLatestHeartRateFlow(): Flow<WatchHeartRateEntity?>
    
    @Query("""
        SELECT * FROM watch_heart_rate 
        WHERE measurementTimestamp BETWEEN :startTimestamp AND :endTimestamp
        ORDER BY measurementTimestamp ASC
    """)
    suspend fun getHeartRatesInRange(startTimestamp: Long, endTimestamp: Long): List<WatchHeartRateEntity>
    
    @Query("SELECT AVG(bpm) FROM watch_heart_rate WHERE measurementTimestamp BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun getAverageHeartRate(startTimestamp: Long, endTimestamp: Long): Double?
    
    @Query("SELECT * FROM watch_heart_rate WHERE syncedAt IS NULL ORDER BY measurementTimestamp ASC LIMIT :limit")
    suspend fun getUnsyncedHeartRates(limit: Int = 100): List<WatchHeartRateEntity>
    
    @Query("UPDATE watch_heart_rate SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markHeartRatesSynced(ids: List<Long>, syncedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM watch_heart_rate WHERE measurementTimestamp < :before")
    suspend fun deleteOldHeartRates(before: Long)
    
    // ============ SLEEP ============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleep(sleep: WatchSleepEntity)
    
    @Query("SELECT * FROM watch_sleep WHERE date = :date LIMIT 1")
    suspend fun getSleepForDate(date: String): WatchSleepEntity?
    
    @Query("SELECT * FROM watch_sleep WHERE date = :date LIMIT 1")
    fun getSleepForDateFlow(date: String): Flow<WatchSleepEntity?>
    
    @Query("SELECT * FROM watch_sleep ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSleep(limit: Int = 7): List<WatchSleepEntity>
    
    @Query("SELECT AVG(sleepMinutes) FROM watch_sleep WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageSleepInRange(startDate: String, endDate: String): Double?
    
    @Query("SELECT * FROM watch_sleep WHERE syncedAt IS NULL")
    suspend fun getUnsyncedSleep(): List<WatchSleepEntity>
    
    @Query("UPDATE watch_sleep SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSleepSynced(ids: List<Long>, syncedAt: Long = System.currentTimeMillis())
    
    // ============ DAILY SUMMARY ============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: WatchDailySummaryEntity)
    
    @Query("SELECT * FROM watch_daily_summary WHERE date = :date LIMIT 1")
    suspend fun getDailySummary(date: String): WatchDailySummaryEntity?
    
    @Query("SELECT * FROM watch_daily_summary WHERE date = :date LIMIT 1")
    fun getDailySummaryFlow(date: String): Flow<WatchDailySummaryEntity?>
    
    @Query("SELECT * FROM watch_daily_summary ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDailySummaries(limit: Int = 7): List<WatchDailySummaryEntity>
    
    @Query("SELECT * FROM watch_daily_summary WHERE syncedToBackendAt IS NULL")
    suspend fun getUnsyncedDailySummaries(): List<WatchDailySummaryEntity>
    
    @Query("UPDATE watch_daily_summary SET syncedToBackendAt = :syncedAt WHERE date IN (:dates)")
    suspend fun markDailySummariesSynced(dates: List<String>, syncedAt: Long = System.currentTimeMillis())
    
    // ============ DIAGNOSTICS ============
    
    @Query("SELECT COUNT(*) FROM watch_steps")
    suspend fun getStepsRowCount(): Int
    
    @Query("SELECT COUNT(*) FROM watch_sleep")
    suspend fun getSleepRowCount(): Int
    
    @Query("SELECT COUNT(*) FROM watch_heart_rate")
    suspend fun getHeartRateRowCount(): Int
    
    @Query("SELECT date FROM watch_steps ORDER BY date DESC LIMIT 1")
    suspend fun getLatestStepsDate(): String?
    
    @Query("SELECT date FROM watch_sleep ORDER BY date DESC LIMIT 1")
    suspend fun getLatestSleepDate(): String?
    
    @Query("SELECT measurementTimestamp FROM watch_heart_rate ORDER BY measurementTimestamp DESC LIMIT 1")
    suspend fun getLatestHeartRateTimestamp(): Long?
}
