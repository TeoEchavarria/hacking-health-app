package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

/**
 * Single heart rate sample for API upload.
 */
data class HeartRateSampleApi(
    @SerializedName("bpm") val bpm: Int,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("accuracy") val accuracy: String? = null
)

/**
 * Request body for POST /health/metrics.
 * Uploads watch health data to backend.
 */
data class HealthMetricsRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("date") val date: String,  // YYYY-MM-DD
    @SerializedName("steps") val steps: Int? = null,
    @SerializedName("sleep_minutes") val sleepMinutes: Int? = null,
    @SerializedName("heart_rate_samples") val heartRateSamples: List<HeartRateSampleApi>? = null,
    @SerializedName("avg_heart_rate") val avgHeartRate: Int? = null,
    @SerializedName("min_heart_rate") val minHeartRate: Int? = null,
    @SerializedName("max_heart_rate") val maxHeartRate: Int? = null,
    @SerializedName("sync_timestamp") val syncTimestamp: Long
)

/**
 * Response from POST /health/metrics.
 */
data class HealthMetricsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("metrics_stored") val metricsStored: Int = 0
)
