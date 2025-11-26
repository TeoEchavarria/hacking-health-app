package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class BodyCompositionOmhConverter : OmhConverter<OmhBodyWeight> {
    override fun convert(dataPoint: HealthDataPoint): OmhBodyWeight? {
        val weight = dataPoint.getValue(DataType.BodyCompositionType.WEIGHT) as? Float
            ?: dataPoint.getValue(DataType.BodyCompositionType.WEIGHT) as? Double
            ?: return null

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhBodyWeight(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "body-weight")
            ),
            body = OmhBodyWeightBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                body_weight = BodyWeightValue(
                    value = weight.toDouble()
                )
            )
        )
    }
}

class BodyFatPercentageOmhConverter : OmhConverter<OmhBodyFatPercentage> {
    override fun convert(dataPoint: HealthDataPoint): OmhBodyFatPercentage? {
        val bodyFat = dataPoint.getValue(DataType.BodyCompositionType.BODY_FAT) as? Float
            ?: dataPoint.getValue(DataType.BodyCompositionType.BODY_FAT) as? Double
            ?: return null

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhBodyFatPercentage(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "body-fat-percentage")
            ),
            body = OmhBodyFatPercentageBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                body_fat_percentage = BodyFatPercentageValue(
                    value = bodyFat.toDouble()
                )
            )
        )
    }
}

