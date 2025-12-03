package com.samsung.android.health.sdk.sample.healthdiary.data.domain

/**
 * Domain models for TxAgent queries and responses
 */
data class TxAgentQuery(
    val id: Long = 0,
    val timestamp: Long,
    val queryText: String,
    val documentId: Long? = null,
    val queryType: QueryType,
    val status: QueryStatus,
    val sentAt: Long? = null,
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val userId: String? = null
)

data class TxAgentResponse(
    val id: Long = 0,
    val queryId: Long,
    val timestamp: Long,
    val responseText: String,
    val metadata: Map<String, Any>? = null,
    val confidence: Float? = null,
    val sources: List<String>? = null,
    val userRating: Int? = null,
    val userFeedback: String? = null,
    val processingTimeMs: Long? = null,
    val modelVersion: String? = null
)

/**
 * Combined model for query with its response
 */
data class QueryWithResponse(
    val query: TxAgentQuery,
    val response: TxAgentResponse?
)

enum class QueryType(val value: String) {
    DIAGNOSIS("diagnosis"),
    TREATMENT("treatment"),
    ANALYSIS("analysis"),
    GENERAL("general");
    
    companion object {
        fun fromValue(value: String): QueryType {
            return values().find { it.value == value } ?: GENERAL
        }
    }
}

enum class QueryStatus(val value: String) {
    PENDING("pending"),
    COMPLETED("completed"),
    FAILED("failed");
    
    companion object {
        fun fromValue(value: String): QueryStatus {
            return values().find { it.value == value } ?: PENDING
        }
    }
}
