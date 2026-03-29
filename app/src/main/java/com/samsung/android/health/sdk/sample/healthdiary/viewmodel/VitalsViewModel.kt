package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * UI State for the new Vitals screen (Notifications & Monitoring).
 */
data class VitalsUiState(
    val currentAlert: HealthAlert? = null,
    val lastLocation: LocationData? = null,
    val notifications: List<Notification> = emptyList(),
    val daySummary: DaySummary? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel for the NEW Vitals screen.
 * 
 * Responsibilities:
 * - Manage health notifications
 * - Track GPS location
 * - Display day summary
 * - Show urgent alerts
 * 
 * Note: Biometric data is now in BiometricsViewModel
 */
class VitalsViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "VitalsViewModel"
    }
    
    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
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
                
                // Mock day summary
                val mockSummary = DaySummary(
                    steps = 2450,
                    sleepHours = 7.7f,
                    description = "Estado general estable. La tendencia de presión sugiere evitar esfuerzos intensos en las próximas 4h."
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
                        daySummary = mockSummary,
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
