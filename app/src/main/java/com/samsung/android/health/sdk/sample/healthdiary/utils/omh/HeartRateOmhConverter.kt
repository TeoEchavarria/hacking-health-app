package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class HeartRateOmhConverter : OmhConverter<OmhHeartRate> {
    override fun convert(dataPoint: HealthDataPoint): OmhHeartRate? {
        val heartRate = dataPoint.getValue(DataType.HeartRateType.HEART_RATE) as? Float
            ?: dataPoint.getValue(DataType.HeartRateType.HEART_RATE) as? Int
            ?: return null

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhHeartRate(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "heart-rate")
            ),
            body = OmhHeartRateBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                heart_rate = HeartRateValue(
                    value = heartRate.toInt()
                )
            )
        )
    }
}

