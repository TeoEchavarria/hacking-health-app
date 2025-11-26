package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhFloorsClimbed(
    override val header: OmhHeader,
    val body: OmhFloorsClimbedBody
) : OmhDataPoint

@Serializable
data class OmhFloorsClimbedBody(
    val effective_time_frame: EffectiveTimeFrame,
    val floors_climbed: FloorsClimbedValue
)

@Serializable
data class FloorsClimbedValue(
    val value: Int,
    val unit: String = "floors"
)


