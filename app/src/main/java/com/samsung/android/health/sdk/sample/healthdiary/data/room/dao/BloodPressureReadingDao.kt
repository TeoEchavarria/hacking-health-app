package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.BloodPressureReadingEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for blood pressure readings.
 *
 * Supports the offline-first sync pattern with state machine queries.
 */
@Dao
interface BloodPressureReadingDao {
    
    // =========================================
    // Insert Operations
    // =========================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BloodPressureReadingEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<BloodPressureReadingEntity>)
    
    // =========================================
    // Sync State Queries
    // =========================================
    
    /**
     * Get all readings that need to be synced (PENDING or FAILED).
     * Ordered by creation time to preserve chronological order.
     */
    @Query("""
        SELECT * FROM blood_pressure_readings 
        WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED' 
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingReadings(): List<BloodPressureReadingEntity>
    
    /**
     * Get count of pending readings for UI indicator.
     */
    @Query("""
        SELECT COUNT(*) FROM blood_pressure_readings 
        WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'
    """)
    fun getPendingSyncCount(): Flow<Int>
    
    /**
     * Update sync status for a specific reading.
     */
    @Query("""
        UPDATE blood_pressure_readings 
        SET syncStatus = :status, lastSyncAttempt = :timestamp, syncError = :error
        WHERE idempotencyKey = :key
    """)
    suspend fun updateSyncStatus(
        key: String,
        status: SyncStatus,
        timestamp: Long,
        error: String? = null
    )
    
    /**
     * Update sync status with server response data (successful sync).
     */
    @Query("""
        UPDATE blood_pressure_readings 
        SET syncStatus = :status, 
            lastSyncAttempt = :timestamp, 
            serverStage = :stage,
            serverSeverity = :severity,
            serverAlertGenerated = :alertGenerated,
            syncError = NULL
        WHERE idempotencyKey = :key
    """)
    suspend fun updateSyncSuccess(
        key: String,
        status: SyncStatus,
        timestamp: Long,
        stage: String,
        severity: String,
        alertGenerated: Boolean
    )
    
    // =========================================
    // Read Operations
    // =========================================
    
    /**
     * Get reading by idempotency key.
     */
    @Query("SELECT * FROM blood_pressure_readings WHERE idempotencyKey = :key")
    suspend fun getByKey(key: String): BloodPressureReadingEntity?
    
    /**
     * Get all readings for a user, ordered by timestamp descending.
     */
    @Query("""
        SELECT * FROM blood_pressure_readings 
        WHERE userId = :userId 
        ORDER BY timestamp DESC
    """)
    fun getReadingsForUser(userId: String): Flow<List<BloodPressureReadingEntity>>
    
    /**
     * Get recent readings (last N days) for a user.
     */
    @Query("""
        SELECT * FROM blood_pressure_readings 
        WHERE userId = :userId AND createdAt >= :sinceTimestamp
        ORDER BY timestamp DESC
    """)
    suspend fun getRecentReadings(userId: String, sinceTimestamp: Long): List<BloodPressureReadingEntity>
    
    /**
     * Get crisis readings for user (for history/review).
     */
    @Query("""
        SELECT * FROM blood_pressure_readings 
        WHERE userId = :userId AND crisisFlag = 1
        ORDER BY timestamp DESC
    """)
    fun getCrisisReadings(userId: String): Flow<List<BloodPressureReadingEntity>>
    
    // =========================================
    // Delete Operations
    // =========================================
    
    /**
     * Delete old synced readings (cleanup after N days).
     */
    @Query("""
        DELETE FROM blood_pressure_readings 
        WHERE syncStatus = 'SYNCED' AND createdAt < :beforeTimestamp
    """)
    suspend fun deleteSyncedOlderThan(beforeTimestamp: Long): Int
    
    /**
     * Delete all readings for a user (for account logout/delete).
     */
    @Query("DELETE FROM blood_pressure_readings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
