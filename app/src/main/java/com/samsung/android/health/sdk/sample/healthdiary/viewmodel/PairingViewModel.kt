package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CreatePairingCodeResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ValidatePairingCodeResponse
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairingEntity
import com.samsung.android.health.sdk.sample.healthdiary.repository.PairingRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for family pairing functionality.
 * 
 * Handles both caregiver and patient flows:
 * - Patient: Generate code, poll for status
 * - Caregiver: Enter code, validate
 */
class PairingViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "PairingViewModel"
        private const val POLLING_INTERVAL_MS = 3000L // Poll every 3 seconds
    }
    
    private val repository = PairingRepository(context)
    private val pairingDao = AppDatabase.getDatabase(context).pairingDao()
    
    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = _uiState
    
    private val _codeInput = MutableStateFlow("")
    val codeInput: StateFlow<String> = _codeInput
    
    private var pollingJob: Job? = null
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            Log.e(TAG, "Error in PairingViewModel", exception)
            _uiState.emit(PairingUiState.Error(exception.message ?: "Error desconocido"))
        }
    }
    
    // ==========================================================================
    // Patient Flow (Generate and Show Code)
    // ==========================================================================
    
    /**
     * Generate a new pairing code for the patient.
     * Code is valid for 10 minutes.
     */
    fun generatePairingCode() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.emit(PairingUiState.Loading)
            
            val result = repository.createPairingCode()
            
            result.fold(
                onSuccess = { response ->
                    // Save to local database
                    savePairingToDatabase(response, userRole = "patient")
                    
                    // Update UI state
                    _uiState.emit(
                        PairingUiState.CodeGenerated(
                            code = response.code,
                            pairingId = response.pairingId,
                            expiresAt = response.expiresAt
                        )
                    )
                    
                    // Start polling for status
                    startPollingPairingStatus(response.pairingId)
                },
                onFailure = { exception ->
                    _uiState.emit(PairingUiState.Error(exception.message ?: "Error al generar código"))
                }
            )
        }
    }
    
    /**
     * Start polling the backend to check if caregiver has used the code.
     */
    private fun startPollingPairingStatus(pairingId: String) {
        // Cancel any existing polling
        pollingJob?.cancel()
        
        pollingJob = viewModelScope.launch(exceptionHandler) {
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                
                val result = repository.checkPairingStatus(pairingId)
                
                result.fold(
                    onSuccess = { status ->
                        if (status.linked && status.caregiverId != null) {
                            // Pairing was successful!
                            Log.i(TAG, "Pairing completed with caregiver: ${status.caregiverName}")
                            
                            // Update database
                            pairingDao.activatePairing(
                                pairingId = pairingId,
                                caregiverId = status.caregiverId,
                                caregiverName = status.caregiverName ?: "Cuidador"
                            )
                            
                            // Update UI
                            _uiState.emit(
                                PairingUiState.PairingSuccess(
                                    pairingId = pairingId,
                                    linkedUserId = status.caregiverId,
                                    linkedUserName = status.caregiverName ?: "Cuidador"
                                )
                            )
                            
                            // Stop polling
                            pollingJob?.cancel()
                        } else if (status.status == "expired") {
                            _uiState.emit(PairingUiState.Error("El código ha expirado"))
                            pollingJob?.cancel()
                        } else {
                            // Continue polling - status is still pending
                        }
                    },
                    onFailure = { exception ->
                        // Log error but continue polling (network might be temporarily down)
                        Log.w(TAG, "Polling error: ${exception.message}")
                    }
                )
            }
        }
    }
    
    /**
     * Stop polling (call when screen is destroyed or user navigates away).
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    // ==========================================================================
    // Caregiver Flow (Enter and Validate Code)
    // ==========================================================================
    
    /**
     * Update the code input as user types.
     * Auto-validates when 6 digits are entered.
     */
    fun updateCodeInput(newCode: String) {
        // Only allow digits
        val digitsOnly = newCode.filter { it.isDigit() }.take(6)
        _codeInput.value = digitsOnly
        
        // Auto-validate when 6 digits entered
        if (digitsOnly.length == 6) {
            validatePairingCode(digitsOnly)
        }
    }
    
    /**
     * Delete last digit from code input.
     */
    fun deleteLastDigit() {
        val current = _codeInput.value
        if (current.isNotEmpty()) {
            _codeInput.value = current.dropLast(1)
        }
    }
    
    /**
     * Validate the pairing code entered by caregiver.
     */
    fun validatePairingCode(code: String = _codeInput.value) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.emit(PairingUiState.CodeValidating(code))
            
            val result = repository.validatePairingCode(code)
            
            result.fold(
                onSuccess = { response ->
                    if (response.success && response.patientId != null) {
                        // Save to local database
                        saveCaregiverPairingToDatabase(response)
                        
                        // Update UI
                        _uiState.emit(
                            PairingUiState.PairingSuccess(
                                pairingId = response.pairingId ?: "",
                                linkedUserId = response.patientId,
                                linkedUserName = response.patientName ?: "Persona a cuidar"
                            )
                        )
                    } else {
                        _uiState.emit(
                            PairingUiState.Error(response.error ?: "Código inválido o expirado")
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.emit(
                        PairingUiState.Error(exception.message ?: "Error al validar código")
                    )
                }
            )
        }
    }
    
    /**
     * Refresh/regenerate the pairing code.
     */
    fun refreshCode() {
        generatePairingCode()
    }
    
    /**
     * Reset state to idle.
     */
    fun resetState() {
        stopPolling()
        _uiState.value = PairingUiState.Idle
        _codeInput.value = ""
    }
    
    // ==========================================================================
    // Database Helpers
    // ==========================================================================
    
    private suspend fun savePairingToDatabase(response: CreatePairingCodeResponse, userRole: String) {
        val entity = PairingEntity(
            pairingId = response.pairingId,
            code = response.code,
            status = "pending",
            userRole = userRole,
            createdAt = response.createdAt,
            expiresAt = response.expiresAt
        )
        pairingDao.insert(entity)
    }
    
    private suspend fun saveCaregiverPairingToDatabase(response: ValidatePairingCodeResponse) {
        if (response.pairingId != null && response.patientId != null) {
            val entity = PairingEntity(
                pairingId = response.pairingId,
                patientId = response.patientId,
                patientName = response.patientName,
                status = "active",
                userRole = "caregiver",
                createdAt = System.currentTimeMillis(),
                activatedAt = System.currentTimeMillis()
            )
            pairingDao.insert(entity)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

/**
 * UI state for pairing screens.
 */
sealed class PairingUiState {
    object Idle : PairingUiState()
    object Loading : PairingUiState()
    
    data class CodeGenerated(
        val code: String,
        val pairingId: String,
        val expiresAt: Long
    ) : PairingUiState()
    
    data class CodeValidating(
        val code: String
    ) : PairingUiState()
    
    data class PairingSuccess(
        val pairingId: String,
        val linkedUserId: String,
        val linkedUserName: String
    ) : PairingUiState()
    
    data class Error(
        val message: String
    ) : PairingUiState()
}
