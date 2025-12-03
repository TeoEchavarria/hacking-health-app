package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing queries made to the TxAgent API
 * Tracks medical/health queries with optional document context
 */
@Entity(
    tableName = "txagent_queries",
    foreignKeys = [
        ForeignKey(
            entity = MedicalDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["status"]),
        Index(value = ["queryType"]),
        Index(value = ["timestamp"])
    ]
)
data class TxAgentQueryEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    /** Timestamp when the query was created (milliseconds since epoch) */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** The actual query text sent to TxAgent */
    val queryText: String,
    
    /** Optional reference to a medical document for context */
    val documentId: Long? = null,
    
    /** Type/category of query (e.g., "diagnosis", "treatment", "analysis", "general") */
    val queryType: String,
    
    /** Current status of the query ("pending", "completed", "failed") */
    val status: String,
    
    /** Timestamp when the query was sent to TxAgent */
    val sentAt: Long? = null,
    
    /** Timestamp when the response was received */
    val completedAt: Long? = null,
    
    /** Error message if query failed */
    val errorMessage: String? = null,
    
    /** User ID if multi-user support is added */
    val userId: String? = null
)
