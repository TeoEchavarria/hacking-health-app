package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhOxygenSaturation(
    override val header: OmhHeader,
    val body: OmhOxygenSaturationBody
) : OmhDataPoint

@Serializable
data class OmhOxygenSaturationBody(
    val effective_time_frame: EffectiveTimeFrame,
    val oxygen_saturation: OxygenSaturationValue
)

@Serializable
data class OxygenSaturationValue(
    val value: Double,
    val unit: String = "%"
)


