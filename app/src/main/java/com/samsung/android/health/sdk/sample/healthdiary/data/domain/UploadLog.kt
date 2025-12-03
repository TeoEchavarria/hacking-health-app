package com.samsung.android.health.sdk.sample.healthdiary.data.domain

/**
 * Domain model for upload/sync log entries
 */
data class UploadLog(
    val id: Long = 0,
    val timestamp: Long,
    val entityType: EntityType,
    val entityId: Long,
    val status: UploadStatus,
    val endpoint: String,
    val responseCode: Int? = null,
    val errorMessage: String? = null,
    val dataSizeBytes: Long? = null,
    val durationMs: Long? = null
)

enum class EntityType(val value: String) {
    SENSOR_BATCH("sensor_batch"),
    MEDICAL_DOCUMENT("medical_doc"),
    HEALTH_DATA("health_data"),
    NUTRITION("nutrition");
    
    companion object {
        fun fromValue(value: String): EntityType {
            return values().find { it.value == value } ?: SENSOR_BATCH
        }
    }
}

enum class UploadStatus(val value: String) {
    SUCCESS("success"),
    FAILED("failed"),
    PENDING("pending");
    
    companion object {
        fun fromValue(value: String): UploadStatus {
            return values().find { it.value == value } ?: PENDING
        }
    }
}
