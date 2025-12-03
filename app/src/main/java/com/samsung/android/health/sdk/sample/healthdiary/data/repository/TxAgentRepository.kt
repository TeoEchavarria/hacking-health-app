package com.samsung.android.health.sdk.sample.healthdiary.data.repository

import android.content.Context
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.*
import com.samsung.android.health.sdk.sample.healthdiary.data.mapper.EntityMapper.toDomain
import com.samsung.android.health.sdk.sample.healthdiary.data.mapper.EntityMapper.toEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.TxAgentQueryDao
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.TxAgentResponseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for TxAgent operations
 * Manages both queries and responses together for convenience
 */
class TxAgentRepository(context: Context) {
    
    private val queryDao: TxAgentQueryDao = 
        AppDatabase.getDatabase(context).txAgentQueryDao()
    private val responseDao: TxAgentResponseDao = 
        AppDatabase.getDatabase(context).txAgentResponseDao()
    
    // ========== Query Operations ==========
    
    /**
     * Save a new TxAgent query
     * @return ID of the inserted query
     */
    suspend fun saveQuery(query: TxAgentQuery): Long = withContext(Dispatchers.IO) {
        queryDao.insert(query.toEntity())
    }
    
    /**
     * Update an existing query
     */
    suspend fun updateQuery(query: TxAgentQuery) = withContext(Dispatchers.IO) {
        queryDao.update(query.toEntity())
    }
    
    /**
     * Get all queries
     */
    suspend fun getAllQueries(): List<TxAgentQuery> = withContext(Dispatchers.IO) {
        queryDao.getAll().map { it.toDomain() }
    }
    
    /**
     * Get all queries as Flow
     */
    fun getAllQueriesFlow(): Flow<List<TxAgentQuery>> {
        return queryDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get query by ID
     */
    suspend fun getQueryById(id: Long): TxAgentQuery? = withContext(Dispatchers.IO) {
        queryDao.getById(id)?.toDomain()
    }
    
    /**
     * Get queries by status
     */
    suspend fun getQueriesByStatus(status: QueryStatus): List<TxAgentQuery> =
        withContext(Dispatchers.IO) {
            queryDao.getByStatus(status.value).map { it.toDomain() }
        }
    
    /**
     * Get queries by type
     */
    suspend fun getQueriesByType(queryType: QueryType): List<TxAgentQuery> =
        withContext(Dispatchers.IO) {
            queryDao.getByType(queryType.value).map { it.toDomain() }
        }
    
    /**
     * Get queries for a specific document
     */
    suspend fun getQueriesForDocument(documentId: Long): List<TxAgentQuery> =
        withContext(Dispatchers.IO) {
            queryDao.getByDocument(documentId).map { it.toDomain() }
        }
    
    /**
     * Get pending queries
     */
    suspend fun getPendingQueries(): List<TxAgentQuery> = withContext(Dispatchers.IO) {
        queryDao.getPendingQueries().map { it.toDomain() }
    }
    
    /**
     * Search queries by text
     */
    suspend fun searchQueries(searchTerm: String): List<TxAgentQuery> =
        withContext(Dispatchers.IO) {
            queryDao.searchByText(searchTerm).map { it.toDomain() }
        }
    
    /**
     * Mark query as completed
     */
    suspend fun markQueryAsCompleted(queryId: Long) = withContext(Dispatchers.IO) {
        queryDao.markAsCompleted(queryId, System.currentTimeMillis())
    }
    
    /**
     * Mark query as failed
     */
    suspend fun markQueryAsFailed(queryId: Long, errorMessage: String) = 
        withContext(Dispatchers.IO) {
            queryDao.markAsFailed(queryId, errorMessage)
        }
    
    // ========== Response Operations ==========
    
    /**
     * Save a TxAgent response
     * @return ID of the inserted response
     */
    suspend fun saveResponse(response: TxAgentResponse): Long = withContext(Dispatchers.IO) {
        responseDao.insert(response.toEntity())
    }
    
    /**
     * Update an existing response
     */
    suspend fun updateResponse(response: TxAgentResponse) = withContext(Dispatchers.IO) {
        responseDao.update(response.toEntity())
    }
    
    /**
     * Get response for a query
     */
    suspend fun getResponseForQuery(queryId: Long): TxAgentResponse? = 
        withContext(Dispatchers.IO) {
            responseDao.getByQueryId(queryId)?.toDomain()
        }
    
    /**
     * Get all responses
     */
    suspend fun getAllResponses(): List<TxAgentResponse> = withContext(Dispatchers.IO) {
        responseDao.getAll().map { it.toDomain() }
    }
    
    /**
     * Search responses by text
     */
    suspend fun searchResponses(searchTerm: String): List<TxAgentResponse> =
        withContext(Dispatchers.IO) {
            responseDao.searchByText(searchTerm).map { it.toDomain() }
        }
    
    /**
     * Get responses with minimum confidence
     */
    suspend fun getResponsesByMinConfidence(minConfidence: Float): List<TxAgentResponse> =
        withContext(Dispatchers.IO) {
            responseDao.getByMinConfidence(minConfidence).map { it.toDomain() }
        }
    
    /**
     * Update user rating for a response
     */
    suspend fun updateResponseRating(responseId: Long, rating: Int) = 
        withContext(Dispatchers.IO) {
            responseDao.updateUserRating(responseId, rating)
        }
    
    /**
     * Update user feedback for a response
     */
    suspend fun updateResponseFeedback(responseId: Long, feedback: String) =
        withContext(Dispatchers.IO) {
            responseDao.updateUserFeedback(responseId, feedback)
        }
    
    // ========== Combined Operations ==========
    
    /**
     * Get query with its response
     */
    suspend fun getQueryWithResponse(queryId: Long): QueryWithResponse? = 
        withContext(Dispatchers.IO) {
            val query = queryDao.getById(queryId)?.toDomain() ?: return@withContext null
            val response = responseDao.getByQueryId(queryId)?.toDomain()
            QueryWithResponse(query, response)
        }
    
    /**
     * Get all queries with their responses
     */
    suspend fun getAllQueriesWithResponses(): List<QueryWithResponse> = 
        withContext(Dispatchers.IO) {
            val queries = queryDao.getAll().map { it.toDomain() }
            queries.map { query ->
                val response = responseDao.getByQueryId(query.id)?.toDomain()
                QueryWithResponse(query, response)
            }
        }
    
    /**
     * Save query and response together
     * @return Pair of (queryId, responseId)
     */
    suspend fun saveQueryAndResponse(
        query: TxAgentQuery, 
        response: TxAgentResponse
    ): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val queryId = queryDao.insert(query.toEntity())
        val updatedResponse = response.copy(queryId = queryId)
        val responseId = responseDao.insert(updatedResponse.toEntity())
        Pair(queryId, responseId)
    }
    
    /**
     * Get analytics: average confidence
     */
    suspend fun getAverageConfidence(): Float = withContext(Dispatchers.IO) {
        responseDao.getAverageConfidence() ?: 0.0f
    }
    
    /**
     * Get analytics: average user rating
     */
    suspend fun getAverageUserRating(): Float = withContext(Dispatchers.IO) {
        responseDao.getAverageUserRating() ?: 0.0f
    }
    
    /**
     * Get analytics: average processing time
     */
    suspend fun getAverageProcessingTime(): Float = withContext(Dispatchers.IO) {
        responseDao.getAverageProcessingTime() ?: 0.0f
    }
    
    /**
     * Delete old queries and their responses
     * @param daysOld Delete queries older than this many days
     * @return Number of queries deleted
     */
    suspend fun deleteOldQueries(daysOld: Int = 90): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        queryDao.deleteOlderThan(cutoffTime)
    }
}
