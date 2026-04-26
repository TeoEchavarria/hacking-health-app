package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

/**
 * Voice parse result from LLM - extracted BP values from transcription.
 */
data class VoiceParseResult(
    @SerializedName("systolic") val systolic: Int?,
    @SerializedName("diastolic") val diastolic: Int?,
    @SerializedName("pulse") val pulse: Int?,
    @SerializedName("device_classification") val deviceClassification: String?,
    @SerializedName("confidence") val confidence: String  // "high" or "low"
)

/**
 * Audio parse result from Whisper STT + LLM extraction.
 * Includes the transcription text for user verification.
 */
data class AudioParseResult(
    @SerializedName("systolic") val systolic: Int?,
    @SerializedName("diastolic") val diastolic: Int?,
    @SerializedName("pulse") val pulse: Int?,
    @SerializedName("device_classification") val deviceClassification: String?,
    @SerializedName("confidence") val confidence: String,  // "high" or "low"
    @SerializedName("transcription") val transcription: String  // What was said
)

/**
 * Request body for POST /health/blood-pressure
 */
data class BloodPressureRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("systolic") val systolic: Int,
    @SerializedName("diastolic") val diastolic: Int,
    @SerializedName("pulse") val pulse: Int?,
    @SerializedName("timestamp") val timestamp: String,  // ISO 8601
    @SerializedName("source") val source: String = "voice",
    @SerializedName("crisis_flag") val crisisFlag: Boolean = false,
    @SerializedName("low_confidence_flag") val lowConfidenceFlag: Boolean = false
)

/**
 * Response from POST /health/blood-pressure
 */
data class BloodPressureResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("stage") val stage: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("alert_generated") val alertGenerated: Boolean,
    @SerializedName("message") val message: String?
)

/**
 * Request body for POST /health/parse-bp-voice
 */
data class ParseBPVoiceRequest(
    @SerializedName("transcription") val transcription: String
)

/**
 * BP classification result (mirrors Python classification output)
 */
data class BPClassificationResult(
    val stage: String,
    val severity: String,
    val label: String,
    val guideline: String = "AHA/ACC 2025"
)

/**
 * Heart rate classification result
 */
data class HRClassificationResult(
    val category: String,
    val severity: String,
    val label: String
)

/**
 * Crisis detection result (for immediate edge alerting)
 */
data class CrisisResult(
    val type: String,
    val severity: String,
    val title: String,
    val body: String,
    val guidanceCategory: String
)

/**
 * Validation result for BP/HR readings
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)
