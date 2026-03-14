package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing daily step counts received from the watch.
 * This is the PRIMARY source of truth for steps (direct from watch).
 */
@Entity(tableName = "watch_steps")
data class WatchStepsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** ISO date format: "2026-03-14" */
    val date: String,
    
    /** Total step count for the day */
    val steps: Int,
    
    /** Timestamp when this record was last updated */
    val updatedAt: Long = System.currentTimeMillis(),
    
    /** Timestamp when this was synced to backend */
    val syncedAt: Long? = null
)

/**
 * Entity for storing heart rate samples received from the watch.
 */
@Entity(tableName = "watch_heart_rate")
data class WatchHeartRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Heart rate in beats per minute */
    val bpm: Int,
    
    /** Measurement timestamp from watch */
    val measurementTimestamp: Long,
    
    /** Accuracy level: UNKNOWN, LOW, MEDIUM, HIGH */
    val accuracy: String = "UNKNOWN",
    
    /** Timestamp when this record was received */
    val receivedAt: Long = System.currentTimeMillis(),
    
    /** Timestamp when this was synced to backend */
    val syncedAt: Long? = null
)

/**
 * Entity for storing sleep data received from the watch.
 */
@Entity(tableName = "watch_sleep")
data class WatchSleepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** ISO date format: "2026-03-14" */
    val date: String,
    
    /** Total sleep duration in minutes */
    val sleepMinutes: Int,
    
    /** Timestamp when this record was last updated */
    val updatedAt: Long = System.currentTimeMillis(),
    
    /** Timestamp when this was synced to backend */
    val syncedAt: Long? = null
)

/**
 * Entity for storing daily health summaries received from watch.
 * Contains aggregated data for a full day.
 */
@Entity(tableName = "watch_daily_summary")
data class WatchDailySummaryEntity(
    @PrimaryKey
    val date: String, // ISO date format: "2026-03-14"
    
    val steps: Int,
    val sleepMinutes: Int?,
    val avgHeartRate: Int?,
    val minHeartRate: Int?,
    val maxHeartRate: Int?,
    val heartRateSampleCount: Int,
    
    /** JSON array of HeartRateSample for detailed data */
    val heartRateSamplesJson: String?,
    
    /** Timestamp from watch when summary was created */
    val syncTimestamp: Long,
    
    /** Timestamp when this record was received on phone */
    val receivedAt: Long = System.currentTimeMillis(),
    
    /** Timestamp when this was synced to backend */
    val syncedToBackendAt: Long? = null
)
