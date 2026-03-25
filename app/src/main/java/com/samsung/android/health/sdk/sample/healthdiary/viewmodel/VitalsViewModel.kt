package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Health alert status for biometric readings.
 */
enum class HealthAlertStatus {
    OPTIMAL,   // Green - Normal values
    WARNING,   // Amber - Requires attention
    CRITICAL   // Red - Urgent, requires immediate attention
}

/**
 * A biometric reading with its status.
 */
data class BiometricReading(
    val value: String,
    val status: HealthAlertStatus,
    val statusLabel: String,
    val secondaryText: String,
    val timestamp: Long? = null,
    val isAvailable: Boolean = true
)

/**
 * Health alert for urgent conditions.
 */
data class HealthAlert(
    val title: String,
    val description: String,
    val timestamp: Long,
    val metricType: String
)

/**
 * Health tip for user guidance.
 */
data class HealthTip(
    val title: String,
    val subtitle: String,
    val description: String,
    val actionText: String
)

/**
 * Day summary data.
 */
data class DaySummary(
    val steps: Int?,
    val sleepHours: Float?,
    val description: String
)

/**
 * UI State for the Vitals screen.
 */
data class VitalsUiState(
    // Alert (shown at top if critical)
    val currentAlert: HealthAlert? = null,
    
    // Health tip
    val healthTip: HealthTip = HealthTip(
        title = "Consejo de Salud",
        subtitle = "Sugerencia personalizada para hoy",
        description = "Realizar 5 minutos de respiración guiada puede ayudar a normalizar su presión arterial actual y reducir el estrés acumulado.",
        actionText = "Iniciar Respiración Guiada"
    ),
    
    // Biometric readings
    val spO2: BiometricReading = BiometricReading(
        value = "--",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "Sin lectura",
        isAvailable = false
    ),
    val bloodPressure: BiometricReading = BiometricReading(
        value = "--/--",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "Sin lectura",
        isAvailable = false
    ),
    val temperature: BiometricReading = BiometricReading(
        value = "--°C",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "Sin lectura",
        isAvailable = false
    ),
    
    // Day summary
    val daySummary: DaySummary = DaySummary(
        steps = null,
        sleepHours = null,
        description = "Estado general estable."
    ),
    
    // Last update time
    val lastUpdateTime: String = "Sin actualización",
    
    // Loading state
    val isLoading: Boolean = true
)

/**
 * ViewModel for the Vitals screen.
 * 
 * Provides real-time biometric data from the watch with health status evaluation.
 */
class VitalsViewModel(private val context: Context) : ViewModel() {
    
    private val healthRepository = WatchHealthIngestionRepository(context)
    
    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    init {
        observeHealthData()
    }
    
    private fun observeHealthData() {
        val today = dateFormat.format(Date())
        
        viewModelScope.launch {
            // Combine flows for steps, sleep, and heart rate
            combine(
                healthRepository.getTodayStepsFlow(today),
                healthRepository.getTodaySleepFlow(today),
                healthRepository.getLatestHeartRateFlow()
            ) { steps, sleep, heartRate ->
                Triple(steps, sleep, heartRate)
            }.collect { (steps, sleep, heartRate) ->
                
                val stepsValue = steps?.steps
                val sleepMinutes = sleep?.sleepMinutes
                val sleepHours = sleepMinutes?.let { it / 60f }
                
                // For now, we don't have SpO2, BP, Temperature from watch
                // These would come from Samsung Health SDK or additional sensors
                // Using placeholder logic that can be extended
                
                // Generate health tip based on current state
                val tip = generateHealthTip()
                
                // Generate day summary description
                val summaryDesc = generateSummaryDescription(stepsValue, sleepHours)
                
                _uiState.update { state ->
                    state.copy(
                        daySummary = DaySummary(
                            steps = stepsValue,
                            sleepHours = sleepHours,
                            description = summaryDesc
                        ),
                        healthTip = tip,
                        lastUpdateTime = "Hace ${getMinutesSinceLastUpdate()} min",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Update SpO2 reading (would be called when data becomes available).
     */
    fun updateSpO2(value: Int) {
        val status = when {
            value >= 96 -> HealthAlertStatus.OPTIMAL
            value in 93..95 -> HealthAlertStatus.WARNING
            else -> HealthAlertStatus.CRITICAL
        }
        
        val statusLabel = when (status) {
            HealthAlertStatus.OPTIMAL -> "ÓPTIMO"
            HealthAlertStatus.WARNING -> "AVISO"
            HealthAlertStatus.CRITICAL -> "BAJO"
        }
        
        val secondaryText = when (status) {
            HealthAlertStatus.OPTIMAL -> "Estable"
            HealthAlertStatus.WARNING -> "Monitorear"
            HealthAlertStatus.CRITICAL -> "Urgente"
        }
        
        _uiState.update { state ->
            state.copy(
                spO2 = BiometricReading(
                    value = "$value%",
                    status = status,
                    statusLabel = statusLabel,
                    secondaryText = secondaryText,
                    timestamp = System.currentTimeMillis(),
                    isAvailable = true
                )
            )
        }
        
        checkForAlerts()
    }
    
    /**
     * Update blood pressure reading.
     */
    fun updateBloodPressure(systolic: Int, diastolic: Int) {
        val status = when {
            systolic < 120 && diastolic < 80 -> HealthAlertStatus.OPTIMAL
            systolic in 120..139 || diastolic in 80..89 -> HealthAlertStatus.WARNING
            else -> HealthAlertStatus.CRITICAL
        }
        
        val statusLabel = when (status) {
            HealthAlertStatus.OPTIMAL -> "NORMAL"
            HealthAlertStatus.WARNING -> "ELEVADA"
            HealthAlertStatus.CRITICAL -> "ALTA"
        }
        
        val secondaryText = when (status) {
            HealthAlertStatus.OPTIMAL -> "Estable"
            HealthAlertStatus.WARNING -> "Monitorear"
            HealthAlertStatus.CRITICAL -> "Urgente"
        }
        
        _uiState.update { state ->
            state.copy(
                bloodPressure = BiometricReading(
                    value = "$systolic/$diastolic",
                    status = status,
                    statusLabel = statusLabel,
                    secondaryText = secondaryText,
                    timestamp = System.currentTimeMillis(),
                    isAvailable = true
                )
            )
        }
        
        checkForAlerts()
    }
    
    /**
     * Update temperature reading.
     */
    fun updateTemperature(tempCelsius: Float) {
        val status = when {
            tempCelsius in 36.1f..37.2f -> HealthAlertStatus.OPTIMAL
            tempCelsius in 37.3f..37.9f -> HealthAlertStatus.WARNING
            else -> HealthAlertStatus.CRITICAL
        }
        
        val statusLabel = when (status) {
            HealthAlertStatus.OPTIMAL -> "NORMAL"
            HealthAlertStatus.WARNING -> "ELEVADA"
            HealthAlertStatus.CRITICAL -> "FIEBRE"
        }
        
        val secondaryText = when (status) {
            HealthAlertStatus.OPTIMAL -> "Estable"
            HealthAlertStatus.WARNING -> "Monitorear"
            HealthAlertStatus.CRITICAL -> "Urgente"
        }
        
        _uiState.update { state ->
            state.copy(
                temperature = BiometricReading(
                    value = String.format("%.1f°C", tempCelsius),
                    status = status,
                    statusLabel = statusLabel,
                    secondaryText = secondaryText,
                    timestamp = System.currentTimeMillis(),
                    isAvailable = true
                )
            )
        }
        
        checkForAlerts()
    }
    
    /**
     * Check for critical conditions and generate alerts.
     */
    private fun checkForAlerts() {
        val state = _uiState.value
        
        // Check blood pressure first (most urgent)
        if (state.bloodPressure.status == HealthAlertStatus.CRITICAL && state.bloodPressure.isAvailable) {
            _uiState.update {
                it.copy(
                    currentAlert = HealthAlert(
                        title = "Alerta: Presión Arterial Elevada",
                        description = "Detectada hace ${getMinutesSinceLastUpdate()} min. Se recomienda reposo.",
                        timestamp = System.currentTimeMillis(),
                        metricType = "blood_pressure"
                    ),
                    healthTip = HealthTip(
                        title = "Consejo de Salud",
                        subtitle = "Sugerencia personalizada para hoy",
                        description = "Realizar 5 minutos de respiración guiada puede ayudar a normalizar su presión arterial actual y reducir el estrés acumulado.",
                        actionText = "Iniciar Respiración Guiada"
                    )
                )
            }
            return
        }
        
        // Check SpO2
        if (state.spO2.status == HealthAlertStatus.CRITICAL && state.spO2.isAvailable) {
            _uiState.update {
                it.copy(
                    currentAlert = HealthAlert(
                        title = "Alerta: Nivel de Oxígeno Bajo",
                        description = "SpO2 por debajo del rango normal. Busque atención médica.",
                        timestamp = System.currentTimeMillis(),
                        metricType = "spo2"
                    )
                )
            }
            return
        }
        
        // Check temperature
        if (state.temperature.status == HealthAlertStatus.CRITICAL && state.temperature.isAvailable) {
            _uiState.update {
                it.copy(
                    currentAlert = HealthAlert(
                        title = "Alerta: Temperatura Elevada",
                        description = "Fiebre detectada. Se recomienda descanso y monitoreo.",
                        timestamp = System.currentTimeMillis(),
                        metricType = "temperature"
                    )
                )
            }
            return
        }
        
        // No critical alerts
        _uiState.update { it.copy(currentAlert = null) }
    }
    
    private fun generateHealthTip(): HealthTip {
        val state = _uiState.value
        
        return when {
            state.bloodPressure.status == HealthAlertStatus.CRITICAL ||
            state.bloodPressure.status == HealthAlertStatus.WARNING -> {
                HealthTip(
                    title = "Consejo de Salud",
                    subtitle = "Sugerencia personalizada para hoy",
                    description = "Realizar 5 minutos de respiración guiada puede ayudar a normalizar su presión arterial actual y reducir el estrés acumulado.",
                    actionText = "Iniciar Respiración Guiada"
                )
            }
            state.spO2.status == HealthAlertStatus.WARNING -> {
                HealthTip(
                    title = "Consejo de Salud",
                    subtitle = "Mejora tu oxigenación",
                    description = "Realiza ejercicios de respiración profunda y asegúrate de estar en un ambiente bien ventilado.",
                    actionText = "Ver Ejercicios"
                )
            }
            else -> {
                HealthTip(
                    title = "Consejo de Salud",
                    subtitle = "Mantén tu bienestar",
                    description = "Tus signos vitales están estables. Continúa con tu rutina saludable y mantén la hidratación.",
                    actionText = "Ver Más Consejos"
                )
            }
        }
    }
    
    private fun generateSummaryDescription(steps: Int?, sleepHours: Float?): String {
        val state = _uiState.value
        
        val hasWarning = state.bloodPressure.status != HealthAlertStatus.OPTIMAL ||
                        state.spO2.status != HealthAlertStatus.OPTIMAL ||
                        state.temperature.status != HealthAlertStatus.OPTIMAL
        
        return if (hasWarning) {
            "Algunos indicadores requieren atención. Evite esfuerzos intensos y mantenga reposo."
        } else {
            "Estado general estable. La tendencia de sus vitales es positiva."
        }
    }
    
    private fun getMinutesSinceLastUpdate(): Int {
        // For now, return a placeholder value
        // In a real implementation, track actual last update timestamp
        return 15
    }
    
    /**
     * Dismiss the current alert.
     */
    fun dismissAlert() {
        _uiState.update { it.copy(currentAlert = null) }
    }
    
    /**
     * Refresh all health data.
     */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        // Re-observe will trigger data refresh
        observeHealthData()
    }
}
