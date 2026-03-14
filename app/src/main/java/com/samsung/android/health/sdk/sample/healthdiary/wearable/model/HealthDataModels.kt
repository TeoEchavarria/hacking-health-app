package com.samsung.android.health.sdk.sample.healthdiary.wearable.model

import kotlinx.serialization.Serializable

/**
 * A single heart rate measurement sample from the watch.
 */
@Serializable
data class HeartRateSample(
    val bpm: Int,
    val timestamp: Long,
    val accuracy: HeartRateAccuracy = HeartRateAccuracy.UNKNOWN
)

@Serializable
enum class HeartRateAccuracy {
    UNKNOWN,
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Daily health summary received from the watch.
 * This is the primary data structure for watch → phone health sync.
 */
@Serializable
data class HealthDailySummary(
    val date: String, // ISO date format: "2026-03-14"
    val steps: Int,
    val sleepMinutes: Int?,
    val heartRateSamples: List<HeartRateSample>,
    val avgHeartRate: Int?,
    val minHeartRate: Int?,
    val maxHeartRate: Int?,
    val syncTimestamp: Long = System.currentTimeMillis()
)

/**
 * Incremental health update received between full daily syncs.
 */
@Serializable
data class HealthIncrementalUpdate(
    val type: UpdateType,
    val timestamp: Long,
    val heartRateSample: HeartRateSample? = null,
    val steps: Int? = null,
    val sleepMinutes: Int? = null
)

@Serializable
enum class UpdateType {
    HEART_RATE,
    STEPS,
    SLEEP
}
