package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.AggregatedData
import com.samsung.android.sdk.health.data.data.HealthDataPoint

class StepOmhConverter : OmhConverter<OmhStepCount> {
    override fun convert(dataPoint: HealthDataPoint): OmhStepCount? {
        // Steps are typically handled via aggregated data, not individual data points
        // Return null here and use convertAggregated instead
        return null
    }

    override fun convertAggregated(aggregatedData: AggregatedData<*>): OmhStepCount? {
        val stepCount = aggregatedData.value as? Long ?: aggregatedData.value as? Int ?: return null
        val startTime = aggregatedData.startTime?.toEpochMilli() ?: return null
        val endTime = aggregatedData.endTime?.toEpochMilli() ?: return null

        return OmhStepCount(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "step-count")
            ),
            body = OmhStepCountBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                step_count = stepCount.toInt()
            )
        )
    }
}

