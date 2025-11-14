package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricDefinition
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import com.samsung.android.health.sdk.sample.healthdiary.utils.HealthMetricRegistry
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.DataPoint
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.UserDataPoint

@Suppress("UNCHECKED_CAST")
private fun <T : DataPoint> metricDefinition(activityId: Int): HealthMetricDefinition<T> =
    requireNotNull(HealthMetricRegistry.getMetric(activityId)) {
        "Missing metric definition for activityId=$activityId"
    } as HealthMetricDefinition<T>

class ExerciseViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.EXERCISE_ACTIVITY))

class ExerciseLocationViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.EXERCISE_LOCATION_ACTIVITY))

class SkinTemperatureDetailViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.SKIN_TEMPERATURE_ACTIVITY))

class BloodOxygenDetailViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.BLOOD_OXYGEN_ACTIVITY))

class ActivitySummaryViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.ACTIVITY_SUMMARY_ACTIVITY))

class FloorsClimbedViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.FLOORS_CLIMBED_ACTIVITY))

class BloodGlucoseViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.BLOOD_GLUCOSE_ACTIVITY))

class BloodPressureDetailViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.BLOOD_PRESSURE_ACTIVITY))

class BodyCompositionViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.BODY_COMPOSITION_ACTIVITY))

class SleepGoalViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.SLEEP_GOAL_ACTIVITY))

class StepsGoalViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.STEPS_GOAL_ACTIVITY))

class ActiveCaloriesGoalViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.ACTIVE_CALORIES_GOAL_ACTIVITY))

class ActiveTimeGoalViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.ACTIVE_TIME_GOAL_ACTIVITY))

class WaterIntakeViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.WATER_INTAKE_ACTIVITY))

class WaterIntakeGoalViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.WATER_INTAKE_GOAL_ACTIVITY))

class NutritionGoalViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.NUTRITION_GOAL_ACTIVITY))

class EnergyScoreViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.ENERGY_SCORE_ACTIVITY))

class UserProfileViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<UserDataPoint>(store, metricDefinition(AppConstants.USER_PROFILE_ACTIVITY))

class BodyTemperatureViewModel(store: HealthDataStore) :
    BaseHealthDataViewModel<HealthDataPoint>(store, metricDefinition(AppConstants.BODY_TEMPERATURE_ACTIVITY))

