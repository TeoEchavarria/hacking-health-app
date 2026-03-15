package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.WatchStepsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data point for steps chart.
 */
data class StepsDataPoint(
    val date: String,
    val steps: Int
)

/**
 * UI State for steps history screen.
 */
data class StepsHistoryUiState(
    val dataPoints: List<StepsDataPoint> = emptyList(),
    val selectedRange: Int = 7, // days
    val totalSteps: Int = 0,
    val averageSteps: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for steps history chart.
 */
class StepsHistoryViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "StepsHistoryVM"
    }
    
    private val repository = WatchHealthIngestionRepository(context)
    
    private val _uiState = MutableStateFlow(StepsHistoryUiState(isLoading = true))
    val uiState: StateFlow<StepsHistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadStepsHistory()
    }
    
    fun loadStepsHistory(days: Int = 7) {
        _uiState.value = _uiState.value.copy(isLoading = true, selectedRange = days)
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "[STEPS][PHONE][QUERIED] range=${days}days, query=getStepsHistory")
                val stepsEntities = repository.getStepsHistory(days)
                Log.i(TAG, "[STEPS][PHONE][QUERIED] rows_returned=${stepsEntities.size}, range=${days}days")
                val dataPoints = stepsEntities.map { it.toDataPoint() }
                
                val totalSteps = dataPoints.sumOf { it.steps }
                val averageSteps = if (dataPoints.isNotEmpty()) {
                    totalSteps / dataPoints.size
                } else 0
                
                _uiState.value = _uiState.value.copy(
                    dataPoints = dataPoints,
                    totalSteps = totalSteps,
                    averageSteps = averageSteps,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "[STEPS][PHONE][QUERIED] ERROR: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load steps history"
                )
            }
        }
    }
    
    fun setRange(days: Int) {
        if (days != _uiState.value.selectedRange) {
            loadStepsHistory(days)
        }
    }
    
    private fun WatchStepsEntity.toDataPoint(): StepsDataPoint {
        return StepsDataPoint(
            date = formatDate(this.date),
            steps = this.steps
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
