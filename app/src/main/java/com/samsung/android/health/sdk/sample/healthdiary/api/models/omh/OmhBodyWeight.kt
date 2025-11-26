package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhBodyWeight(
    override val header: OmhHeader,
    val body: OmhBodyWeightBody
) : OmhDataPoint

@Serializable
data class OmhBodyWeightBody(
    val effective_time_frame: EffectiveTimeFrame,
    val body_weight: BodyWeightValue
)

@Serializable
data class BodyWeightValue(
    val value: Double,
    val unit: String = "kg"
)


