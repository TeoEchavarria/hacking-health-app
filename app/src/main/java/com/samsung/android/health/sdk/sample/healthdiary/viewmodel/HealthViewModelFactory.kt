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

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al crear ViewModel ${modelClass.simpleName}: ${e.message}", e)
        throw e
    } as T
}
