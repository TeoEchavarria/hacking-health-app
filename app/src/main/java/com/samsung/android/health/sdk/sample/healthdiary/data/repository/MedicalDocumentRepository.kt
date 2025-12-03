package com.samsung.android.health.sdk.sample.healthdiary.data.repository

import android.content.Context
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.MedicalDocument
import com.samsung.android.health.sdk.sample.healthdiary.data.mapper.EntityMapper.toDomain
import com.samsung.android.health.sdk.sample.healthdiary.data.mapper.EntityMapper.toEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.MedicalDocumentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for MedicalDocument operations
 * Manages PDF medical history files metadata
 */
class MedicalDocumentRepository(context: Context) {
    
    private val medicalDocumentDao: MedicalDocumentDao = 
        AppDatabase.getDatabase(context).medicalDocumentDao()
    
    /**
     * Save a new medical document
     * @return ID of the inserted document
     */
    suspend fun saveDocument(document: MedicalDocument): Long = withContext(Dispatchers.IO) {
        medicalDocumentDao.insert(document.toEntity())
    }
    
    /**
     * Update an existing document
     */
    suspend fun updateDocument(document: MedicalDocument) = withContext(Dispatchers.IO) {
        medicalDocumentDao.update(document.toEntity())
    }
    
    /**
     * Get all medical documents
     */
    suspend fun getAllDocuments(): List<MedicalDocument> = withContext(Dispatchers.IO) {
        medicalDocumentDao.getAll().map { it.toDomain() }
    }
    
    /**
     * Get all documents as Flow for reactive updates
     */
    fun getAllDocumentsFlow(): Flow<List<MedicalDocument>> {
        return medicalDocumentDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get document by ID
     */
    suspend fun getDocumentById(id: Long): MedicalDocument? = withContext(Dispatchers.IO) {
        medicalDocumentDao.getById(id)?.toDomain()
    }
    
    /**
     * Get documents by processed status
     */
    suspend fun getDocumentsByStatus(processed: Boolean): List<MedicalDocument> =
        withContext(Dispatchers.IO) {
            medicalDocumentDao.getByProcessedStatus(processed).map { it.toDomain() }
        }
    
    /**
     * Get unprocessed documents
     */
    suspend fun getUnprocessedDocuments(): List<MedicalDocument> = withContext(Dispatchers.IO) {
        medicalDocumentDao.getUnprocessed().map { it.toDomain() }
    }
    
    /**
     * Search documents by filename
     */
    suspend fun searchByFilename(filename: String): List<MedicalDocument> =
        withContext(Dispatchers.IO) {
            medicalDocumentDao.searchByFilename(filename).map { it.toDomain() }
        }
    
    /**
     * Search documents by description
     */
    suspend fun searchByDescription(searchTerm: String): List<MedicalDocument> =
        withContext(Dispatchers.IO) {
            medicalDocumentDao.searchByDescription(searchTerm).map { it.toDomain() }
        }
    
    /**
     * Get documents by tag
     */
    suspend fun getDocumentsByTag(tag: String): List<MedicalDocument> =
        withContext(Dispatchers.IO) {
            medicalDocumentDao.getByTag(tag).map { it.toDomain() }
        }
    
    /**
     * Get documents within time range
     */
    suspend fun getDocumentsByTimeRange(startTime: Long, endTime: Long): List<MedicalDocument> =
        withContext(Dispatchers.IO) {
            medicalDocumentDao.getByTimeRange(startTime, endTime).map { it.toDomain() }
        }
    
    /**
     * Mark document as processed
     */
    suspend fun markAsProcessed(documentId: Long) = withContext(Dispatchers.IO) {
        medicalDocumentDao.markAsProcessed(documentId, System.currentTimeMillis())
    }
    
    /**
     * Update tags for a document
     */
    suspend fun updateTags(documentId: Long, tags: List<String>) = withContext(Dispatchers.IO) {
        medicalDocumentDao.updateTags(documentId, tags.joinToString(","))
    }
    
    /**
     * Delete document by ID
     */
    suspend fun deleteDocument(id: Long) = withContext(Dispatchers.IO) {
        medicalDocumentDao.deleteById(id)
    }
    
    /**
     * Delete old documents
     * @param daysOld Delete documents older than this many days
     * @return Number of documents deleted
     */
    suspend fun deleteOldDocuments(daysOld: Int = 365): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        medicalDocumentDao.deleteOlderThan(cutoffTime)
    }
    
    /**
     * Get total count of documents
     */
    suspend fun getDocumentCount(): Int = withContext(Dispatchers.IO) {
        medicalDocumentDao.getCount()
    }
    
    /**
     * Get count of unprocessed documents
     */
    suspend fun getUnprocessedCount(): Int = withContext(Dispatchers.IO) {
        medicalDocumentDao.getUnprocessedCount()
    }
    
    /**
     * Get total storage used by all documents in bytes
     */
    suspend fun getTotalStorageUsed(): Long = withContext(Dispatchers.IO) {
        medicalDocumentDao.getTotalStorageUsed() ?: 0L
    }
}
