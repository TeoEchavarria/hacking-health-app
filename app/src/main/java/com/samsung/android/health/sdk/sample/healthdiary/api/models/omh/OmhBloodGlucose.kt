package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhBloodGlucose(
    override val header: OmhHeader,
    val body: OmhBloodGlucoseBody
) : OmhDataPoint

@Serializable
data class OmhBloodGlucoseBody(
    val effective_time_frame: EffectiveTimeFrame,
    val blood_glucose: BloodGlucoseValue
)

@Serializable
data class BloodGlucoseValue(
    val value: Double,
    val unit: String = "mg/dL"
)


