package com.samsung.android.health.sdk.sample.healthdiary.utils

import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricDefinition
import com.samsung.android.sdk.health.data.data.DataPoint
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.UserDataPoint
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes

object HealthMetricRegistry {

    val metrics: List<HealthMetricDefinition<out DataPoint>> = listOf(
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.EXERCISE_ACTIVITY,
            titleRes = R.string.exercise,
            iconRes = R.drawable.ic_metric_exercise,
            dataType = DataTypes.EXERCISE,
            permissions = setOf(Permission.of(DataTypes.EXERCISE, AccessType.READ)),
            summaryUnit = AppConstants.DURATION_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.EXERCISE_LOCATION_ACTIVITY,
            titleRes = R.string.exercise_location,
            iconRes = R.drawable.ic_metric_exercise_location,
            dataType = DataTypes.EXERCISE_LOCATION,
            permissions = setOf(Permission.of(DataTypes.EXERCISE_LOCATION, AccessType.READ))
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.SKIN_TEMPERATURE_ACTIVITY,
            titleRes = R.string.skin_temperature,
            iconRes = R.drawable.ic_metric_skin_temperature,
            dataType = DataTypes.SKIN_TEMPERATURE,
            permissions = setOf(Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ)),
            summaryUnit = AppConstants.SKIN_TEMP_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.BLOOD_OXYGEN_ACTIVITY,
            titleRes = R.string.blood_oxygen,
            iconRes = R.drawable.ic_metric_blood_oxygen,
            dataType = DataTypes.BLOOD_OXYGEN,
            permissions = setOf(Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ)),
            summaryUnit = AppConstants.BLOOD_OXYGEN_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.ACTIVITY_SUMMARY_ACTIVITY,
            titleRes = R.string.activity_summary,
            iconRes = R.drawable.ic_metric_activity_summary,
            dataType = DataTypes.ACTIVITY_SUMMARY,
            permissions = setOf(Permission.of(DataTypes.ACTIVITY_SUMMARY, AccessType.READ))
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.FLOORS_CLIMBED_ACTIVITY,
            titleRes = R.string.floors_climbed,
            iconRes = R.drawable.ic_metric_floors_climbed,
            dataType = DataTypes.FLOORS_CLIMBED,
            permissions = setOf(Permission.of(DataTypes.FLOORS_CLIMBED, AccessType.READ)),
            summaryUnit = AppConstants.FLOOR_COUNT_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.BLOOD_GLUCOSE_ACTIVITY,
            titleRes = R.string.blood_glucose,
            iconRes = R.drawable.ic_metric_blood_glucose,
            dataType = DataTypes.BLOOD_GLUCOSE,
            permissions = setOf(Permission.of(DataTypes.BLOOD_GLUCOSE, AccessType.READ)),
            summaryUnit = AppConstants.BLOOD_GLUCOSE_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.BLOOD_PRESSURE_ACTIVITY,
            titleRes = R.string.blood_pressure,
            iconRes = R.drawable.ic_metric_blood_pressure,
            dataType = DataTypes.BLOOD_PRESSURE,
            permissions = setOf(Permission.of(DataTypes.BLOOD_PRESSURE, AccessType.READ)),
            summaryUnit = AppConstants.BLOOD_PRESSURE_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.BODY_COMPOSITION_ACTIVITY,
            titleRes = R.string.body_composition,
            iconRes = R.drawable.ic_metric_body_composition,
            dataType = DataTypes.BODY_COMPOSITION,
            permissions = setOf(Permission.of(DataTypes.BODY_COMPOSITION, AccessType.READ)),
            summaryUnit = AppConstants.BODY_FAT_PERCENT_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.SLEEP_GOAL_ACTIVITY,
            titleRes = R.string.sleep_goal,
            iconRes = R.drawable.ic_metric_sleep_goal,
            dataType = DataTypes.SLEEP_GOAL,
            permissions = setOf(Permission.of(DataTypes.SLEEP_GOAL, AccessType.READ)),
            summaryUnit = AppConstants.DURATION_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.STEPS_GOAL_ACTIVITY,
            titleRes = R.string.steps_goal,
            iconRes = R.drawable.ic_metric_steps_goal,
            dataType = DataTypes.STEPS_GOAL,
            permissions = setOf(Permission.of(DataTypes.STEPS_GOAL, AccessType.READ))
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.ACTIVE_CALORIES_GOAL_ACTIVITY,
            titleRes = R.string.active_calories_goal,
            iconRes = R.drawable.ic_metric_active_calories_goal,
            dataType = DataTypes.ACTIVE_CALORIES_BURNED_GOAL,
            permissions = setOf(Permission.of(DataTypes.ACTIVE_CALORIES_BURNED_GOAL, AccessType.READ)),
            summaryUnit = AppConstants.CALORIES_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.ACTIVE_TIME_GOAL_ACTIVITY,
            titleRes = R.string.active_time_goal,
            iconRes = R.drawable.ic_metric_active_time_goal,
            dataType = DataTypes.ACTIVE_TIME_GOAL,
            permissions = setOf(Permission.of(DataTypes.ACTIVE_TIME_GOAL, AccessType.READ)),
            summaryUnit = AppConstants.DURATION_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.WATER_INTAKE_ACTIVITY,
            titleRes = R.string.water_intake,
            iconRes = R.drawable.ic_metric_water_intake,
            dataType = DataTypes.WATER_INTAKE,
            permissions = setOf(Permission.of(DataTypes.WATER_INTAKE, AccessType.READ)),
            summaryUnit = AppConstants.WATER_VOLUME_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.WATER_INTAKE_GOAL_ACTIVITY,
            titleRes = R.string.water_intake_goal,
            iconRes = R.drawable.ic_metric_water_intake_goal,
            dataType = DataTypes.WATER_INTAKE_GOAL,
            permissions = setOf(Permission.of(DataTypes.WATER_INTAKE_GOAL, AccessType.READ)),
            summaryUnit = AppConstants.WATER_VOLUME_UNIT
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.NUTRITION_GOAL_ACTIVITY,
            titleRes = R.string.nutrition_goal,
            iconRes = R.drawable.ic_metric_nutrition_goal,
            dataType = DataTypes.NUTRITION_GOAL,
            permissions = setOf(Permission.of(DataTypes.NUTRITION_GOAL, AccessType.READ))
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.ENERGY_SCORE_ACTIVITY,
            titleRes = R.string.energy_score,
            iconRes = R.drawable.ic_metric_energy_score,
            dataType = DataTypes.ENERGY_SCORE,
            permissions = setOf(Permission.of(DataTypes.ENERGY_SCORE, AccessType.READ)),
            summaryUnit = AppConstants.SCORE_UNIT
        ),
        HealthMetricDefinition<UserDataPoint>(
            activityId = AppConstants.USER_PROFILE_ACTIVITY,
            titleRes = R.string.user_profile,
            iconRes = R.drawable.ic_metric_user_profile,
            dataType = DataTypes.USER_PROFILE,
            permissions = setOf(Permission.of(DataTypes.USER_PROFILE, AccessType.READ))
        ),
        HealthMetricDefinition<HealthDataPoint>(
            activityId = AppConstants.BODY_TEMPERATURE_ACTIVITY,
            titleRes = R.string.body_temperature,
            iconRes = R.drawable.ic_metric_body_temperature,
            dataType = DataTypes.BODY_TEMPERATURE,
            permissions = setOf(Permission.of(DataTypes.BODY_TEMPERATURE, AccessType.READ)),
            summaryUnit = AppConstants.BODY_TEMP_UNIT
        )
    )

    fun getMetric(activityId: Int): HealthMetricDefinition<out DataPoint>? =
        metrics.firstOrNull { it.activityId == activityId }
}

