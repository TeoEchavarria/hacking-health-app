package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

// =========================================
// Patient Health Data Models (Caregiver)
// =========================================

/**
 * Single sensor reading from a patient's device.
 */
data class PatientSensorRecord(
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("x")
    val x: Float,
    @SerializedName("y")
    val y: Float,
    @SerializedName("z")
    val z: Float
)

/**
 * Response for patient sensor data request.
 * Returns accelerometer data from the patient's watch.
 */
data class PatientDataResponse(
    @SerializedName("patient_id")
    val patientId: String,
    @SerializedName("records")
    val records: List<PatientSensorRecord>,
    @SerializedName("count")
    val count: Int,
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("oldest_timestamp")
    val oldestTimestamp: Long? = null,
    @SerializedName("newest_timestamp")
    val newestTimestamp: Long? = null
)

// =========================================
// Patient Alerts Models
// =========================================

/**
 * Guidance information for an alert.
 */
data class AlertGuidance(
    @SerializedName("category")
    val category: String, // observe|habit_adjustment|consult_professional|urgent_help
    @SerializedName("primary_message")
    val primaryMessage: String,
    @SerializedName("followup_question")
    val followupQuestion: String? = null,
    @SerializedName("suggested_actions")
    val suggestedActions: List<String>? = null
)

/**
 * Single alert for a patient.
 */
data class PatientAlert(
    @SerializedName("alert_id")
    val alertId: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("severity")
    val severity: String, // info|moderate|high|urgent
    @SerializedName("status")
    val status: String,
    @SerializedName("created_at")
    val createdAt: Long, // timestamp ms
    @SerializedName("title")
    val title: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("guidance")
    val guidance: AlertGuidance? = null,
    @SerializedName("cause")
    val cause: String? = null
)

/**
 * Response for patient alerts request.
 */
data class PatientAlertsResponse(
    @SerializedName("patient_id")
    val patientId: String,
    @SerializedName("alerts")
    val alerts: List<PatientAlert>,
    @SerializedName("count")
    val count: Int,
    @SerializedName("next_cursor")
    val nextCursor: String? = null,
    @SerializedName("has_more")
    val hasMore: Boolean
)

// =========================================
// Sync Request Models (On-Demand Sync)
// =========================================

/**
 * Request body for creating a sync request.
 */
data class SyncRequestCreate(
    @SerializedName("priority")
    val priority: String = "normal"
)

/**
 * Response for sync request creation.
 */
data class SyncRequestResponse(
    @SerializedName("request_id")
    val requestId: String,
    @SerializedName("patient_id")
    val patientId: String,
    @SerializedName("requested_by")
    val requestedBy: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("created_at")
    val createdAt: Long
)

/**
 * Response for checking pending sync requests.
 */
data class PendingSyncResponse(
    @SerializedName("has_pending")
    val hasPending: Boolean,
    @SerializedName("request_id")
    val requestId: String? = null,
    @SerializedName("requested_by")
    val requestedBy: String? = null,
    @SerializedName("priority")
    val priority: String? = null,
    @SerializedName("created_at")
    val createdAt: Long? = null
)

/**
 * Request body for completing a sync request.
 */
data class SyncCompleteRequest(
    @SerializedName("request_id")
    val requestId: String,
    @SerializedName("metrics_synced")
    val metricsSynced: Int = 0
)

/**
 * Response for sync completion.
 */
data class SyncCompleteResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

// =========================================
// Heart Rate History Models
// =========================================

/**
 * Single data point for heart rate history.
 */
data class HeartRateHistoryDataPoint(
    @SerializedName("date")
    val date: String,
    @SerializedName("avg_bpm")
    val avgBpm: Int? = null,
    @SerializedName("min_bpm")
    val minBpm: Int? = null,
    @SerializedName("max_bpm")
    val maxBpm: Int? = null,
    @SerializedName("sample_count")
    val sampleCount: Int = 0
)

/**
 * Response for heart rate history request.
 */
data class HeartRateHistoryResponse(
    @SerializedName("patient_id")
    val patientId: String,
    @SerializedName("patient_name")
    val patientName: String? = null,
    @SerializedName("days_requested")
    val daysRequested: Int,
    @SerializedName("data_points")
    val dataPoints: List<HeartRateHistoryDataPoint>,
    @SerializedName("count")
    val count: Int
)
