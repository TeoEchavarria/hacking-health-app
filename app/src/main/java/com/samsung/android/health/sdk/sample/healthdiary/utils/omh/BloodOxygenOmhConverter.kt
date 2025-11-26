package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class BloodOxygenOmhConverter : OmhConverter<OmhOxygenSaturation> {
    override fun convert(dataPoint: HealthDataPoint): OmhOxygenSaturation? {
        val oxygenSaturation = dataPoint.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION) as? Float
            ?: dataPoint.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION) as? Double
            ?: return null

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhOxygenSaturation(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "oxygen-saturation")
            ),
            body = OmhOxygenSaturationBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                oxygen_saturation = OxygenSaturationValue(
                    value = oxygenSaturation.toDouble()
                )
            )
        )
    }
}

