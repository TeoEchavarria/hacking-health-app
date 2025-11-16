/*
 * Copyright (C) 2024 Samsung Electronics Co., Ltd. All rights reserved
 */
package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore

class HealthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    companion object {
        private const val TAG = "[HTK]HealthViewModelFactory"
    }
    
    private fun getHealthDataStore(): HealthDataStore {
        return try {
            Log.d(TAG, "Inicializando HealthDataStore...")
            val store = HealthDataService.getStore(context)
            Log.i(TAG, "HealthDataStore inicializado correctamente")
            store
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar HealthDataStore: ${e.message}", e)
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            throw RuntimeException("Error al inicializar Samsung Health SDK: ${e.message}. " +
                    "Asegúrate de que Samsung Health esté instalada y actualizada.", e)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = try {
        when (modelClass) {
            HealthMainViewModel::class.java -> {
                Log.d(TAG, "Creando HealthMainViewModel")
                HealthMainViewModel(getHealthDataStore())
            }

            StepViewModel::class.java -> {
                Log.d(TAG, "Creando StepViewModel")
                StepViewModel(getHealthDataStore())
            }

            NutritionViewModel::class.java -> {
                Log.d(TAG, "Creando NutritionViewModel")
                NutritionViewModel(getHealthDataStore())
            }

            HeartRateViewModel::class.java -> {
                Log.d(TAG, "Creando HeartRateViewModel")
                HeartRateViewModel(getHealthDataStore())
            }

            SleepViewModel::class.java -> {
                Log.d(TAG, "Creando SleepViewModel")
                SleepViewModel(getHealthDataStore())
            }

            ChooseFoodViewModel::class.java -> {
                Log.d(TAG, "Creando ChooseFoodViewModel")
                ChooseFoodViewModel(getHealthDataStore())
            }

            UpdateFoodViewModel::class.java -> {
                Log.d(TAG, "Creando UpdateFoodViewModel")
                UpdateFoodViewModel(getHealthDataStore())
            }

            ExerciseViewModel::class.java -> {
                Log.d(TAG, "Creando ExerciseViewModel")
                ExerciseViewModel(getHealthDataStore())
            }

            ExerciseLocationViewModel::class.java -> {
                Log.d(TAG, "Creando ExerciseLocationViewModel")
                ExerciseLocationViewModel(getHealthDataStore())
            }

            SkinTemperatureDetailViewModel::class.java -> {
                Log.d(TAG, "Creando SkinTemperatureDetailViewModel")
                SkinTemperatureDetailViewModel(getHealthDataStore())
            }

            BloodOxygenDetailViewModel::class.java -> {
                Log.d(TAG, "Creando BloodOxygenDetailViewModel")
                BloodOxygenDetailViewModel(getHealthDataStore())
            }

            ActivitySummaryViewModel::class.java -> {
                Log.d(TAG, "Creando ActivitySummaryViewModel")
                ActivitySummaryViewModel(getHealthDataStore())
            }

            FloorsClimbedViewModel::class.java -> {
                Log.d(TAG, "Creando FloorsClimbedViewModel")
                FloorsClimbedViewModel(getHealthDataStore())
            }

            BloodGlucoseViewModel::class.java -> {
                Log.d(TAG, "Creando BloodGlucoseViewModel")
                BloodGlucoseViewModel(getHealthDataStore())
            }

            BloodPressureDetailViewModel::class.java -> {
                Log.d(TAG, "Creando BloodPressureDetailViewModel")
                BloodPressureDetailViewModel(getHealthDataStore())
            }

            BodyCompositionViewModel::class.java -> {
                Log.d(TAG, "Creando BodyCompositionViewModel")
                BodyCompositionViewModel(getHealthDataStore())
            }

            SleepGoalViewModel::class.java -> {
                Log.d(TAG, "Creando SleepGoalViewModel")
                SleepGoalViewModel(getHealthDataStore())
            }

            StepsGoalViewModel::class.java -> {
                Log.d(TAG, "Creando StepsGoalViewModel")
                StepsGoalViewModel(getHealthDataStore())
            }

            ActiveCaloriesGoalViewModel::class.java -> {
                Log.d(TAG, "Creando ActiveCaloriesGoalViewModel")
                ActiveCaloriesGoalViewModel(getHealthDataStore())
            }

            ActiveTimeGoalViewModel::class.java -> {
                Log.d(TAG, "Creando ActiveTimeGoalViewModel")
                ActiveTimeGoalViewModel(getHealthDataStore())
            }

            WaterIntakeViewModel::class.java -> {
                Log.d(TAG, "Creando WaterIntakeViewModel")
                WaterIntakeViewModel(getHealthDataStore())
            }

            WaterIntakeGoalViewModel::class.java -> {
                Log.d(TAG, "Creando WaterIntakeGoalViewModel")
                WaterIntakeGoalViewModel(getHealthDataStore())
            }

            NutritionGoalViewModel::class.java -> {
                Log.d(TAG, "Creando NutritionGoalViewModel")
                NutritionGoalViewModel(getHealthDataStore())
            }

            EnergyScoreViewModel::class.java -> {
                Log.d(TAG, "Creando EnergyScoreViewModel")
                EnergyScoreViewModel(getHealthDataStore())
            }

            UserProfileViewModel::class.java -> {
                Log.d(TAG, "Creando UserProfileViewModel")
                UserProfileViewModel(getHealthDataStore())
            }

            BodyTemperatureViewModel::class.java -> {
                Log.d(TAG, "Creando BodyTemperatureViewModel")
                BodyTemperatureViewModel(getHealthDataStore())
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al crear ViewModel ${modelClass.simpleName}: ${e.message}", e)
        throw e
    } as T
}
