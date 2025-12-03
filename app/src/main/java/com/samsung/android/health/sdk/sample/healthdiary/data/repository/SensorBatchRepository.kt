package com.samsung.android.health.sdk.sample.healthdiary.data.repository

import android.content.Context
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.SensorBatch
import com.samsung.android.health.sdk.sample.healthdiary.data.mapper.EntityMapper.toDomain
import com.samsung.android.health.sdk.sample.healthdiary.data.mapper.EntityMapper.toEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.SensorBatchDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for SensorBatch operations
 * Provides clean API for business logic layer
 */
class SensorBatchRepository(context: Context) {
    
    private val sensorBatchDao: SensorBatchDao = 
        AppDatabase.getDatabase(context).sensorBatchDao()
    
    /**
     * Save a sensor batch to the database
     * @return ID of the inserted batch
     */
    suspend fun saveBatch(batch: SensorBatch): Long = withContext(Dispatchers.IO) {
        sensorBatchDao.insert(batch.toEntity())
    }
    
    /**
     * Save multiple batches
     * @return List of IDs of inserted batches
     */
    suspend fun saveBatches(batches: List<SensorBatch>): List<Long> = withContext(Dispatchers.IO) {
        sensorBatchDao.insertAll(batches.map { it.toEntity() })
    }
    
    /**
     * Get all sensor batches
     */
    suspend fun getAllBatches(): List<SensorBatch> = withContext(Dispatchers.IO) {
        sensorBatchDao.getAll().map { it.toDomain() }
    }
    
    /**
     * Get all sensor batches as Flow for reactive updates
     */
    fun getAllBatchesFlow(): Flow<List<SensorBatch>> {
        return sensorBatchDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get batches that haven't been uploaded yet
     */
    suspend fun getPendingUploads(): List<SensorBatch> = withContext(Dispatchers.IO) {
        sensorBatchDao.getPendingUploads().map { it.toDomain() }
    }
    
    /**
     * Get batches by sensor type
     */
    suspend fun getBatchesBySensorType(sensorType: String): List<SensorBatch> = 
        withContext(Dispatchers.IO) {
            sensorBatchDao.getBySensorType(sensorType).map { it.toDomain() }
        }
    
    /**
     * Get batches within a time range
     */
    suspend fun getBatchesByTimeRange(startTime: Long, endTime: Long): List<SensorBatch> =
        withContext(Dispatchers.IO) {
            sensorBatchDao.getByTimeRange(startTime, endTime).map { it.toDomain() }
        }
    
    /**
     * Mark a batch as uploaded
     */
    suspend fun markAsUploaded(batchId: Long) = withContext(Dispatchers.IO) {
        sensorBatchDao.markAsUploaded(batchId, System.currentTimeMillis())
    }
    
    /**
     * Mark multiple batches as uploaded
     */
    suspend fun markMultipleAsUploaded(batchIds: List<Long>) = withContext(Dispatchers.IO) {
        sensorBatchDao.markMultipleAsUploaded(batchIds, System.currentTimeMillis())
    }
    
    /**
     * Get batch by ID
     */
    suspend fun getBatchById(id: Long): SensorBatch? = withContext(Dispatchers.IO) {
        sensorBatchDao.getById(id)?.toDomain()
    }
    
    /**
     * Delete old batches (cleanup)
     * @param daysOld Delete batches older than this many days
     * @return Number of batches deleted
     */
    suspend fun deleteOldBatches(daysOld: Int = 30): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        sensorBatchDao.deleteOlderThan(cutoffTime)
    }
    
    /**
     * Delete old uploaded batches
     * @param daysOld Delete uploaded batches older than this many days
     * @return Number of batches deleted
     */
    suspend fun deleteOldUploadedBatches(daysOld: Int = 7): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        sensorBatchDao.deleteUploadedOlderThan(cutoffTime)
    }
    
    /**
     * Get total count of batches
     */
    suspend fun getBatchCount(): Int = withContext(Dispatchers.IO) {
        sensorBatchDao.getCount()
    }
    
    /**
     * Get count of pending uploads
     */
    suspend fun getPendingUploadCount(): Int = withContext(Dispatchers.IO) {
        sensorBatchDao.getPendingUploadCount()
    }
}
