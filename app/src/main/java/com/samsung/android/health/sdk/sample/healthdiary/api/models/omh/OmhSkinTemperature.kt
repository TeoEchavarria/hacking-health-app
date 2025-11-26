package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhSkinTemperature(
    override val header: OmhHeader,
    val body: OmhSkinTemperatureBody
) : OmhDataPoint

@Serializable
data class OmhSkinTemperatureBody(
    val effective_time_frame: EffectiveTimeFrame,
    val skin_temperature: SkinTemperatureValue
)

@Serializable
data class SkinTemperatureValue(
    val value: Double,
    val unit: String = "C"
)


