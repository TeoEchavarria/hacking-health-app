package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhFluidIntake(
    override val header: OmhHeader,
    val body: OmhFluidIntakeBody
) : OmhDataPoint

@Serializable
data class OmhFluidIntakeBody(
    val effective_time_frame: EffectiveTimeFrame,
    val fluid: FluidValue
)

@Serializable
data class FluidValue(
    val value: Double,
    val unit: String = "mL"
)


