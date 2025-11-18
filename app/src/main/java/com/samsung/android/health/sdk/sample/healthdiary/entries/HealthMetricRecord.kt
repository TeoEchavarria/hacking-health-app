package com.samsung.android.health.sdk.sample.healthdiary.entries

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HealthMetricFieldValue(
    val label: String,
    val value: String
)

data class HealthMetricRecord(
    val start: Instant,
    val end: Instant,
    val zoneId: ZoneId,
    val fieldValues: List<HealthMetricFieldValue>,
    val dataSourceName: String?
) {
    companion object {
        private val timeFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    }

    val timeRange: String
        get() = "${timeFormatter.format(start)} - ${timeFormatter.format(end)}"
}




