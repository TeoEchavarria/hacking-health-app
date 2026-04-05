package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

/**
 * Heart rate statistics.
 */
data class HeartRateSummary(
    val available: Boolean = false,
    val average: Int? = null,
    val min: Int? = null,
    val max: Int? = null,
    @SerializedName("last_reading") val lastReading: Int? = null,
    @SerializedName("last_reading_time") val lastReadingTime: Long? = null
)

/**
 * Steps statistics.
 */
data class StepsSummary(
    val available: Boolean = false,
    val total: Int? = null,
    @SerializedName("last_updated") val lastUpdated: Long? = null
)

/**
 * Sleep statistics.
 */
data class SleepSummary(
    val available: Boolean = false,
    @SerializedName("total_minutes") val totalMinutes: Int? = null,
    @SerializedName("last_night") val lastNight: Int? = null,
    @SerializedName("last_updated") val lastUpdated: Long? = null
)

/**
 * Metric that is not available from device.
 */
data class UnavailableMetric(
    val name: String,
    val reason: String = "NO DISPONIBLE PARA TU DISPOSITIVO"
)

/**
 * Patient health summary response.
 * Contains last 24 hours of health data.
 */
data class PatientHealthSummaryResponse(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("patient_name") val patientName: String? = null,
    @SerializedName("heart_rate") val heartRate: HeartRateSummary = HeartRateSummary(),
    val steps: StepsSummary = StepsSummary(),
    val sleep: SleepSummary = SleepSummary(),
    @SerializedName("unavailable_metrics") val unavailableMetrics: List<UnavailableMetric> = listOf(
        UnavailableMetric("SpO2"),
        UnavailableMetric("Presión Arterial"),
        UnavailableMetric("Temperatura")
    ),
    @SerializedName("last_sync") val lastSync: Long? = null,
    @SerializedName("data_available") val dataAvailable: Boolean = false
) {
    /**
     * Check if heart rate data is available.
     */
    fun hasHeartRate(): Boolean = heartRate.available && heartRate.average != null
    
    /**
     * Check if steps data is available.
     */
    fun hasSteps(): Boolean = steps.available && steps.total != null
    
    /**
     * Check if sleep data is available.
     */
    fun hasSleep(): Boolean = sleep.available && sleep.totalMinutes != null
    
    /**
     * Get formatted heart rate string.
     */
    fun getHeartRateDisplay(): String {
        return if (hasHeartRate()) {
            "${heartRate.average} BPM"
        } else {
            "Sin datos"
        }
    }
    
    /**
     * Get formatted steps string.
     */
    fun getStepsDisplay(): String {
        return if (hasSteps()) {
            "${steps.total} pasos"
        } else {
            "Sin datos"
        }
    }
    
    /**
     * Get formatted sleep string.
     */
    fun getSleepDisplay(): String {
        return if (hasSleep()) {
            val hours = (sleep.totalMinutes ?: 0) / 60
            val mins = (sleep.totalMinutes ?: 0) % 60
            "${hours}h ${mins}m"
        } else {
            "Sin datos"
        }
    }
}
