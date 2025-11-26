package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhPhysicalActivity(
    override val header: OmhHeader,
    val body: OmhPhysicalActivityBody
) : OmhDataPoint

@Serializable
data class OmhPhysicalActivityBody(
    val effective_time_frame: EffectiveTimeFrame,
    val activity_name: String? = null,
    val distance: DistanceValue? = null,
    val kcal_burned: KcalBurnedValue? = null,
    val duration: DurationValue? = null
)

@Serializable
data class DistanceValue(
    val value: Double,
    val unit: String = "m"
)

@Serializable
data class KcalBurnedValue(
    val value: Double,
    val unit: String = "kcal"
)

@Serializable
data class DurationValue(
    val value: Double,
    val unit: String = "min"
)


