package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class BloodPressureOmhConverter : OmhConverter<OmhBloodPressure> {
    override fun convert(dataPoint: HealthDataPoint): OmhBloodPressure? {
        val systolic = dataPoint.getValue(DataType.BloodPressureType.SYSTOLIC) as? Int
            ?: (dataPoint.getValue(DataType.BloodPressureType.SYSTOLIC) as? Float)?.toInt()
            ?: return null

        val diastolic = dataPoint.getValue(DataType.BloodPressureType.DIASTOLIC) as? Int
            ?: (dataPoint.getValue(DataType.BloodPressureType.DIASTOLIC) as? Float)?.toInt()
            ?: return null

        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        return OmhBloodPressure(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "blood-pressure")
            ),
            body = OmhBloodPressureBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                systolic_blood_pressure = BloodPressureValue(value = systolic),
                diastolic_blood_pressure = BloodPressureValue(value = diastolic)
            )
        )
    }
}

