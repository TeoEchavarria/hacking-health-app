package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhStepCount(
    override val header: OmhHeader,
    val body: OmhStepCountBody
) : OmhDataPoint

@Serializable
data class OmhStepCountBody(
    val effective_time_frame: EffectiveTimeFrame,
    val step_count: Int
)


