package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhHeartRate(
    override val header: OmhHeader,
    val body: OmhHeartRateBody
) : OmhDataPoint

@Serializable
data class OmhHeartRateBody(
    val effective_time_frame: EffectiveTimeFrame,
    val heart_rate: HeartRateValue
)

@Serializable
data class HeartRateValue(
    val value: Int,
    val unit: String = "beats/min"
)


