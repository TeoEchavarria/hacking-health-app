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
import com.samsung.android.health.sdk.sample.healthdiary.repository.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    private val notificationRepository = NotificationRepository(context)
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
                // Fetch notifications from API
                val notificationsResult = notificationRepository.getNotifications()
                val notifications = notificationsResult.getOrElse { 
                    Log.w(TAG, "Failed to fetch notifications from API, using empty list: ${it.message}")
                    emptyList()
                }
                
                // Find most urgent notification as current alert
                val urgentNotification = notifications
                    .filter { it.priority == NotificationPriority.HIGH || it.priority == NotificationPriority.URGENT }
                    .filter { !it.isRead }
                    .maxByOrNull { it.timestamp }
                
                val currentAlert = urgentNotification?.let { notif ->
                    HealthAlert(
                        title = notif.title,
                        description = notif.message,
                        timestamp = notif.timestamp,
                        metricType = notif.type.name.lowercase()
                    )
                }
                
                // Mock location (Madrid, Spain) - TODO: Implement real location tracking
                val mockLocation = LocationData(
                    latitude = 40.4168,
                    longitude = -3.7038,
                    address = "Madrid, España",
                    timestamp = System.currentTimeMillis() - 120_000, // 2 minutes ago
                    accuracy = 10f
                )
                
                _uiState.update { state ->
                    state.copy(
                        currentAlert = currentAlert,
                        lastLocation = mockLocation,
                        notifications = notifications,
                        isLoading = false
                    )
                }
                
                Log.d(TAG, "Initial data loaded successfully with ${notifications.size} notifications from API")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * Dismiss the current alert
     */
    fun dismissAlert() {
        _uiState.update { it.copy(currentAlert = null) }
    }
    
    /**
     * Mark notification as read (updates both UI and API)
     */
    fun markNotificationAsRead(notificationId: String) {
        // Update UI immediately for responsiveness
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
        
        // Sync with API in background
        viewModelScope.launch {
            val result = notificationRepository.markAsRead(notificationId)
            if (result.isFailure) {
                Log.w(TAG, "Failed to sync notification read status to API: ${result.exceptionOrNull()?.message}")
                // Note: Not reverting UI since user action was intentional
            }
        }
    }
    
    /**
     * Refresh notifications from API
     */
    fun refreshNotifications() {
        viewModelScope.launch {
            try {
                val result = notificationRepository.getNotifications()
                result.onSuccess { notifications ->
                    _uiState.update { state ->
                        state.copy(notifications = notifications)
                    }
                    Log.d(TAG, "Notifications refreshed: ${notifications.size} notifications")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to refresh notifications: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing notifications", e)
            }
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
