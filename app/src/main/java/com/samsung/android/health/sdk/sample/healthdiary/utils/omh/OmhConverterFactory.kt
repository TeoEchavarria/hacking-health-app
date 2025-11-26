package com.samsung.android.health.sdk.sample.healthdiary.utils.omh

import com.samsung.android.health.sdk.sample.healthdiary.api.models.omh.OmhDataPoint
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes

object OmhConverterFactory {
    /**
     * Gets the appropriate converter for a given DataType
     */
    @Suppress("UNCHECKED_CAST")
    fun getConverter(dataType: DataType): OmhConverter<out OmhDataPoint>? {
        return when (dataType) {
            DataTypes.STEPS -> StepOmhConverter()
            DataTypes.HEART_RATE -> HeartRateOmhConverter()
            DataTypes.SLEEP -> SleepOmhConverter()
            DataTypes.BLOOD_OXYGEN -> BloodOxygenOmhConverter()
            DataTypes.BODY_TEMPERATURE -> TemperatureOmhConverter()
            DataTypes.SKIN_TEMPERATURE -> SkinTemperatureOmhConverter()
            DataTypes.BLOOD_PRESSURE -> BloodPressureOmhConverter()
            DataTypes.BLOOD_GLUCOSE -> BloodGlucoseOmhConverter()
            DataTypes.BODY_COMPOSITION -> BodyCompositionOmhConverter() // Returns body weight
            DataTypes.EXERCISE -> ExerciseOmhConverter()
            DataTypes.EXERCISE_LOCATION -> ExerciseOmhConverter()
            DataTypes.ACTIVITY_SUMMARY -> ExerciseOmhConverter()
            DataTypes.FLOORS_CLIMBED -> FloorsClimbedOmhConverter()
            DataTypes.WATER_INTAKE -> WaterIntakeOmhConverter()
            DataTypes.NUTRITION -> NutritionOmhConverter()
            DataTypes.ENERGY_SCORE -> EnergyScoreOmhConverter()
            else -> null
        }
    }

    /**
     * Gets converter for body fat percentage (special case of body composition)
     */
    fun getBodyFatConverter(): OmhConverter<out OmhDataPoint> {
        return BodyFatPercentageOmhConverter()
    }
}


