package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class SleepOmhConverter : OmhConverter<OmhSleepDuration> {
    override fun convert(dataPoint: HealthDataPoint): OmhSleepDuration? {
        val duration = dataPoint.getValue(DataType.SleepType.DURATION) as? Long
            ?: return null

        // Duration is in seconds, convert to minutes
        val durationMinutes = duration / 60.0

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhSleepDuration(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "sleep-duration")
            ),
            body = OmhSleepDurationBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                sleep_duration = SleepDurationValue(
                    value = durationMinutes
                )
            )
        )
    }
}

