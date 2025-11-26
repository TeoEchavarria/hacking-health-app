package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.OmhSyncResponse
import com.samsung.android.health.sdk.sample.healthdiary.repository.OmhSyncRepository
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

sealed class OmhSyncState {
    object Idle : OmhSyncState()
    object Syncing : OmhSyncState()
    data class Success(val response: OmhSyncResponse) : OmhSyncState()
    data class Error(val message: String) : OmhSyncState()
}

class OmhSyncViewModel(
    private val omhSyncRepository: OmhSyncRepository
) : ViewModel() {
    
    private val _syncState = MutableStateFlow<OmhSyncState>(OmhSyncState.Idle)
    val syncState: StateFlow<OmhSyncState> = _syncState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _syncState.emit(OmhSyncState.Error(exception.message ?: "Unknown error"))
            _errorMessage.emit(exception.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Syncs a specific data type
     */
    fun syncDataType(
        dataType: DataType,
        startDateTime: LocalDateTime? = null,
        endDateTime: LocalDateTime? = null
    ) {
        viewModelScope.launch(exceptionHandler) {
            _syncState.emit(OmhSyncState.Syncing)
            _errorMessage.emit(null)
            
            val result = omhSyncRepository.syncDataType(dataType, startDateTime, endDateTime)
            
            result.fold(
                onSuccess = { response ->
                    _syncState.emit(OmhSyncState.Success(response))
                },
                onFailure = { error ->
                    _syncState.emit(OmhSyncState.Error(error.message ?: "Sync failed"))
                    _errorMessage.emit(error.message ?: "Sync failed")
                }
            )
        }
    }
    
    /**
     * Syncs all supported data types
     */
    fun syncAll(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        viewModelScope.launch(exceptionHandler) {
            _syncState.emit(OmhSyncState.Syncing)
            _errorMessage.emit(null)
            
            val supportedTypes = listOf(
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
                DataTypes.FLOORS_CLIMBED,
                DataTypes.WATER_INTAKE,
                DataTypes.NUTRITION,
                DataTypes.ENERGY_SCORE
            )
            
            var successCount = 0
            var failureCount = 0
            
            supportedTypes.forEach { dataType ->
                val result = omhSyncRepository.syncDataType(dataType, startDateTime, endDateTime)
                if (result.isSuccess) {
                    successCount++
                } else {
                    failureCount++
                }
            }
            
            if (successCount > 0) {
                _syncState.emit(OmhSyncState.Success(
                    OmhSyncResponse(
                        success = true,
                        message = "Synced $successCount types successfully${if (failureCount > 0) ", $failureCount failed" else ""}"
                    )
                ))
            } else {
                _syncState.emit(OmhSyncState.Error("Failed to sync any data types"))
                _errorMessage.emit("Failed to sync any data types")
            }
        }
    }
    
    // Convenience methods for specific data types
    fun syncSteps(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.STEPS, startDateTime, endDateTime)
    }
    
    fun syncHeartRate(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.HEART_RATE, startDateTime, endDateTime)
    }
    
    fun syncSleep(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.SLEEP, startDateTime, endDateTime)
    }
    
    fun syncBloodOxygen(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.BLOOD_OXYGEN, startDateTime, endDateTime)
    }
    
    fun syncBodyTemperature(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.BODY_TEMPERATURE, startDateTime, endDateTime)
    }
    
    fun syncSkinTemperature(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.SKIN_TEMPERATURE, startDateTime, endDateTime)
    }
    
    fun syncBloodPressure(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.BLOOD_PRESSURE, startDateTime, endDateTime)
    }
    
    fun syncBloodGlucose(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.BLOOD_GLUCOSE, startDateTime, endDateTime)
    }
    
    fun syncBodyComposition(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.BODY_COMPOSITION, startDateTime, endDateTime)
    }
    
    fun syncExercise(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.EXERCISE, startDateTime, endDateTime)
    }
    
    fun syncFloorsClimbed(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.FLOORS_CLIMBED, startDateTime, endDateTime)
    }
    
    fun syncWaterIntake(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.WATER_INTAKE, startDateTime, endDateTime)
    }
    
    fun syncNutrition(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.NUTRITION, startDateTime, endDateTime)
    }
    
    fun syncEnergyScore(startDateTime: LocalDateTime? = null, endDateTime: LocalDateTime? = null) {
        syncDataType(DataTypes.ENERGY_SCORE, startDateTime, endDateTime)
    }
    
    fun resetState() {
        _syncState.value = OmhSyncState.Idle
        _errorMessage.value = null
    }
}


