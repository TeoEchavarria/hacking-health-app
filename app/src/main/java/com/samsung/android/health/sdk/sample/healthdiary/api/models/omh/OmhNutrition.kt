package com.samsung.android.health.sdk.sample.healthdiary.api.models.omh

import kotlinx.serialization.Serializable

@Serializable
data class OmhNutrition(
    override val header: OmhHeader,
    val body: OmhNutritionBody
) : OmhDataPoint

@Serializable
data class OmhNutritionBody(
    val effective_time_frame: EffectiveTimeFrame,
    val meal_type: String? = null,
    val nutrient: NutrientValue? = null,
    val calories: CaloriesValue? = null
)

@Serializable
data class NutrientValue(
    val name: String,
    val amount: Double,
    val unit: String
)

@Serializable
data class CaloriesValue(
    val value: Double,
    val unit: String = "kcal"
)


