package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhBloodPressure(
    override val header: OmhHeader,
    val body: OmhBloodPressureBody
) : OmhDataPoint

@Serializable
data class OmhBloodPressureBody(
    val effective_time_frame: EffectiveTimeFrame,
    val systolic_blood_pressure: BloodPressureValue,
    val diastolic_blood_pressure: BloodPressureValue
)

@Serializable
data class BloodPressureValue(
    val value: Int,
    val unit: String = "mmHg"
)


