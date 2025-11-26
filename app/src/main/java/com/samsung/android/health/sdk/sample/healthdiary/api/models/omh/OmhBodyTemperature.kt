package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhBodyTemperature(
    override val header: OmhHeader,
    val body: OmhBodyTemperatureBody
) : OmhDataPoint

@Serializable
data class OmhBodyTemperatureBody(
    val effective_time_frame: EffectiveTimeFrame,
    val body_temperature: BodyTemperatureValue
)

@Serializable
data class BodyTemperatureValue(
    val value: Double,
    val unit: String = "C"
)


