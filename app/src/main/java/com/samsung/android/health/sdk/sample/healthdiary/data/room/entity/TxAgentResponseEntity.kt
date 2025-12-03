package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing responses from the TxAgent API
 * Each response is linked to a specific query
 */
@Entity(
    tableName = "txagent_responses",
    foreignKeys = [
        ForeignKey(
            entity = TxAgentQueryEntity::class,
            parentColumns = ["id"],
            childColumns = ["queryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["queryId"], unique = true), // One response per query
        Index(value = ["timestamp"])
    ]
)
data class TxAgentResponseEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    /** Foreign key to the associated query */
    val queryId: Long,
    
    /** Timestamp when the response was received (milliseconds since epoch) */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** The response text from TxAgent */
    val responseText: String,
    
    /** Additional metadata from TxAgent in JSON format */
    val metadata: String? = null,
    
    /** Confidence score of the response (0.0 to 1.0) */
    val confidence: Float? = null,
    
    /** Sources or citations provided by TxAgent */
    val sources: String? = null, // JSON array of sources
    
    /** Whether the user found this response helpful */
    val userRating: Int? = null, // 1-5 stars
    
    /** User feedback or notes on the response */
    val userFeedback: String? = null,
    
    /** Processing time on the server in milliseconds */
    val processingTimeMs: Long? = null,
    
    /** Model version that generated the response */
    val modelVersion: String? = null
)
