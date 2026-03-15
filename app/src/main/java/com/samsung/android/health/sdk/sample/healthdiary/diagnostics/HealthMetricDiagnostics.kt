package com.samsung.android.health.sdk.sample.healthdiary.diagnostics

/**
 * Diagnostic information for health metrics to help identify pipeline failures.
 * 
 * Tracks the status of data collection, transport, parsing, and persistence
 * for each health metric (heart rate, steps, sleep).
 */
data class HealthMetricDiagnostics(
    val heartRate: HeartRateDiagnostics = HeartRateDiagnostics(),
    val steps: StepsDiagnostics = StepsDiagnostics(),
    val sleep: SleepDiagnostics = SleepDiagnostics()
)

/**
 * Heart rate pipeline diagnostics.
 */
data class HeartRateDiagnostics(
    val isAvailable: Boolean = false,
    val lastCollectedTimestamp: Long? = null,
    val lastSentTimestamp: Long? = null,
    val lastReceivedTimestamp: Long? = null,
    val lastParsedValue: Int? = null,
    val lastPersistedTimestamp: Long? = null,
    val latestValue: Int? = null,
    val sampleCount: Int = 0,
    val reason: String? = null
)

/**
 * Steps pipeline diagnostics.
 */
data class StepsDiagnostics(
    val isAvailable: Boolean = false,
    val lastCollectedTimestamp: Long? = null,
    val lastSentTimestamp: Long? = null,
    val lastReceivedTimestamp: Long? = null,
    val lastParsedValue: Int? = null,
    val lastPersistedTimestamp: Long? = null,
    val latestValue: Int? = null,
    val reason: String? = null
)

/**
 * Sleep pipeline diagnostics.
 */
data class SleepDiagnostics(
    val isAvailable: Boolean = false,
    val lastSleepStateTimestamp: Long? = null,
    val lastSentTimestamp: Long? = null,
    val lastReceivedTimestamp: Long? = null,
    val lastParsedValue: Int? = null, // minutes
    val lastPersistedTimestamp: Long? = null,
    val latestValue: Int? = null, // minutes
    val reason: String? = null
)

/**
 * Format timestamp as "X time ago" or "Never"
 */
fun formatTimestampAgo(timestamp: Long?): String {
    if (timestamp == null) return "Never"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    
    return when {
        seconds < 5 -> "Just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}
