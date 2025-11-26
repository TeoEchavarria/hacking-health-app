package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import java.time.Instant
import java.time.ZoneOffset

object OmhTimestampUtils {
    /**
     * Converts epoch milliseconds to ISO 8601 string (UTC)
     */
    fun formatTimestamp(millis: Long): String {
        return Instant.ofEpochMilli(millis).toString()
    }

    /**
     * Converts Instant to ISO 8601 string
     */
    fun formatInstant(instant: Instant): String {
        return instant.toString()
    }

    /**
     * Creates a TimeInterval from start and end timestamps
     */
    fun createTimeInterval(startMillis: Long, endMillis: Long): com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.TimeInterval {
        return com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.TimeInterval(
            start_date_time = formatTimestamp(startMillis),
            end_date_time = formatTimestamp(endMillis)
        )
    }
}


