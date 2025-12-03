package com.samsung.android.health.sdk.sample.healthdiary.data.domain

/**
 * Domain model for medical documents
 */
data class MedicalDocument(
    val id: Long = 0,
    val filename: String,
    val filePath: String,
    val uploadTimestamp: Long,
    val fileSize: Long,
    val mimeType: String,
    val description: String? = null,
    val processed: Boolean = false,
    val processedAt: Long? = null,
    val fileHash: String? = null,
    val pageCount: Int? = null,
    val tags: List<String> = emptyList()
)
