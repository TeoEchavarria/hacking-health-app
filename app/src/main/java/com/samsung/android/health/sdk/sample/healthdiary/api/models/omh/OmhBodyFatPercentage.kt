package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhBodyFatPercentage(
    override val header: OmhHeader,
    val body: OmhBodyFatPercentageBody
) : OmhDataPoint

@Serializable
data class OmhBodyFatPercentageBody(
    val effective_time_frame: EffectiveTimeFrame,
    val body_fat_percentage: BodyFatPercentageValue
)

@Serializable
data class BodyFatPercentageValue(
    val value: Double,
    val unit: String = "%"
)


