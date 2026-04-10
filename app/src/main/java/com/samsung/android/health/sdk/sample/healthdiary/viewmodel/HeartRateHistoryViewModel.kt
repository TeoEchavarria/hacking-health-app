package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.WatchDailySummaryEntity
import com.samsung.android.health.sdk.sample.healthdiary.repository.PatientDataRepository
import com.samsung.android.health.sdk.sample.healthdiary.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data point for heart rate chart.
 */
data class HeartRateDataPoint(
    val date: String,
    val avgBpm: Int?,
    val minBpm: Int?,
    val maxBpm: Int?,
    val sampleCount: Int
)

/**
 * UI State for heart rate history screen.
 */
data class HeartRateHistoryUiState(
    val dataPoints: List<HeartRateDataPoint> = emptyList(),
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
            // If caregiver, first request a sync, then reload
            if (role == "caregiver" && patientId != null) {
                Log.i(TAG, "[HEART_RATE] Caregiver requesting sync...")
                val syncResult = apiRepository.requestSync(patientId!!)
                syncResult.onSuccess {
                    Log.i(TAG, "[HEART_RATE] Sync request created: ${it.requestId}")
                }
                syncResult.onFailure {
                    Log.w(TAG, "[HEART_RATE] Sync request failed (continuing anyway): ${it.message}")
                }
            }
            
            loadHeartRateHistory(_uiState.value.selectedRange)
        }
    }
    
    private suspend fun loadFromLocal(days: Int) {
        Log.d(TAG, "[HEART_RATE][LOCAL] Querying local DB for $days days")
        val summaries = localRepository.getHeartRateHistory(days)
        Log.i(TAG, "[HEART_RATE][LOCAL] Got ${summaries.size} records")
        
        val dataPoints = summaries.map { it.toDataPoint() }
        
        _uiState.value = _uiState.value.copy(
            dataPoints = dataPoints,
            isLoading = false,
            isRefreshing = false,
            error = null
        )
        
        Log.i(TAG, "[HEART_RATE][UI] Rendered ${dataPoints.size} data points")
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
            
            _uiState.value = _uiState.value.copy(
                dataPoints = dataPoints,
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
