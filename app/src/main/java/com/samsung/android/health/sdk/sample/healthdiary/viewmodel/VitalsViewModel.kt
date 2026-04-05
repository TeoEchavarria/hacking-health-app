package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientAlert
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairingEntity
import com.samsung.android.health.sdk.sample.healthdiary.model.*
import com.samsung.android.health.sdk.sample.healthdiary.repository.PatientDataRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Linked patient info for caregiver mode.
 */
data class LinkedPatient(
    val patientId: String,
    val patientName: String,
    val pairingId: String
)

/**
 * UI State for the new Vitals screen (Notifications & Monitoring).
 */
data class VitalsUiState(
    val currentAlert: HealthAlert? = null,
    val lastLocation: LocationData? = null,
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    
    // Caregiver mode fields
    val isCaregiverMode: Boolean = false,
    val linkedPatients: List<LinkedPatient> = emptyList(),
    val selectedPatient: LinkedPatient? = null,
    val patientAlerts: List<PatientAlert> = emptyList(),
    val errorMessage: String? = null,
    val lastRefreshTime: Long = 0
)

/**
 * ViewModel for the NEW Vitals screen.
 * 
 * Responsibilities:
 * - Manage health notifications
 * - Track GPS location
 * - Display day summary
 * - Show urgent alerts
 * - **Caregiver mode**: Fetch and display linked patient data
 * 
 * Note: Biometric data is now in BiometricsViewModel
 */
class VitalsViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "VitalsViewModel"
        private const val POLLING_INTERVAL_MS = 30_000L // 30 seconds
    }
    
    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()
    
    private val patientDataRepository = PatientDataRepository(context)
    private val pairingDao = AppDatabase.getDatabase(context).pairingDao()
    
    private var pollingJob: Job? = null
    
    init {
        loadInitialData()
        observeLinkedPatients()
    }
    
    /**
     * Observe linked patients from local database.
     * Automatically updates when pairings change.
     */
    private fun observeLinkedPatients() {
        viewModelScope.launch {
            val userRole = UserRole.getFromPreferences(context)
            val isCaregiver = userRole == UserRole.CAREGIVER
            
            _uiState.update { it.copy(isCaregiverMode = isCaregiver) }
            
            if (isCaregiver) {
                pairingDao.getCaregiverPatients().collect { pairings ->
                    val linkedPatients = pairings.mapNotNull { pairing ->
                        if (pairing.patientId != null && pairing.patientName != null) {
                            LinkedPatient(
                                patientId = pairing.patientId,
                                patientName = pairing.patientName,
                                pairingId = pairing.pairingId
                            )
                        } else null
                    }
                    
                    _uiState.update { state ->
                        state.copy(
                            linkedPatients = linkedPatients,
                            // Auto-select first patient if none selected
                            selectedPatient = state.selectedPatient 
                                ?: linkedPatients.firstOrNull()
                        )
                    }
                    
                    // Start polling if we have a selected patient
                    _uiState.value.selectedPatient?.let {
                        startPollingPatientData()
                    }
                    
                    Log.d(TAG, "Linked patients updated: ${linkedPatients.size} patients")
                }
            }
        }
    }
    
    /**
     * Select a patient to view (for caregivers with multiple linked patients).
     */
    fun selectPatient(patient: LinkedPatient) {
        _uiState.update { it.copy(selectedPatient = patient) }
        refreshPatientData()
    }
    
    /**
     * Start periodic polling for patient data.
     */
    private fun startPollingPatientData() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                refreshPatientData()
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Stop polling (called when leaving the screen).
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    /**
     * Manually refresh patient data.
     */
    fun refreshPatientData() {
        val patient = _uiState.value.selectedPatient ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Fetch alerts from API
                val alertsResult = patientDataRepository.getPatientAlerts(
                    patientId = patient.patientId,
                    limit = 20
                )
                
                alertsResult.fold(
                    onSuccess = { response ->
                        val mostUrgentAlert = response.alerts
                            .filter { it.severity == "urgent" || it.severity == "high" }
                            .maxByOrNull { it.createdAt }
                        
                        _uiState.update { state ->
                            state.copy(
                                patientAlerts = response.alerts,
                                currentAlert = mostUrgentAlert?.let { alert ->
                                    HealthAlert(
                                        title = alert.title,
                                        description = alert.body,
                                        timestamp = alert.createdAt,
                                        metricType = alert.type
                                    )
                                },
                                isLoading = false,
                                lastRefreshTime = System.currentTimeMillis()
                            )
                        }
                        Log.d(TAG, "Fetched ${response.alerts.size} alerts for patient ${patient.patientId}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to fetch patient alerts: ${error.message}")
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                errorMessage = error.message
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing patient data", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Load initial data: notifications, location, summary
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Generate mock notifications
                val mockNotifications = generateMockNotifications()
                
                // Mock location (Madrid, Spain)
                val mockLocation = LocationData(
                    latitude = 40.4168,
                    longitude = -3.7038,
                    address = "Madrid, España",
                    timestamp = System.currentTimeMillis() - 120_000, // 2 minutes ago
                    accuracy = 10f
                )
                
                // Mock urgent alert (simulating high blood pressure)
                val mockAlert = HealthAlert(
                    title = "Alerta: Presión Arterial Elevada",
                    description = "Detectada hace 15 min. Se recomienda reposo.",
                    timestamp = System.currentTimeMillis() - 900_000, // 15 min ago
                    metricType = "blood_pressure"
                )
                
                _uiState.update { state ->
                    state.copy(
                        currentAlert = mockAlert,
                        lastLocation = mockLocation,
                        notifications = mockNotifications,
                        isLoading = false
                    )
                }
                
                Log.d(TAG, "Initial data loaded successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * Generate mock notifications for testing
     */
    private fun generateMockNotifications(): List<Notification> {
        val now = System.currentTimeMillis()
        
        return listOf(
            Notification(
                id = UUID.randomUUID().toString(),
                type = NotificationType.MEDICATION,
                title = "Recordatorio de Medicación",
                message = "Es hora de tomar su Enalapril (10mg).",
                timestamp = now,
                isRead = false,
                priority = NotificationPriority.HIGH
            ),
            Notification(
                id = UUID.randomUUID().toString(),
                type = NotificationType.ACHIEVEMENT,
                title = "Objetivo de Pasos",
                message = "¡Felicidades! Has alcanzado el 50% de tu meta diaria.",
                timestamp = now - 3_600_000, // 1 hour ago
                isRead = false,
                priority = NotificationPriority.NORMAL
            ),
            Notification(
                id = UUID.randomUUID().toString(),
                type = NotificationType.WARNING,
                title = "Batería del Sensor Baja",
                message = "El dispositivo de monitoreo está al 15%. Favor cargar.",
                timestamp = now - 10_800_000, // 3 hours ago
                isRead = false,
                priority = NotificationPriority.HIGH
            ),
            Notification(
                id = UUID.randomUUID().toString(),
                type = NotificationType.APPOINTMENT,
                title = "Cita Médica Mañana",
                message = "Confirmación: Cita con Cardiología a las 10:30 AM.",
                timestamp = now - 21_600_000, // 6 hours ago
                isRead = false,
                priority = NotificationPriority.NORMAL
            ),
            Notification(
                id = UUID.randomUUID().toString(),
                type = NotificationType.REPORT,
                title = "Actualización de Informe",
                message = "Su resumen semanal de salud ya está disponible.",
                timestamp = now - 86_400_000, // Yesterday
                isRead = true,
                priority = NotificationPriority.LOW
            )
        )
    }
    
    /**
     * Dismiss the current alert
     */
    fun dismissAlert() {
        _uiState.update { it.copy(currentAlert = null) }
    }
    
    /**
     * Mark notification as read
     */
    fun markNotificationAsRead(notificationId: String) {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
            )
        }
    }
    
    /**
     * Refresh location data
     */
    fun refreshLocation() {
        viewModelScope.launch {
            try {
                // TODO: Implement actual GPS location fetch
                Log.d(TAG, "Location refresh requested")
                
                // For now, just update timestamp
                _uiState.update { state ->
                    state.lastLocation?.let { location ->
                        state.copy(
                            lastLocation = location.copy(
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } ?: state
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing location", e)
            }
        }
    }
    
    /**
     * Load more notifications (pagination)
     */
    fun loadMoreNotifications() {
        viewModelScope.launch {
            try {
                // TODO: Implement pagination from database
                Log.d(TAG, "Load more notifications requested")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more notifications", e)
            }
        }
    }
}
