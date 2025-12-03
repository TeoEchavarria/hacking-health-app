package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.SensorBatchEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SensorBatch operations
 * All methods are coroutine-friendly (suspend functions)
 */
@Dao
interface SensorBatchDao {
    
    /**
     * Insert a single sensor batch
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: SensorBatchEntity): Long
    
    /**
     * Insert multiple sensor batches
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(batches: List<SensorBatchEntity>): List<Long>
    
    /**
     * Update an existing sensor batch
     */
    @Update
    suspend fun update(batch: SensorBatchEntity)
    
    /**
     * Get all sensor batches ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM sensor_batches ORDER BY timestamp DESC")
    suspend fun getAll(): List<SensorBatchEntity>
    
    /**
     * Get all sensor batches as a Flow for reactive updates
     */
    @Query("SELECT * FROM sensor_batches ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SensorBatchEntity>>
    
    /**
     * Get sensor batches that haven't been uploaded yet
     */
    @Query("SELECT * FROM sensor_batches WHERE uploaded = 0 ORDER BY timestamp ASC")
    suspend fun getPendingUploads(): List<SensorBatchEntity>
    
    /**
     * Get sensor batches by sensor type
     */
    @Query("SELECT * FROM sensor_batches WHERE sensorType = :sensorType ORDER BY timestamp DESC")
    suspend fun getBySensorType(sensorType: String): List<SensorBatchEntity>
    
    /**
     * Get sensor batches within a time range
     */
    @Query("SELECT * FROM sensor_batches WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<SensorBatchEntity>
    
    /**
     * Mark a batch as uploaded
     */
    @Query("UPDATE sensor_batches SET uploaded = 1, uploadedAt = :uploadedAt WHERE id = :batchId")
    suspend fun markAsUploaded(batchId: Long, uploadedAt: Long)
    
    /**
     * Mark multiple batches as uploaded
     */
    @Query("UPDATE sensor_batches SET uploaded = 1, uploadedAt = :uploadedAt WHERE id IN (:batchIds)")
    suspend fun markMultipleAsUploaded(batchIds: List<Long>, uploadedAt: Long)
    
    /**
     * Get batch by ID
     */
    @Query("SELECT * FROM sensor_batches WHERE id = :id")
    suspend fun getById(id: Long): SensorBatchEntity?
    
    /**
     * Delete batches older than specified timestamp
     */
    @Query("DELETE FROM sensor_batches WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    /**
     * Delete uploaded batches older than specified timestamp
     */
    @Query("DELETE FROM sensor_batches WHERE uploaded = 1 AND uploadedAt < :timestamp")
    suspend fun deleteUploadedOlderThan(timestamp: Long): Int
    
    /**
     * Get count of all batches
     */
    @Query("SELECT COUNT(*) FROM sensor_batches")
    suspend fun getCount(): Int
    
    /**
     * Get count of pending uploads
     */
    @Query("SELECT COUNT(*) FROM sensor_batches WHERE uploaded = 0")
    suspend fun getPendingUploadCount(): Int
    
    /**
     * Delete batch by ID
     */
    @Query("DELETE FROM sensor_batches WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Delete all batches (use with caution)
     */
    @Query("DELETE FROM sensor_batches")
    suspend fun deleteAll()
}
