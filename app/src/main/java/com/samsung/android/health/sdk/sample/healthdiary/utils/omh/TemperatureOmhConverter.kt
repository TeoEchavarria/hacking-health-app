package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class TemperatureOmhConverter : OmhConverter<OmhBodyTemperature> {
    override fun convert(dataPoint: HealthDataPoint): OmhBodyTemperature? {
        // Try body temperature first
        val bodyTemp = dataPoint.getValue(DataType.BodyTemperatureType.BODY_TEMPERATURE) as? Float
            ?: dataPoint.getValue(DataType.BodyTemperatureType.BODY_TEMPERATURE) as? Double
            ?: return null

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhBodyTemperature(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "body-temperature")
            ),
            body = OmhBodyTemperatureBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                body_temperature = BodyTemperatureValue(
                    value = bodyTemp.toDouble()
                )
            )
        )
    }
}

class SkinTemperatureOmhConverter : OmhConverter<OmhSkinTemperature> {
    override fun convert(dataPoint: HealthDataPoint): OmhSkinTemperature? {
        val skinTemp = dataPoint.getValue(DataType.SkinTemperatureType.SKIN_TEMPERATURE) as? Float
            ?: dataPoint.getValue(DataType.SkinTemperatureType.SKIN_TEMPERATURE) as? Double
            ?: return null

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhSkinTemperature(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "skin-temperature", namespace = "custom")
            ),
            body = OmhSkinTemperatureBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                skin_temperature = SkinTemperatureValue(
                    value = skinTemp.toDouble()
                )
            )
        )
    }
}

