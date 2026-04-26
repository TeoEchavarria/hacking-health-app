package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samsung.android.health.sdk.sample.healthdiary.api.models.BloodPressureRequest
import java.util.UUID

/**
 * Sync status state machine for offline-first data synchronization.
 *
 * States:
 * - PENDING: Captured locally but not yet sent to server
 * - SYNCING: Currently uploading to server
 * - SYNCED: Successfully acknowledged by server
 * - FAILED: Upload failed, ready for retry
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

/**
 * Room entity for blood pressure readings with offline sync support.
 *
 * The idempotencyKey serves as both the primary key and the HTTP
 * Idempotency-Key header to prevent duplicate entries on retry.
 */
@Entity(
    tableName = "blood_pressure_readings",
    indices = [
        Index(value = ["syncStatus"]),
        Index(value = ["userId"]),
        Index(value = ["createdAt"])
    ]
)
data class BloodPressureReadingEntity(
    @PrimaryKey
    val idempotencyKey: String = UUID.randomUUID().toString(),
    
    val userId: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val timestamp: String,  // ISO 8601
    val source: String = "voice",
    val crisisFlag: Boolean = false,
    val lowConfidenceFlag: Boolean = false,
    
    // Sync state machine
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAttempt: Long? = null,
    val syncError: String? = null,
    
    // Server response (populated after successful sync)
    val serverStage: String? = null,
    val serverSeverity: String? = null,
    val serverAlertGenerated: Boolean? = null
) {
    /**
     * Convert to API request model.
     */
    fun toRequest(): BloodPressureRequest {
        return BloodPressureRequest(
            userId = userId,
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            timestamp = timestamp,
            source = source,
            crisisFlag = crisisFlag,
            lowConfidenceFlag = lowConfidenceFlag
        )
    }
}
