package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig
import com.samsung.android.health.sdk.sample.healthdiary.utils.buildReadRequest
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.android.sdk.health.data.request.Ordering
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    
    // Biometric readings - Available from watch
    val heartRate: BiometricReading = BiometricReading(
        value = "--",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "Sin lectura",
        isAvailable = false
    ),
    val steps: BiometricReading = BiometricReading(
        value = "--",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "Sin lectura",
        isAvailable = false
    ),
    val sleep: BiometricReading = BiometricReading(
        value = "--",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "Sin lectura",
        isAvailable = false
    ),
    
    // Biometric readings - NOT available from watch (placeholder)
    val spO2: BiometricReading = BiometricReading(
        value = "--",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "No disponible",
        isAvailable = false
    ),
    val bloodPressure: BiometricReading = BiometricReading(
        value = "--/--",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "No disponible",
        isAvailable = false
    ),
    val temperature: BiometricReading = BiometricReading(
        value = "--°C",
        status = HealthAlertStatus.OPTIMAL,
        statusLabel = "SIN DATOS",
        secondaryText = "No disponible",
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
 * PRIMARY DATA SOURCE: Watch database (WatchHealthIngestionRepository)
 * FALLBACK: Samsung Health SDK (if watch data unavailable)
 * 
 * The watch sends health data every 15 minutes via WatchHealthIngestion,
 * which stores it in the Room database. This ViewModel queries that database
 * and uses reactive Flow queries to automatically update the UI when new data arrives.
 */
class VitalsViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "VitalsViewModel"
        private const val REFRESH_INTERVAL_MS = 30_000L // 30 seconds
    }
    
    private val watchHealthRepo: WatchHealthIngestionRepository by lazy {
        WatchHealthIngestionRepository(context)
    }
    
    private val healthDataStore: HealthDataStore by lazy {
        HealthDataService.getStore(context)
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()
    
    private var lastUpdateTimestamp: Long = 0L
    
    init {
        loadHealthData()
        startPeriodicRefresh()
        observeWatchDataChanges()
    }
    
    /**
     * Observe watch database for real-time updates using Flow.
     * This enables automatic UI updates when new watch data arrives.
     */
    private fun observeWatchDataChanges() {
        val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        // Observe steps from watch
        viewModelScope.launch {
            watchHealthRepo.getTodayStepsFlow(today).collect { stepsEntity ->
                if (stepsEntity != null) {
                    Log.d(TAG, "[FLOW] Watch steps updated: ${stepsEntity.steps}")
                    _uiState.update { state ->
                        state.copy(
                            steps = buildStepsReading(stepsEntity.steps),
                            daySummary = state.daySummary.copy(steps = stepsEntity.steps),
                            lastUpdateTime = "Actualizado ahora"
                        )
                    }
                }
            }
        }
        
        // Observe heart rate from watch
        viewModelScope.launch {
            watchHealthRepo.getLatestHeartRateFlow().collect { hrEntity ->
                if (hrEntity != null) {
                    Log.d(TAG, "[FLOW] Watch heart rate updated: ${hrEntity.bpm} bpm")
                    _uiState.update { state ->
                        state.copy(
                            heartRate = buildHeartRateReading(Pair(hrEntity.bpm, hrEntity.measurementTimestamp)),
                            lastUpdateTime = "Actualizado ahora"
                        )
                    }
                }
            }
        }
        
        // Observe sleep from watch
        viewModelScope.launch {
            val yesterday = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            watchHealthRepo.getTodaySleepFlow(yesterday).collect { sleepEntity ->
                if (sleepEntity != null) {
                    Log.d(TAG, "[FLOW] Watch sleep updated: ${sleepEntity.sleepMinutes} min")
                    val sleepHours = sleepEntity.sleepMinutes / 60f
                    _uiState.update { state ->
                        state.copy(
                            sleep = buildSleepReading(sleepHours),
                            daySummary = state.daySummary.copy(sleepHours = sleepHours),
                            lastUpdateTime = "Actualizado ahora"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Start periodic refresh of health data (every 30 seconds).
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                loadHealthData()
            }
        }
    }
    
    /**
     * Load all health data from WATCH DATABASE first, fallback to Samsung Health if unavailable.
     * Watch data is synced every 15 minutes via WatchHealthIngestion.
     */
    private fun loadHealthData() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "========== LOADING HEALTH DATA (WATCH DB PRIMARY) ==========")
            
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get today's date
                val now = LocalDateTime.now()
                val today = now.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val yesterday = now.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                Log.d(TAG, "[VITALS] Querying watch database for date: $today")
                
                // PRIMARY: Load data from watch database
                var stepsResult = watchHealthRepo.getTodaySteps(today)
                var sleepResult = watchHealthRepo.getTodaySleep(yesterday)?.let { it / 60f }
                var heartRateResult = watchHealthRepo.getLatestHeartRate()?.let { 
                    Pair(it.bpm, it.measurementTimestamp) 
                }
                
                Log.d(TAG, "[VITALS][WATCH_DB] Steps: ${stepsResult ?: "NULL"}")
                Log.d(TAG, "[VITALS][WATCH_DB] Sleep: ${sleepResult ?: "NULL"} hours")
                Log.d(TAG, "[VITALS][WATCH_DB] Heart Rate: ${heartRateResult?.first ?: "NULL"} bpm")
                
                // FALLBACK: If watch data is unavailable, try Samsung Health
                val startOfToday = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                val endOfDay = startOfToday.plusDays(1)
                val startOfYesterday = startOfToday.minusDays(1)
                
                if (stepsResult == null || stepsResult == 0) {
                    Log.d(TAG, "[VITALS] Watch steps unavailable, trying Samsung Health fallback")
                    stepsResult = loadTodaySteps(startOfToday, endOfDay)
                    Log.d(TAG, "[VITALS][SAMSUNG_HEALTH] Steps: ${stepsResult ?: "NULL"}")
                }
                
                if (sleepResult == null) {
                    Log.d(TAG, "[VITALS] Watch sleep unavailable, trying Samsung Health fallback")
                    sleepResult = loadSleepData(startOfYesterday, endOfDay)
                    Log.d(TAG, "[VITALS][SAMSUNG_HEALTH] Sleep: ${sleepResult ?: "NULL"} hours")
                }
                
                if (heartRateResult == null) {
                    Log.d(TAG, "[VITALS] Watch heart rate unavailable, trying Samsung Health fallback")
                    heartRateResult = loadLatestHeartRate(startOfToday, endOfDay)
                    Log.d(TAG, "[VITALS][SAMSUNG_HEALTH] Heart Rate: ${heartRateResult?.first ?: "NULL"} bpm")
                }
                
                // Build readings
                val stepsReading = buildStepsReading(stepsResult)
                val sleepReading = buildSleepReading(sleepResult)
                val heartRateReading = buildHeartRateReading(heartRateResult)
                
                // Update timestamp
                lastUpdateTimestamp = System.currentTimeMillis()
                
                // Generate summary
                val summaryDesc = generateSummaryDescription(stepsResult, sleepResult)
                val tip = generateHealthTip()
                
                _uiState.update { state ->
                    state.copy(
                        heartRate = heartRateReading,
                        steps = stepsReading,
                        sleep = sleepReading,
                        daySummary = DaySummary(
                            steps = stepsResult,
                            sleepHours = sleepResult,
                            description = summaryDesc
                        ),
                        healthTip = tip,
                        lastUpdateTime = "Actualizado ahora",
                        isLoading = false
                    )
                }
                
                Log.d(TAG, "========== HEALTH DATA LOADED SUCCESSFULLY ==========")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading health data from Samsung Health", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * Load today's total steps from Samsung Health.
     */
    private suspend fun loadTodaySteps(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Int? {
        return try {
            val localTimeFilter = LocalTimeFilter.of(startOfDay, endOfDay)
            val localTimeGroup = LocalTimeGroup.of(LocalTimeGroupUnit.HOURLY, 1)
            
            val aggregateRequest = DataType.StepsType.TOTAL.requestBuilder
                .setLocalTimeFilterWithGroup(localTimeFilter, localTimeGroup)
                .setOrdering(Ordering.ASC)
                .build()
            
            val result = healthDataStore.aggregateData(aggregateRequest)
            var totalSteps = 0L
            
            result.dataList.forEach { stepData ->
                val hourlySteps = (stepData.value as? Number)?.toLong() ?: 0L
                totalSteps += hourlySteps
            }
            
            Log.d(TAG, "[STEPS] Samsung Health returned ${result.dataList.size} hourly aggregations, total: $totalSteps")
            
            if (totalSteps > 0) totalSteps.toInt() else null
        } catch (e: Exception) {
            Log.w(TAG, "[STEPS] Error reading steps from Samsung Health", e)
            null
        }
    }
    
    /**
     * Load sleep data from Samsung Health.
     * Returns total hours of sleep.
     */
    private suspend fun loadSleepData(startOfYesterday: LocalDateTime, endOfDay: LocalDateTime): Float? {
        return try {
            val readRequest = DataTypes.SLEEP.buildReadRequest(
                start = startOfYesterday,
                end = endOfDay,
                ordering = Ordering.DESC
            )
            
            val sleepDataList = healthDataStore.readData(readRequest).dataList
            var totalSleepMinutes = 0L
            
            sleepDataList.forEach { sleepData ->
                val duration = sleepData.getValue(DataType.SleepType.DURATION)
                if (duration != null) {
                    // Duration is in seconds, convert to minutes
                    @Suppress("USELESS_CAST")
                    totalSleepMinutes += (duration as Long) / 60
                }
            }
            
            Log.d(TAG, "[SLEEP] Samsung Health returned ${sleepDataList.size} sleep records, total: $totalSleepMinutes min")
            
            if (totalSleepMinutes > 0) totalSleepMinutes / 60f else null
        } catch (e: Exception) {
            Log.w(TAG, "[SLEEP] Error reading sleep from Samsung Health", e)
            null
        }
    }
    
    /**
     * Load the latest heart rate reading from Samsung Health.
     * Returns Pair(bpm, timestamp) or null.
     */
    private suspend fun loadLatestHeartRate(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Pair<Int, Long>? {
        return try {
            val readRequest = DataTypes.HEART_RATE.buildReadRequest(
                start = startOfDay.minusDays(1), // Look back 1 day for recent reading
                end = endOfDay,
                ordering = Ordering.DESC
            )
            
            val heartRateList = healthDataStore.readData(readRequest).dataList
            
            Log.d(TAG, "[HEART_RATE] Samsung Health returned ${heartRateList.size} heart rate records")
            
            val latestReading = heartRateList.firstOrNull()
            if (latestReading != null) {
                val bpm = latestReading.getValue(DataType.HeartRateType.HEART_RATE)?.toInt()
                val timestamp = latestReading.startTime.toEpochMilli()
                
                if (bpm != null && bpm > 0) {
                    Log.d(TAG, "[HEART_RATE] Latest: $bpm bpm at ${latestReading.startTime}")
                    Pair(bpm, timestamp)
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "[HEART_RATE] Error reading heart rate from Samsung Health", e)
            null
        }
    }
    
    private fun buildStepsReading(steps: Int?): BiometricReading {
        return if (steps != null && steps > 0) {
            val status = when {
                steps >= 10000 -> HealthAlertStatus.OPTIMAL
                steps >= 5000 -> HealthAlertStatus.WARNING
                else -> HealthAlertStatus.OPTIMAL
            }
            BiometricReading(
                value = "%,d".format(steps),
                status = status,
                statusLabel = when {
                    steps >= 10000 -> "EXCELENTE"
                    steps >= 5000 -> "BIEN"
                    else -> "ACTIVO"
                },
                secondaryText = "pasos hoy",
                isAvailable = true
            )
        } else {
            BiometricReading(
                value = "0",
                status = HealthAlertStatus.OPTIMAL,
                statusLabel = "INICIO",
                secondaryText = "pasos hoy",
                isAvailable = true
            )
        }
    }
    
    private fun buildSleepReading(sleepHours: Float?): BiometricReading {
        return if (sleepHours != null && sleepHours > 0) {
            val status = when {
                sleepHours >= 7f -> HealthAlertStatus.OPTIMAL
                sleepHours >= 5f -> HealthAlertStatus.WARNING
                else -> HealthAlertStatus.CRITICAL
            }
            BiometricReading(
                value = String.format("%.1f", sleepHours),
                status = status,
                statusLabel = when (status) {
                    HealthAlertStatus.OPTIMAL -> "ÓPTIMO"
                    HealthAlertStatus.WARNING -> "BAJO"
                    HealthAlertStatus.CRITICAL -> "MUY BAJO"
                },
                secondaryText = "horas ayer",
                isAvailable = true
            )
        } else {
            BiometricReading(
                value = "--",
                status = HealthAlertStatus.OPTIMAL,
                statusLabel = "SIN DATOS",
                secondaryText = "Sin lectura",
                isAvailable = false
            )
        }
    }
    
    private fun buildHeartRateReading(hrData: Pair<Int, Long>?): BiometricReading {
        return if (hrData != null) {
            val (bpm, timestamp) = hrData
            val status = when {
                bpm in 60..100 -> HealthAlertStatus.OPTIMAL
                bpm in 50..59 || bpm in 101..120 -> HealthAlertStatus.WARNING
                else -> HealthAlertStatus.CRITICAL
            }
            BiometricReading(
                value = "$bpm",
                status = status,
                statusLabel = when (status) {
                    HealthAlertStatus.OPTIMAL -> "NORMAL"
                    HealthAlertStatus.WARNING -> "ATENCIÓN"
                    HealthAlertStatus.CRITICAL -> "ALERTA"
                },
                secondaryText = "bpm",
                timestamp = timestamp,
                isAvailable = true
            )
        } else {
            BiometricReading(
                value = "--",
                status = HealthAlertStatus.OPTIMAL,
                statusLabel = "SIN DATOS",
                secondaryText = "Sin lectura",
                isAvailable = false
            )
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
                        description = "Detectada recientemente. Se recomienda reposo.",
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
        val stepsMessage = when {
            steps == null -> "Sin datos de pasos"
            steps >= 10000 -> "¡Excelente actividad física!"
            steps >= 5000 -> "Buen progreso en actividad"
            else -> "Intenta moverte más hoy"
        }
        
        val sleepMessage = when {
            sleepHours == null -> ""
            sleepHours >= 7f -> "Descanso óptimo."
            sleepHours >= 5f -> "Considera dormir más."
            else -> "Sueño insuficiente."
        }
        
        return listOf(stepsMessage, sleepMessage)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifEmpty { "Estado general estable." }
    }
    
    private fun getMinutesSinceLastUpdate(): Int {
        if (lastUpdateTimestamp == 0L) return 0
        val elapsed = System.currentTimeMillis() - lastUpdateTimestamp
        return (elapsed / 60_000).toInt()
    }
    
    /**
     * Dismiss the current alert.
     */
    fun dismissAlert() {
        _uiState.update { it.copy(currentAlert = null) }
    }
    
    /**
     * Refresh all health data manually.
     */
    fun refresh() {
        loadHealthData()
    }
    
    /**
     * Request an on-demand measurement from the watch.
     * This sends a message to the watch to trigger immediate data collection.
     * 
     * @param metric The metric to measure: "heart_rate", "steps", or "all"
     */
    fun requestMeasurement(metric: String = "all") {
        viewModelScope.launch {
            try {
                val boundNodeId = DeviceConfig.getBoundNodeId()
                if (boundNodeId == null) {
                    Log.w(TAG, "[MEASURE_REQUEST] No watch bound, cannot request measurement")
                    return@launch
                }
                
                Log.d(TAG, "[MEASURE_REQUEST] Requesting $metric measurement from watch: $boundNodeId")
                
                val payload = json.encodeToString(
                    mapOf(
                        "metric" to metric,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                
                val messageClient = Wearable.getMessageClient(context)
                messageClient.sendMessage(
                    boundNodeId,
                    "/health/measure_request",
                    payload.toByteArray()
                ).await()
                
                Log.d(TAG, "[MEASURE_REQUEST] ✅ Request sent successfully")
                
                // Show loading state briefly
                _uiState.update { it.copy(lastUpdateTime = "Midiendo...") }
                
            } catch (e: Exception) {
                Log.e(TAG, "[MEASURE_REQUEST] ❌ Failed to send request", e)
            }
        }
    }
}
