package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.TxAgentResponseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TxAgentResponse operations
 * Manages responses received from the TxAgent API
 */
@Dao
interface TxAgentResponseDao {
    
    /**
     * Insert a new TxAgent response
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(response: TxAgentResponseEntity): Long
    
    /**
     * Insert multiple responses
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(responses: List<TxAgentResponseEntity>): List<Long>
    
    /**
     * Update an existing response
     */
    @Update
    suspend fun update(response: TxAgentResponseEntity)
    
    /**
     * Get all responses ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM txagent_responses ORDER BY timestamp DESC")
    suspend fun getAll(): List<TxAgentResponseEntity>
    
    /**
     * Get all responses as a Flow
     */
    @Query("SELECT * FROM txagent_responses ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TxAgentResponseEntity>>
    
    /**
     * Get response by ID
     */
    @Query("SELECT * FROM txagent_responses WHERE id = :id")
    suspend fun getById(id: Long): TxAgentResponseEntity?
    
    /**
     * Get response for a specific query
     */
    @Query("SELECT * FROM txagent_responses WHERE queryId = :queryId")
    suspend fun getByQueryId(queryId: Long): TxAgentResponseEntity?
    
    /**
     * Get responses within a time range
     */
    @Query("SELECT * FROM txagent_responses WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<TxAgentResponseEntity>
    
    /**
     * Get responses with minimum confidence threshold
     */
    @Query("SELECT * FROM txagent_responses WHERE confidence >= :minConfidence ORDER BY confidence DESC")
    suspend fun getByMinConfidence(minConfidence: Float): List<TxAgentResponseEntity>
    
    /**
     * Get responses with user ratings
     */
    @Query("SELECT * FROM txagent_responses WHERE userRating IS NOT NULL ORDER BY userRating DESC, timestamp DESC")
    suspend fun getRatedResponses(): List<TxAgentResponseEntity>
    
    /**
     * Get responses by user rating
     */
    @Query("SELECT * FROM txagent_responses WHERE userRating = :rating ORDER BY timestamp DESC")
    suspend fun getByUserRating(rating: Int): List<TxAgentResponseEntity>
    
    /**
     * Search responses by text (partial match)
     */
    @Query("SELECT * FROM txagent_responses WHERE responseText LIKE '%' || :searchTerm || '%' ORDER BY timestamp DESC")
    suspend fun searchByText(searchTerm: String): List<TxAgentResponseEntity>
    
    /**
     * Update user rating for a response
     */
    @Query("UPDATE txagent_responses SET userRating = :rating WHERE id = :responseId")
    suspend fun updateUserRating(responseId: Long, rating: Int)
    
    /**
     * Update user feedback for a response
     */
    @Query("UPDATE txagent_responses SET userFeedback = :feedback WHERE id = :responseId")
    suspend fun updateUserFeedback(responseId: Long, feedback: String)
    
    /**
     * Update both rating and feedback
     */
    @Query("UPDATE txagent_responses SET userRating = :rating, userFeedback = :feedback WHERE id = :responseId")
    suspend fun updateUserRatingAndFeedback(responseId: Long, rating: Int, feedback: String)
    
    /**
     * Get count of all responses
     */
    @Query("SELECT COUNT(*) FROM txagent_responses")
    suspend fun getCount(): Int
    
    /**
     * Get average confidence score
     */
    @Query("SELECT AVG(confidence) FROM txagent_responses WHERE confidence IS NOT NULL")
    suspend fun getAverageConfidence(): Float?
    
    /**
     * Get average user rating
     */
    @Query("SELECT AVG(userRating) FROM txagent_responses WHERE userRating IS NOT NULL")
    suspend fun getAverageUserRating(): Float?
    
    /**
     * Get average processing time in milliseconds
     */
    @Query("SELECT AVG(processingTimeMs) FROM txagent_responses WHERE processingTimeMs IS NOT NULL")
    suspend fun getAverageProcessingTime(): Float?
    
    /**
     * Delete response by ID
     */
    @Query("DELETE FROM txagent_responses WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Delete responses older than specified timestamp
     */
    @Query("DELETE FROM txagent_responses WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    /**
     * Delete all responses (use with caution)
     */
    @Query("DELETE FROM txagent_responses")
    suspend fun deleteAll()
    
    /**
     * Get recent responses with limit
     */
    @Query("SELECT * FROM txagent_responses ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentResponses(limit: Int = 50): List<TxAgentResponseEntity>
}
