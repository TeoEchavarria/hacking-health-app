package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientHealthSummaryResponse
import com.samsung.android.health.sdk.sample.healthdiary.repository.PatientDataRepository
import com.samsung.android.health.sdk.sample.healthdiary.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for calendar biometric analysis.
 */
data class BiometricAnalysisUiState(
    val isLoading: Boolean = true,
    val healthSummary: PatientHealthSummaryResponse? = null,
    val patientName: String? = null,
    val role: String = "none",  // "caregiver", "patient", or "none"
    val error: String? = null,
    val lastUpdatedText: String = "Cargando..."
)

/**
 * ViewModel for Calendar screen biometric analysis.
 * 
 * Loads health summary based on user role:
 * - Caregiver: Fetches health summary for their primary patient
 * - Patient: Fetches their own health summary
 * - None: Shows empty state
 */
class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CalendarViewModel"
    }

    private val userRepository = UserRepository(application.applicationContext)
    private val patientDataRepository = PatientDataRepository(application.applicationContext)

    private val _biometricState = MutableStateFlow(BiometricAnalysisUiState())
    val biometricState: StateFlow<BiometricAnalysisUiState> = _biometricState.asStateFlow()

    init {
        loadBiometricData()
    }

    /**
     * Refresh biometric data.
     */
    fun refreshBiometricData() {
        loadBiometricData()
    }

    /**
     * Load biometric data based on user role.
     */
    private fun loadBiometricData() {
        viewModelScope.launch {
            _biometricState.value = BiometricAnalysisUiState(isLoading = true)
            
            // First, get user profile to determine role
            val profileResult = userRepository.getFullProfile()
            
            profileResult.fold(
                onSuccess = { profile ->
                    Log.i(TAG, "Profile loaded: role=${profile.role}")
                    
                    when (profile.role) {
                        "caregiver" -> {
                            // Get primary patient's health summary
                            val primaryPatientId = profile.getPrimaryPatientId()
                            if (primaryPatientId != null) {
                                loadPatientHealthSummary(primaryPatientId, profile.role)
                            } else {
                                _biometricState.value = BiometricAnalysisUiState(
                                    isLoading = false,
                                    role = profile.role,
                                    error = "No tienes pacientes vinculados"
                                )
                            }
                        }
                        "patient" -> {
                            // Get own health summary
                            loadPatientHealthSummary(profile.id, profile.role)
                        }
                        else -> {
                            // No role set - show own health data (user can view their own biometrics)
                            Log.i(TAG, "No role set, showing own health data for user ${profile.id}")
                            loadPatientHealthSummary(profile.id, profile.role ?: "none")
                        }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load profile: ${error.message}")
                    _biometricState.value = BiometricAnalysisUiState(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    /**
     * Load health summary for a specific patient.
     */
    private suspend fun loadPatientHealthSummary(patientId: String, role: String) {
        val result = patientDataRepository.getPatientHealthSummary(patientId)
        
        result.fold(
            onSuccess = { summary ->
                Log.i(TAG, "Health summary loaded for patient $patientId: dataAvailable=${summary.dataAvailable}")
                
                val lastUpdatedText = if (summary.lastSync != null) {
                    formatLastUpdated(summary.lastSync)
                } else {
                    "Sin sincronizar"
                }
                
                _biometricState.value = BiometricAnalysisUiState(
                    isLoading = false,
                    healthSummary = summary,
                    patientName = summary.patientName,
                    role = role,
                    error = null,
                    lastUpdatedText = lastUpdatedText
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to load health summary: ${error.message}")
                _biometricState.value = BiometricAnalysisUiState(
                    isLoading = false,
                    role = role,
                    error = error.message
                )
            }
        )
    }

    /**
     * Format last updated timestamp to human-readable text.
     */
    private fun formatLastUpdated(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestampMs
        val diffMinutes = diffMs / (1000 * 60)
        val diffHours = diffMs / (1000 * 60 * 60)
        val diffDays = diffMs / (1000 * 60 * 60 * 24)
        
        return when {
            diffMinutes < 1 -> "Justo ahora"
            diffMinutes < 60 -> "Hace $diffMinutes min"
            diffHours < 24 -> "Hace $diffHours h"
            diffDays < 7 -> "Hace $diffDays días"
            else -> "Hace más de una semana"
        }
    }
}
