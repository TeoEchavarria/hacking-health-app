package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.WatchDailySummaryEntity
import com.samsung.android.health.sdk.sample.healthdiary.repository.PatientDataRepository
import com.samsung.android.health.sdk.sample.healthdiary.repository.UserRepository
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.HeartRateSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data point for heart rate chart (legacy - daily aggregation).
 */
data class HeartRateDataPoint(
    val date: String,
    val avgBpm: Int?,
    val minBpm: Int?,
    val maxBpm: Int?,
    val sampleCount: Int
)

/**
 * Hourly heart rate data for the new chart.
 * Shows min-max range as a vertical bar for each hour.
 */
data class HourlyHeartRateData(
    val hour: String, // Format: "08:00", "09:00", etc.
    val hourIndex: Int, // 0-23
    val minBpm: Int,
    val maxBpm: Int,
    val sampleCount: Int
)

/**
 * UI State for heart rate history screen.
 */
data class HeartRateHistoryUiState(
    val dataPoints: List<HeartRateDataPoint> = emptyList(),
    val hourlyData: List<HourlyHeartRateData> = emptyList(),
    val selectedRange: Int = 1, // 24 hours (1 day)
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val patientName: String? = null,
    val isCaregiver: Boolean = false
)

/**
 * ViewModel for heart rate history chart.
 * 
 * Supports dual data sources:
 * - PATIENT: Uses local WatchHealthIngestionRepository (data from own watch)
 * - CAREGIVER: Uses PatientDataRepository (data from API)
 */
class HeartRateHistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "HeartRateHistoryVM"
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    private val localRepository = WatchHealthIngestionRepository(application.applicationContext)
    private val apiRepository = PatientDataRepository(application.applicationContext)
    private val userRepository = UserRepository(application.applicationContext)
    
    private var role: String = "patient"
    private var patientId: String? = null
    
    private val _uiState = MutableStateFlow(HeartRateHistoryUiState(isLoading = true))
    val uiState: StateFlow<HeartRateHistoryUiState> = _uiState.asStateFlow()
    
    init {
        initializeAndLoad()
    }
    
    private fun initializeAndLoad() {
        viewModelScope.launch {
            // Determine role and patientId from active pairing
            val activePairing = userRepository.getActivePairing()
            
            if (activePairing != null) {
                role = activePairing.userRole
                patientId = if (role == "caregiver") {
                    activePairing.patientId
                } else {
                    null
                }
                
                _uiState.value = _uiState.value.copy(
                    isCaregiver = role == "caregiver",
                    patientName = if (role == "caregiver") activePairing.patientName else null
                )
                
                Log.d(TAG, "[HEART_RATE] Initialized: role=$role, patientId=$patientId")
            }
            
            loadHeartRateHistory()
        }
    }
    
    fun loadHeartRateHistory(days: Int = 1) {
        _uiState.value = _uiState.value.copy(isLoading = true, selectedRange = days)
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "[HEART_RATE] Loading history: role=$role, patientId=$patientId, days=$days")
                
                if (role == "caregiver" && patientId != null) {
                    loadFromApi(patientId!!, days)
                } else {
                    loadFromLocal(days)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "[HEART_RATE] ERROR: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Error al cargar historial"
                )
            }
        }
    }
    
    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        
        viewModelScope.launch {
            if (role == "caregiver" && patientId != null) {
                // Caregiver: Request sync from patient, then reload from API
                Log.i(TAG, "[HEART_RATE] Caregiver requesting sync...")
                val syncResult = apiRepository.requestSync(patientId!!)
                syncResult.onSuccess {
                    Log.i(TAG, "[HEART_RATE] Sync request created: ${it.requestId}")
                }
                syncResult.onFailure {
                    Log.w(TAG, "[HEART_RATE] Sync request failed (continuing anyway): ${it.message}")
                }
            } else {
                // Patient: Trigger immediate sync to backend BEFORE reloading
                Log.i(TAG, "[HEART_RATE] Patient triggering immediate sync to backend...")
                com.samsung.android.health.sdk.sample.healthdiary.worker.HealthSyncWorker.triggerNow(
                    getApplication<android.app.Application>().applicationContext
                )
                // Small delay to let sync complete
                kotlinx.coroutines.delay(1000)
            }
            
            loadHeartRateHistory(_uiState.value.selectedRange)
        }
    }
    
    private suspend fun loadFromLocal(days: Int) {
        Log.d(TAG, "[HEART_RATE][LOCAL] Querying local DB for $days days")
        val summaries = localRepository.getHeartRateHistory(days)
        Log.i(TAG, "[HEART_RATE][LOCAL] Got ${summaries.size} daily summary records")
        
        // Debug: Log each summary's HR data
        summaries.forEachIndexed { index, summary ->
            Log.d(TAG, "[HEART_RATE][LOCAL][DEBUG] Record $index: date=${summary.date}, " +
                "avgHR=${summary.avgHeartRate}, minHR=${summary.minHeartRate}, maxHR=${summary.maxHeartRate}, " +
                "sampleCount=${summary.heartRateSampleCount}, hasJson=${!summary.heartRateSamplesJson.isNullOrEmpty()}")
        }
        
        val dataPoints = summaries.map { it.toDataPoint() }
        
        // Parse all heart rate samples from daily summaries
        val allSamples = mutableListOf<HeartRateSample>()
        val existingTimestamps = mutableSetOf<Long>()
        
        summaries.forEach { summary ->
            if (!summary.heartRateSamplesJson.isNullOrEmpty()) {
                try {
                    val samples = json.decodeFromString<List<HeartRateSample>>(summary.heartRateSamplesJson)
                    allSamples.addAll(samples)
                    samples.forEach { existingTimestamps.add(it.timestamp) }
                    Log.d(TAG, "[HEART_RATE][LOCAL][SAMPLES] Parsed ${samples.size} samples from ${summary.date}")
                } catch (e: Exception) {
                    Log.e(TAG, "[HEART_RATE][LOCAL][SAMPLES] Failed to parse JSON for ${summary.date}: ${e.message}")
                }
            }
        }
        
        Log.i(TAG, "[HEART_RATE][LOCAL][SUMMARIES] Samples from daily summaries: ${allSamples.size}")
        
        // Also fetch real-time HR samples from watch_heart_rate table
        // This includes recent data that hasn't been batched into daily summaries yet
        try {
            val realtimeSamples = localRepository.getRealtimeHeartRates(days)
            Log.i(TAG, "[HEART_RATE][LOCAL][REALTIME] Found ${realtimeSamples.size} real-time HR samples")
            
            // Convert WatchHeartRateEntity to HeartRateSample and add if not duplicate
            var addedCount = 0
            realtimeSamples.forEach { entity ->
                if (!existingTimestamps.contains(entity.measurementTimestamp)) {
                    val accuracy = try {
                        com.samsung.android.health.sdk.sample.healthdiary.wearable.model.HeartRateAccuracy.valueOf(entity.accuracy)
                    } catch (e: Exception) {
                        com.samsung.android.health.sdk.sample.healthdiary.wearable.model.HeartRateAccuracy.UNKNOWN
                    }
                    allSamples.add(HeartRateSample(
                        bpm = entity.bpm,
                        timestamp = entity.measurementTimestamp,
                        accuracy = accuracy
                    ))
                    existingTimestamps.add(entity.measurementTimestamp)
                    addedCount++
                }
            }
            Log.i(TAG, "[HEART_RATE][LOCAL][REALTIME] Added $addedCount new samples (${realtimeSamples.size - addedCount} duplicates skipped)")
        } catch (e: Exception) {
            Log.e(TAG, "[HEART_RATE][LOCAL][REALTIME] Failed to load real-time samples: ${e.message}")
        }
        
        Log.i(TAG, "[HEART_RATE][LOCAL][SAMPLES] Total samples collected: ${allSamples.size}")
        
        // Group samples by hour and calculate min/max
        val hourlyData = groupSamplesByHour(allSamples)
        Log.i(TAG, "[HEART_RATE][LOCAL][HOURLY] Grouped into ${hourlyData.size} hourly buckets")
        
        _uiState.value = _uiState.value.copy(
            dataPoints = dataPoints,
            hourlyData = hourlyData,
            isLoading = false,
            isRefreshing = false,
            error = null
        )
        
        Log.i(TAG, "[HEART_RATE][UI] Rendered ${dataPoints.size} daily points, ${hourlyData.size} hourly bars")
    }
    
    /**
     * Group heart rate samples by hour and calculate min/max for each hour.
     */
    private fun groupSamplesByHour(samples: List<HeartRateSample>): List<HourlyHeartRateData> {
        if (samples.isEmpty()) return emptyList()
        
        val calendar = Calendar.getInstance()
        val hourFormat = SimpleDateFormat("HH:00", Locale.getDefault())
        
        // Group by hour
        val hourlyGroups = samples.groupBy { sample ->
            calendar.timeInMillis = sample.timestamp
            calendar.get(Calendar.HOUR_OF_DAY)
        }
        
        return hourlyGroups.map { (hourIndex, hourSamples) ->
            val bpmValues = hourSamples.map { it.bpm }
            HourlyHeartRateData(
                hour = String.format("%02d:00", hourIndex),
                hourIndex = hourIndex,
                minBpm = bpmValues.minOrNull() ?: 0,
                maxBpm = bpmValues.maxOrNull() ?: 0,
                sampleCount = hourSamples.size
            )
        }.sortedBy { it.hourIndex }
    }
    
    private suspend fun loadFromApi(patientId: String, days: Int) {
        Log.d(TAG, "[HEART_RATE][API] Fetching from API for patient $patientId, $days days")
        
        val result = apiRepository.getPatientHeartRateHistory(patientId, days)
        
        result.onSuccess { response ->
            Log.i(TAG, "[HEART_RATE][API] Got ${response.dataPoints.size} data points")
            
            val dataPoints = response.dataPoints.map { dataPoint ->
                HeartRateDataPoint(
                    date = formatDate(dataPoint.date),
                    avgBpm = dataPoint.avgBpm,
                    minBpm = dataPoint.minBpm,
                    maxBpm = dataPoint.maxBpm,
                    sampleCount = dataPoint.sampleCount
                )
            }
            
            // API currently returns daily aggregates only, not hourly samples
            // Caregiver sees daily data - no fake hourly bars
            val hourlyData = emptyList<HourlyHeartRateData>()
            Log.i(TAG, "[HEART_RATE][API] Daily aggregates only (no hourly samples from API)")
            
            _uiState.value = _uiState.value.copy(
                dataPoints = dataPoints,
                hourlyData = hourlyData,
                patientName = response.patientName,
                isLoading = false,
                isRefreshing = false,
                error = null
            )
            
            Log.i(TAG, "[HEART_RATE][UI] Rendered ${dataPoints.size} data points for ${response.patientName}")
        }
        
        result.onFailure { error ->
            Log.e(TAG, "[HEART_RATE][API] Failed: ${error.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = error.message ?: "Error al cargar historial"
            )
        }
    }
    
    private fun WatchDailySummaryEntity.toDataPoint(): HeartRateDataPoint {
        return HeartRateDataPoint(
            date = formatDate(this.date),
            avgBpm = this.avgHeartRate,
            minBpm = this.minHeartRate,
            maxBpm = this.maxHeartRate,
            sampleCount = this.heartRateSampleCount
        )
    }
    
    private fun formatDate(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val date = inputFormat.parse(isoDate)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            isoDate
        }
    }
}
