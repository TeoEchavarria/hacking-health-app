package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for tracking all upload/sync operations from phone to backend
 * Provides audit trail of data synchronization
 */
@Entity(
    tableName = "upload_logs",
    indices = [
        Index(value = ["entityType", "entityId"]),
        Index(value = ["status"]),
        Index(value = ["timestamp"])
    ]
)
data class UploadLogEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    /** Timestamp when the upload was attempted (milliseconds since epoch) */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Type of entity being uploaded (e.g., "sensor_batch", "medical_doc") */
    val entityType: String,
    
    /** ID of the entity being uploaded */
    val entityId: Long,
    
    /** Status of the upload ("success", "failed", "pending") */
    val status: String,
    
    /** API endpoint that was called */
    val endpoint: String,
    
    /** HTTP response code (null if request failed before receiving response) */
    val responseCode: Int? = null,
    
    /** Error message if upload failed */
    val errorMessage: String? = null,
    
    /** Size of data uploaded in bytes */
    val dataSizeBytes: Long? = null,
    
    /** Duration of the upload in milliseconds */
    val durationMs: Long? = null
)
