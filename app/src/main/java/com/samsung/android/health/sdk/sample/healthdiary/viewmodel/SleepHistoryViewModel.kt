package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.WatchSleepEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data point for sleep chart.
 */
data class SleepDataPoint(
    val date: String,
    val hours: Float // hours (converted from minutes)
)

/**
 * UI State for sleep history screen.
 */
data class SleepHistoryUiState(
    val dataPoints: List<SleepDataPoint> = emptyList(),
    val selectedRange: Int = 7, // days
    val averageHours: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for sleep history chart.
 */
class SleepHistoryViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "SleepHistoryVM"
    }
    
    private val repository = WatchHealthIngestionRepository(context)
    
    private val _uiState = MutableStateFlow(SleepHistoryUiState(isLoading = true))
    val uiState: StateFlow<SleepHistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadSleepHistory()
    }
    
    fun loadSleepHistory(days: Int = 7) {
        _uiState.value = _uiState.value.copy(isLoading = true, selectedRange = days)
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "[SLEEP][PHONE][QUERIED] range=${days}days, query=getSleepHistory")
                val sleepEntities = repository.getSleepHistory(days)
                Log.i(TAG, "[SLEEP][PHONE][QUERIED] rows_returned=${sleepEntities.size}, range=${days}days")
                val dataPoints = sleepEntities.map { it.toDataPoint() }
                
                val averageHours = if (dataPoints.isNotEmpty()) {
                    dataPoints.map { it.hours }.average().toFloat()
                } else 0f
                
                _uiState.value = _uiState.value.copy(
                    dataPoints = dataPoints,
                    averageHours = averageHours,
                    isLoading = false,
                    error = null
                )
                
                // UI render confirmation
                Log.i(TAG, "[SLEEP][UI][RENDERED] count=${dataPoints.size}, avg_hours=$averageHours")
            } catch (e: Exception) {
                Log.e(TAG, "[SLEEP][PHONE][QUERIED] ERROR: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sleep history"
                )
            }
        }
    }
    
    fun setRange(days: Int) {
        if (days != _uiState.value.selectedRange) {
            loadSleepHistory(days)
        }
    }
    
    private fun WatchSleepEntity.toDataPoint(): SleepDataPoint {
        return SleepDataPoint(
            date = formatDate(this.date),
            hours = this.sleepMinutes / 60f
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
