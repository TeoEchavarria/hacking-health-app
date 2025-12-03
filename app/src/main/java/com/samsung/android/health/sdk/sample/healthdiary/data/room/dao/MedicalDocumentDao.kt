package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.MedicalDocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for MedicalDocument operations
 * Manages metadata for uploaded PDF medical files
 */
@Dao
interface MedicalDocumentDao {
    
    /**
     * Insert a new medical document
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: MedicalDocumentEntity): Long
    
    /**
     * Insert multiple medical documents
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<MedicalDocumentEntity>): List<Long>
    
    /**
     * Update an existing document
     */
    @Update
    suspend fun update(document: MedicalDocumentEntity)
    
    /**
     * Get all medical documents ordered by upload timestamp (newest first)
     */
    @Query("SELECT * FROM medical_documents ORDER BY uploadTimestamp DESC")
    suspend fun getAll(): List<MedicalDocumentEntity>
    
    /**
     * Get all medical documents as a Flow
     */
    @Query("SELECT * FROM medical_documents ORDER BY uploadTimestamp DESC")
    fun getAllFlow(): Flow<List<MedicalDocumentEntity>>
    
    /**
     * Get document by ID
     */
    @Query("SELECT * FROM medical_documents WHERE id = :id")
    suspend fun getById(id: Long): MedicalDocumentEntity?
    
    /**
     * Get documents by processed status
     */
    @Query("SELECT * FROM medical_documents WHERE processed = :processed ORDER BY uploadTimestamp DESC")
    suspend fun getByProcessedStatus(processed: Boolean): List<MedicalDocumentEntity>
    
    /**
     * Get unprocessed documents
     */
    @Query("SELECT * FROM medical_documents WHERE processed = 0 ORDER BY uploadTimestamp ASC")
    suspend fun getUnprocessed(): List<MedicalDocumentEntity>
    
    /**
     * Get documents by filename (partial match)
     */
    @Query("SELECT * FROM medical_documents WHERE filename LIKE '%' || :filename || '%' ORDER BY uploadTimestamp DESC")
    suspend fun searchByFilename(filename: String): List<MedicalDocumentEntity>
    
    /**
     * Get documents by description (partial match)
     */
    @Query("SELECT * FROM medical_documents WHERE description LIKE '%' || :searchTerm || '%' ORDER BY uploadTimestamp DESC")
    suspend fun searchByDescription(searchTerm: String): List<MedicalDocumentEntity>
    
    /**
     * Get documents by tags (contains tag)
     */
    @Query("SELECT * FROM medical_documents WHERE tags LIKE '%' || :tag || '%' ORDER BY uploadTimestamp DESC")
    suspend fun getByTag(tag: String): List<MedicalDocumentEntity>
    
    /**
     * Get documents within a time range
     */
    @Query("SELECT * FROM medical_documents WHERE uploadTimestamp BETWEEN :startTime AND :endTime ORDER BY uploadTimestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<MedicalDocumentEntity>
    
    /**
     * Mark document as processed
     */
    @Query("UPDATE medical_documents SET processed = 1, processedAt = :processedAt WHERE id = :documentId")
    suspend fun markAsProcessed(documentId: Long, processedAt: Long)
    
    /**
     * Update tags for a document
     */
    @Query("UPDATE medical_documents SET tags = :tags WHERE id = :documentId")
    suspend fun updateTags(documentId: Long, tags: String)
    
    /**
     * Get count of all documents
     */
    @Query("SELECT COUNT(*) FROM medical_documents")
    suspend fun getCount(): Int
    
    /**
     * Get count of unprocessed documents
     */
    @Query("SELECT COUNT(*) FROM medical_documents WHERE processed = 0")
    suspend fun getUnprocessedCount(): Int
    
    /**
     * Get total size of all documents in bytes
     */
    @Query("SELECT SUM(fileSize) FROM medical_documents")
    suspend fun getTotalStorageUsed(): Long?
    
    /**
     * Delete document by ID
     */
    @Query("DELETE FROM medical_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Delete documents older than specified timestamp
     */
    @Query("DELETE FROM medical_documents WHERE uploadTimestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    /**
     * Delete all documents (use with caution)
     */
    @Query("DELETE FROM medical_documents")
    suspend fun deleteAll()
}
