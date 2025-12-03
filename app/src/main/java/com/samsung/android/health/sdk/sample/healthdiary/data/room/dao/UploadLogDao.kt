package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.UploadLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for UploadLog operations
 * Tracks all sync operations from phone to backend
 */
@Dao
interface UploadLogDao {
    
    /**
     * Insert a new upload log entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UploadLogEntity): Long
    
    /**
     * Insert multiple upload log entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<UploadLogEntity>): List<Long>
    
    /**
     * Get all upload logs ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM upload_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<UploadLogEntity>
    
    /**
     * Get all upload logs as a Flow
     */
    @Query("SELECT * FROM upload_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int = 100): Flow<List<UploadLogEntity>>
    
    /**
     * Get upload logs by status
     */
    @Query("SELECT * FROM upload_logs WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getByStatus(status: String): List<UploadLogEntity>
    
    /**
     * Get upload logs by entity type
     */
    @Query("SELECT * FROM upload_logs WHERE entityType = :entityType ORDER BY timestamp DESC")
    suspend fun getByEntityType(entityType: String): List<UploadLogEntity>
    
    /**
     * Get upload logs for a specific entity
     */
    @Query("SELECT * FROM upload_logs WHERE entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    suspend fun getByEntity(entityType: String, entityId: Long): List<UploadLogEntity>
    
    /**
     * Get failed upload logs
     */
    @Query("SELECT * FROM upload_logs WHERE status = 'failed' ORDER BY timestamp DESC")
    suspend fun getFailedUploads(): List<UploadLogEntity>
    
    /**
     * Get successful upload logs
     */
    @Query("SELECT * FROM upload_logs WHERE status = 'success' ORDER BY timestamp DESC")
    suspend fun getSuccessfulUploads(): List<UploadLogEntity>
    
    /**
     * Get upload logs within a time range
     */
    @Query("SELECT * FROM upload_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<UploadLogEntity>
    
    /**
     * Get upload log by ID
     */
    @Query("SELECT * FROM upload_logs WHERE id = :id")
    suspend fun getById(id: Long): UploadLogEntity?
    
    /**
     * Get count of all logs
     */
    @Query("SELECT COUNT(*) FROM upload_logs")
    suspend fun getCount(): Int
    
    /**
     * Get count by status
     */
    @Query("SELECT COUNT(*) FROM upload_logs WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int
    
    /**
     * Get success rate (percentage of successful uploads)
     */
    @Query("""
        SELECT CAST(SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) * 100 
        FROM upload_logs
    """)
    suspend fun getSuccessRate(): Float?
    
    /**
     * Delete logs older than specified timestamp
     */
    @Query("DELETE FROM upload_logs WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    /**
     * Delete all logs (use with caution)
     */
    @Query("DELETE FROM upload_logs")
    suspend fun deleteAll()
    
    /**
     * Get recent logs with limit
     */
    @Query("SELECT * FROM upload_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<UploadLogEntity>
}
