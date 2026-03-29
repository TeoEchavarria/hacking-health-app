package com.samsung.android.health.sdk.sample.healthdiary.model

/**
 * Day summary data model.
 * 
 * Contains aggregated health metrics for the current day.
 */
data class DaySummary(
    val steps: Int? = null,
    val sleepHours: Float? = null,
    val description: String = "Estado general estable."
) {
    /**
     * Get formatted steps string
     */
    fun getStepsFormatted(): String {
        return if (steps != null && steps > 0) {
            "%,d pasos".format(steps)
        } else {
            "-- pasos"
        }
    }
    
    /**
     * Get formatted sleep string
     */
    fun getSleepFormatted(): String {
        return if (sleepHours != null && sleepHours > 0) {
            val hours = sleepHours.toInt()
            val minutes = ((sleepHours - hours) * 60).toInt()
            "${hours}h ${minutes}m"
        } else {
            "-- h"
        }
    }
}
