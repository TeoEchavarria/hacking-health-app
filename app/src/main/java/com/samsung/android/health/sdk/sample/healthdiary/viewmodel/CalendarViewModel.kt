package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientHealthSummaryResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.HeartRateSummary
import com.samsung.android.health.sdk.sample.healthdiary.api.models.StepsSummary
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SleepSummary
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.repository.PatientDataRepository
import com.samsung.android.health.sdk.sample.healthdiary.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    private val watchHealthRepository = WatchHealthIngestionRepository(application.applicationContext)

    private val _biometricState = MutableStateFlow(BiometricAnalysisUiState())
    val biometricState: StateFlow<BiometricAnalysisUiState> = _biometricState.asStateFlow()

    init {
        loadBiometricData()
        observeLocalHealthData()
    }

    /**
     * Observe local health data changes for real-time updates (patients only).
     */
    private fun observeLocalHealthData() {
        // Observe heart rate changes
        viewModelScope.launch {
            watchHealthRepository.getLatestHeartRateFlow().collect { hrEntity ->
                // Only refresh if current role is patient and data changed
                val currentState = _biometricState.value
                if (currentState.role == "patient" && hrEntity != null) {
                    Log.i(TAG, "Heart rate updated: ${hrEntity.bpm} BPM - refreshing display")
                    // Refresh the local data to update UI
                    val profile = userRepository.getFullProfile().getOrNull()
                    if (profile != null) {
                        loadLocalHealthSummary(profile.id, profile.role ?: "patient")
                    }
                }
            }
        }
        
        // Observe steps changes
        viewModelScope.launch {
            val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            watchHealthRepository.getTodayStepsFlow(today).collect { stepsEntity ->
                val currentState = _biometricState.value
                if (currentState.role == "patient" && stepsEntity != null) {
                    Log.i(TAG, "Steps updated: ${stepsEntity.steps} - refreshing display")
                    val profile = userRepository.getFullProfile().getOrNull()
                    if (profile != null) {
                        loadLocalHealthSummary(profile.id, profile.role ?: "patient")
                    }
                }
            }
        }
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
                            // Get own health summary from LOCAL database (real-time watch data)
                            loadLocalHealthSummary(profile.id, profile.role)
                        }
                        else -> {
                            // No role set - show own health data from LOCAL database (real-time watch data)
                            Log.i(TAG, "No role set, showing own health data from local DB for user ${profile.id}")
                            loadLocalHealthSummary(profile.id, profile.role ?: "none")
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
     * Load health summary from LOCAL database (for patients viewing their own data).
     * This provides real-time data directly from the watch without API round-trip.
     */
    private suspend fun loadLocalHealthSummary(patientId: String, role: String) {
        try {
            val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            Log.i(TAG, "📊 [LOCAL_DATA] Loading health summary for date: $today, role: $role, patientId: $patientId")
            
            // Read from local Room DB
            val latestHR = watchHealthRepository.getLatestHeartRate()
            val todaySteps = watchHealthRepository.getTodaySteps(today)
            val todaySleep = watchHealthRepository.getTodaySleep(today)
            val dailySummary = watchHealthRepository.getDailySummary(today)
            
            // Detailed diagnostic logging
            Log.i(TAG, "📊 [LOCAL_DATA] Query Results:")
            Log.i(TAG, "📊 [LOCAL_DATA]   - latestHR: ${latestHR?.bpm ?: "NULL"} bpm (ts: ${latestHR?.measurementTimestamp ?: "N/A"})")
            Log.i(TAG, "📊 [LOCAL_DATA]   - todaySteps: ${todaySteps ?: "NULL"}")
            Log.i(TAG, "📊 [LOCAL_DATA]   - todaySleep: ${todaySleep ?: "NULL"} min")
            Log.i(TAG, "📊 [LOCAL_DATA]   - dailySummary: ${if (dailySummary != null) "EXISTS (avgHR=${dailySummary.avgHeartRate}, steps=${dailySummary.steps})" else "NULL"}")
            
            // Convert to PatientHealthSummaryResponse format
            val summary = PatientHealthSummaryResponse(
                patientId = patientId,
                heartRate = if (latestHR != null) {
                    // Use aggregated values from daily summary if available, otherwise use latest reading
                    val avgHr = dailySummary?.avgHeartRate ?: latestHR.bpm
                    val minHr = dailySummary?.minHeartRate ?: latestHR.bpm
                    val maxHr = dailySummary?.maxHeartRate ?: latestHR.bpm
                    Log.i(TAG, "📊 [LOCAL_DATA] Creating HeartRateSummary: avg=$avgHr, min=$minHr, max=$maxHr, available=true")
                    
                    HeartRateSummary(
                        available = true,
                        average = avgHr,
                        min = minHr,
                        max = maxHr,
                        lastReading = latestHR.bpm,
                        lastReadingTime = latestHR.measurementTimestamp
                    )
                } else {
                    Log.w(TAG, "📊 [LOCAL_DATA] No heart rate data found - HeartRateSummary will be unavailable")
                    HeartRateSummary()
                },
                steps = if (todaySteps != null) {
                    StepsSummary(
                        available = true,
                        total = todaySteps,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    StepsSummary()
                },
                sleep = if (todaySleep != null) {
                    SleepSummary(
                        available = true,
                        totalMinutes = todaySleep,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    SleepSummary()
                },
                dataAvailable = latestHR != null || todaySteps != null || todaySleep != null,
                lastSync = latestHR?.measurementTimestamp ?: System.currentTimeMillis()
            )
            
            val lastUpdatedText = if (summary.lastSync != null) {
                formatLastUpdated(summary.lastSync)
            } else {
                "Sin sincronizar"
            }
            
            Log.i(TAG, "📊 [LOCAL_DATA] Final summary - dataAvailable=${summary.dataAvailable}, hasHeartRate=${summary.hasHeartRate()}, hasSteps=${summary.hasSteps()}, hasSleep=${summary.hasSleep()}")
            Log.i(TAG, "📊 [LOCAL_DATA] Updating UI state with role=$role, lastUpdated=$lastUpdatedText")
            
            _biometricState.value = BiometricAnalysisUiState(
                isLoading = false,
                healthSummary = summary,
                patientName = null,
                role = role,
                error = null,
                lastUpdatedText = lastUpdatedText
            )
        } catch (e: Exception) {
            Log.e(TAG, "📊 [LOCAL_DATA] ERROR loading local health summary", e)
            _biometricState.value = BiometricAnalysisUiState(
                isLoading = false,
                role = role,
                error = "Error al cargar datos locales: ${e.message}"
            )
        }
    }

    /**
     * Load health summary for a specific patient from API (for caregivers viewing patient data).
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
