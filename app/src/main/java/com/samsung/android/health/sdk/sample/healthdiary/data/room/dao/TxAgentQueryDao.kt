package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.TxAgentQueryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TxAgentQuery operations
 * Manages queries sent to the TxAgent API
 */
@Dao
interface TxAgentQueryDao {
    
    /**
     * Insert a new TxAgent query
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(query: TxAgentQueryEntity): Long
    
    /**
     * Insert multiple queries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(queries: List<TxAgentQueryEntity>): List<Long>
    
    /**
     * Update an existing query
     */
    @Update
    suspend fun update(query: TxAgentQueryEntity)
    
    /**
     * Get all queries ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM txagent_queries ORDER BY timestamp DESC")
    suspend fun getAll(): List<TxAgentQueryEntity>
    
    /**
     * Get all queries as a Flow
     */
    @Query("SELECT * FROM txagent_queries ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TxAgentQueryEntity>>
    
    /**
     * Get query by ID
     */
    @Query("SELECT * FROM txagent_queries WHERE id = :id")
    suspend fun getById(id: Long): TxAgentQueryEntity?
    
    /**
     * Get queries by status
     */
    @Query("SELECT * FROM txagent_queries WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getByStatus(status: String): List<TxAgentQueryEntity>
    
    /**
     * Get queries by type
     */
    @Query("SELECT * FROM txagent_queries WHERE queryType = :queryType ORDER BY timestamp DESC")
    suspend fun getByType(queryType: String): List<TxAgentQueryEntity>
    
    /**
     * Get queries for a specific document
     */
    @Query("SELECT * FROM txagent_queries WHERE documentId = :documentId ORDER BY timestamp DESC")
    suspend fun getByDocument(documentId: Long): List<TxAgentQueryEntity>
    
    /**
     * Get pending queries (not yet completed)
     */
    @Query("SELECT * FROM txagent_queries WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPendingQueries(): List<TxAgentQueryEntity>
    
    /**
     * Get completed queries
     */
    @Query("SELECT * FROM txagent_queries WHERE status = 'completed' ORDER BY timestamp DESC")
    suspend fun getCompletedQueries(): List<TxAgentQueryEntity>
    
    /**
     * Get failed queries
     */
    @Query("SELECT * FROM txagent_queries WHERE status = 'failed' ORDER BY timestamp DESC")
    suspend fun getFailedQueries(): List<TxAgentQueryEntity>
    
    /**
     * Search queries by text (partial match)
     */
    @Query("SELECT * FROM txagent_queries WHERE queryText LIKE '%' || :searchTerm || '%' ORDER BY timestamp DESC")
    suspend fun searchByText(searchTerm: String): List<TxAgentQueryEntity>
    
    /**
     * Get queries within a time range
     */
    @Query("SELECT * FROM txagent_queries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<TxAgentQueryEntity>
    
    /**
     * Mark query as completed
     */
    @Query("UPDATE txagent_queries SET status = 'completed', completedAt = :completedAt WHERE id = :queryId")
    suspend fun markAsCompleted(queryId: Long, completedAt: Long)
    
    /**
     * Mark query as failed
     */
    @Query("UPDATE txagent_queries SET status = 'failed', errorMessage = :errorMessage WHERE id = :queryId")
    suspend fun markAsFailed(queryId: Long, errorMessage: String)
    
    /**
     * Update query status and sent timestamp
     */
    @Query("UPDATE txagent_queries SET status = :status, sentAt = :sentAt WHERE id = :queryId")
    suspend fun updateStatus(queryId: Long, status: String, sentAt: Long)
    
    /**
     * Get count of all queries
     */
    @Query("SELECT COUNT(*) FROM txagent_queries")
    suspend fun getCount(): Int
    
    /**
     * Get count by status
     */
    @Query("SELECT COUNT(*) FROM txagent_queries WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int
    
    /**
     * Get count by document
     */
    @Query("SELECT COUNT(*) FROM txagent_queries WHERE documentId = :documentId")
    suspend fun getCountByDocument(documentId: Long): Int
    
    /**
     * Delete query by ID
     */
    @Query("DELETE FROM txagent_queries WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Delete queries older than specified timestamp
     */
    @Query("DELETE FROM txagent_queries WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    /**
     * Delete all queries (use with caution)
     */
    @Query("DELETE FROM txagent_queries")
    suspend fun deleteAll()
    
    /**
     * Get recent queries with limit
     */
    @Query("SELECT * FROM txagent_queries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentQueries(limit: Int = 50): List<TxAgentQueryEntity>
}
