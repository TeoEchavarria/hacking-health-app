package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing metadata about uploaded PDF medical history files
 * Actual file content is stored in app's private storage, this tracks metadata
 */
@Entity(
    tableName = "medical_documents",
    indices = [
        Index(value = ["filename"]),
        Index(value = ["uploadTimestamp"]),
        Index(value = ["processed"])
    ]
)
data class MedicalDocumentEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    /** Original filename of the uploaded document */
    val filename: String,
    
    /** Absolute file path where the document is stored on device */
    val filePath: String,
    
    /** Timestamp when the document was uploaded (milliseconds since epoch) */
    val uploadTimestamp: Long = System.currentTimeMillis(),
    
    /** Size of the file in bytes */
    val fileSize: Long,
    
    /** MIME type of the document (e.g., "application/pdf") */
    val mimeType: String,
    
    /** User-provided description or notes about the document */
    val description: String? = null,
    
    /** Whether the document has been processed/analyzed by TxAgent */
    val processed: Boolean = false,
    
    /** Timestamp when processing was completed */
    val processedAt: Long? = null,
    
    /** SHA-256 hash of the file for integrity verification */
    val fileHash: String? = null,
    
    /** Number of pages in the document (if PDF) */
    val pageCount: Int? = null,
    
    /** Tags or categories assigned to the document */
    val tags: String? = null // Comma-separated tags
)
