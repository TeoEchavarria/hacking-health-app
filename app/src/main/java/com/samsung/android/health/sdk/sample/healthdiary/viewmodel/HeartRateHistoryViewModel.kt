package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.WatchDailySummaryEntity
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
    val selectedRange: Int = 7, // days
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for heart rate history chart.
 */
class HeartRateHistoryViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "HeartRateHistoryVM"
    }
    
    private val repository = WatchHealthIngestionRepository(context)
    
    private val _uiState = MutableStateFlow(HeartRateHistoryUiState(isLoading = true))
    val uiState: StateFlow<HeartRateHistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHeartRateHistory()
    }
    
    fun loadHeartRateHistory(days: Int = 7) {
        _uiState.value = _uiState.value.copy(isLoading = true, selectedRange = days)
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "[HEART_RATE][PHONE][QUERIED] range=${days}days, query=getHeartRateHistory")
                val summaries = repository.getHeartRateHistory(days)
                Log.i(TAG, "[HEART_RATE][PHONE][QUERIED] rows_returned=${summaries.size}, range=${days}days")
                val dataPoints = summaries.map { it.toDataPoint() }
                
                _uiState.value = _uiState.value.copy(
                    dataPoints = dataPoints,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "[HEART_RATE][PHONE][QUERIED] ERROR: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load heart rate history"
                )
            }
        }
    }
    
    fun setRange(days: Int) {
        if (days != _uiState.value.selectedRange) {
            loadHeartRateHistory(days)
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
