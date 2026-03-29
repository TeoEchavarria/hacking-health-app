package com.samsung.android.health.sdk.sample.healthdiary.model

/**
 * Health alert for urgent conditions.
 * 
 * Used to display critical health warnings to the user.
 */
data class HealthAlert(
    val title: String,
    val description: String,
    val timestamp: Long,
    val metricType: String
)
