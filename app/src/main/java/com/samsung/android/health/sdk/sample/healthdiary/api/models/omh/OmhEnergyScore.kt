package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhEnergyScore(
    override val header: OmhHeader,
    val body: OmhEnergyScoreBody
) : OmhDataPoint

@Serializable
data class OmhEnergyScoreBody(
    val effective_time_frame: EffectiveTimeFrame,
    val energy_score: EnergyScoreValue
)

@Serializable
data class EnergyScoreValue(
    val value: Int,
    val unit: String = "pts"
)


