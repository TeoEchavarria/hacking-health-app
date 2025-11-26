package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes

object OmhTypeMapper {
    /**
     * Maps DataType to endpoint method name for /sync/{method}
     */
    fun getEndpointMethod(dataType: DataType): String {
        return when (dataType) {
            DataTypes.STEPS -> "steps"
            DataTypes.HEART_RATE -> "heart-rate"
            DataTypes.SLEEP -> "sleep"
            DataTypes.BLOOD_OXYGEN -> "blood-oxygen"
            DataTypes.BODY_TEMPERATURE -> "body-temperature"
            DataTypes.SKIN_TEMPERATURE -> "skin-temperature"
            DataTypes.BLOOD_PRESSURE -> "blood-pressure"
            DataTypes.BLOOD_GLUCOSE -> "blood-glucose"
            DataTypes.BODY_COMPOSITION -> "body-composition"
            DataTypes.EXERCISE -> "exercise"
            DataTypes.EXERCISE_LOCATION -> "exercise-location"
            DataTypes.ACTIVITY_SUMMARY -> "activity-summary"
            DataTypes.FLOORS_CLIMBED -> "floors-climbed"
            DataTypes.WATER_INTAKE -> "water-intake"
            DataTypes.NUTRITION -> "nutrition"
            DataTypes.ENERGY_SCORE -> "energy-score"
            else -> dataType.name.lowercase().replace("_", "-")
        }
    }

    /**
     * Maps DataType to OMH schema name
     */
    fun getOmhSchemaName(dataType: DataType): String {
        return when (dataType) {
            DataTypes.STEPS -> "step-count"
            DataTypes.HEART_RATE -> "heart-rate"
            DataTypes.SLEEP -> "sleep-duration"
            DataTypes.BLOOD_OXYGEN -> "oxygen-saturation"
            DataTypes.BODY_TEMPERATURE -> "body-temperature"
            DataTypes.SKIN_TEMPERATURE -> "skin-temperature"
            DataTypes.BLOOD_PRESSURE -> "blood-pressure"
            DataTypes.BLOOD_GLUCOSE -> "blood-glucose"
            DataTypes.BODY_COMPOSITION -> "body-weight" // Can also be body-fat-percentage
            DataTypes.EXERCISE -> "physical-activity"
            DataTypes.EXERCISE_LOCATION -> "physical-activity"
            DataTypes.ACTIVITY_SUMMARY -> "physical-activity"
            DataTypes.FLOORS_CLIMBED -> "floors-climbed"
            DataTypes.WATER_INTAKE -> "fluid-intake"
            DataTypes.NUTRITION -> "nutrition"
            DataTypes.ENERGY_SCORE -> "energy-score"
            else -> dataType.name.lowercase().replace("_", "-")
        }
    }

    /**
     * Checks if a DataType is supported for OMH conversion
     */
    fun isSupported(dataType: DataType): Boolean {
        return when (dataType) {
            DataTypes.STEPS,
            DataTypes.HEART_RATE,
            DataTypes.SLEEP,
            DataTypes.BLOOD_OXYGEN,
            DataTypes.BODY_TEMPERATURE,
            DataTypes.SKIN_TEMPERATURE,
            DataTypes.BLOOD_PRESSURE,
            DataTypes.BLOOD_GLUCOSE,
            DataTypes.BODY_COMPOSITION,
            DataTypes.EXERCISE,
            DataTypes.EXERCISE_LOCATION,
            DataTypes.ACTIVITY_SUMMARY,
            DataTypes.FLOORS_CLIMBED,
            DataTypes.WATER_INTAKE,
            DataTypes.NUTRITION,
            DataTypes.ENERGY_SCORE -> true
            else -> false
        }
    }
}


