package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.*
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType

class ExerciseOmhConverter : OmhConverter<OmhPhysicalActivity> {
    override fun convert(dataPoint: HealthDataPoint): OmhPhysicalActivity? {
        val startTime = dataPoint.startTime?.toEpochMilli() ?: return null
        val endTime = dataPoint.endTime?.toEpochMilli() ?: return null

        // Get exercise type/name - simplified, return null for now
        val exerciseType: String? = null

        // Get distance, calories, duration - simplified approach
        // These fields may not be directly accessible, so we'll return minimal data
        val distance: Double? = null
        val calories: Double? = null
        val durationSeconds: Long? = null
        val durationMinutes = durationSeconds?.div(60.0)

        return OmhPhysicalActivity(
            header = OmhHeader(
                schema_id = OmhSchemaId(name = "physical-activity")
            ),
            body = OmhPhysicalActivityBody(
                effective_time_frame = EffectiveTimeFrame(
                    time_interval = OmhTimestampUtils.createTimeInterval(startTime, endTime)
                ),
                activity_name = exerciseType,
                distance = distance?.let { DistanceValue(value = it.toDouble()) },
                kcal_burned = calories?.let { KcalBurnedValue(value = it) },
                duration = durationMinutes?.let { DurationValue(value = it) }
            )
        )
    }
}

