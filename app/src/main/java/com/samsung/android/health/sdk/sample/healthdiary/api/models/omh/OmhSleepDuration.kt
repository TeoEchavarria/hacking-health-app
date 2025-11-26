package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhSleepDuration(
    override val header: OmhHeader,
    val body: OmhSleepDurationBody
) : OmhDataPoint

@Serializable
data class OmhSleepDurationBody(
    val effective_time_frame: EffectiveTimeFrame,
    val sleep_duration: SleepDurationValue
)

@Serializable
data class SleepDurationValue(
    val value: Double,
    val unit: String = "min"
)


