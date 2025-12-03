package com.samsung.android.health.sdk.sample.healthdiary.data.repository

import android.content.Context
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.EntityType
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UploadLog
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UploadStatus
import com.samsung.android.health.sdk.sample.healthdiary.data.mapper.EntityMapper.toDomain
import com.samsung.android.health.sdk.sample.healthdiary.data.mapper.EntityMapper.toEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.UploadLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for UploadLog operations
 * Tracks all sync operations from phone to backend
 */
class UploadLogRepository(context: Context) {
    
    private val uploadLogDao: UploadLogDao = 
        AppDatabase.getDatabase(context).uploadLogDao()
    
    /**
     * Log a new upload attempt
     * @return ID of the inserted log entry
     */
    suspend fun logUpload(log: UploadLog): Long = withContext(Dispatchers.IO) {
        uploadLogDao.insert(log.toEntity())
    }
    
    /**
     * Log multiple uploads
     */
    suspend fun logMultipleUploads(logs: List<UploadLog>): List<Long> = 
        withContext(Dispatchers.IO) {
            uploadLogDao.insertAll(logs.map { it.toEntity() })
        }
    
    /**
     * Get all upload logs
     */
    suspend fun getAllLogs(): List<UploadLog> = withContext(Dispatchers.IO) {
        uploadLogDao.getAll().map { it.toDomain() }
    }
    
    /**
     * Get recent logs as Flow
     */
    fun getRecentLogsFlow(limit: Int = 100): Flow<List<UploadLog>> {
        return uploadLogDao.getRecentLogsFlow(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get logs by status
     */
    suspend fun getLogsByStatus(status: UploadStatus): List<UploadLog> = 
        withContext(Dispatchers.IO) {
            uploadLogDao.getByStatus(status.value).map { it.toDomain() }
        }
    
    /**
     * Get logs by entity type
     */
    suspend fun getLogsByEntityType(entityType: EntityType): List<UploadLog> =
        withContext(Dispatchers.IO) {
            uploadLogDao.getByEntityType(entityType.value).map { it.toDomain() }
        }
    
    /**
     * Get logs for a specific entity
     */
    suspend fun getLogsForEntity(entityType: EntityType, entityId: Long): List<UploadLog> =
        withContext(Dispatchers.IO) {
            uploadLogDao.getByEntity(entityType.value, entityId).map { it.toDomain() }
        }
    
    /**
     * Get failed uploads
     */
    suspend fun getFailedUploads(): List<UploadLog> = withContext(Dispatchers.IO) {
        uploadLogDao.getFailedUploads().map { it.toDomain() }
    }
    
    /**
     * Get successful uploads
     */
    suspend fun getSuccessfulUploads(): List<UploadLog> = withContext(Dispatchers.IO) {
        uploadLogDao.getSuccessfulUploads().map { it.toDomain() }
    }
    
    /**
     * Get upload success rate as percentage
     */
    suspend fun getSuccessRate(): Float = withContext(Dispatchers.IO) {
        uploadLogDao.getSuccessRate() ?: 0.0f
    }
    
    /**
     * Get logs within time range
     */
    suspend fun getLogsByTimeRange(startTime: Long, endTime: Long): List<UploadLog> =
        withContext(Dispatchers.IO) {
            uploadLogDao.getByTimeRange(startTime, endTime).map { it.toDomain() }
        }
    
    /**
     * Get recent logs with limit
     */
    suspend fun getRecentLogs(limit: Int = 50): List<UploadLog> = 
        withContext(Dispatchers.IO) {
            uploadLogDao.getRecentLogs(limit).map { it.toDomain() }
        }
    
    /**
     * Delete old logs to save space
     * @param daysOld Delete logs older than this many days
     * @return Number of logs deleted
     */
    suspend fun deleteOldLogs(daysOld: Int = 90): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        uploadLogDao.deleteOlderThan(cutoffTime)
    }
    
    /**
     * Get total count of logs
     */
    suspend fun getLogCount(): Int = withContext(Dispatchers.IO) {
        uploadLogDao.getCount()
    }
    
    /**
     * Get count by status
     */
    suspend fun getCountByStatus(status: UploadStatus): Int = withContext(Dispatchers.IO) {
        uploadLogDao.getCountByStatus(status.value)
    }
}
