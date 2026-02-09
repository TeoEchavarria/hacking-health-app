package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.MedicalDocumentEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.SensorBatchEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.UploadLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LogsUiState(
    val sensorBatches: List<SensorBatchEntity> = emptyList(),
    val uploadLogs: List<UploadLogEntity> = emptyList(),
    val medicalDocuments: List<MedicalDocumentEntity> = emptyList(),
    val systemLogs: List<com.samsung.android.health.sdk.sample.healthdiary.utils.LogEntry> = emptyList(),
    // Filtered logs by source
    val watchLogs: List<com.samsung.android.health.sdk.sample.healthdiary.utils.LogEntry> = emptyList(),
    val phoneLogs: List<com.samsung.android.health.sdk.sample.healthdiary.utils.LogEntry> = emptyList(),
    val habitLogs: List<com.samsung.android.health.sdk.sample.healthdiary.utils.LogEntry> = emptyList(),
    val apiLogs: List<com.samsung.android.health.sdk.sample.healthdiary.utils.LogEntry> = emptyList(),
    // Sensor data stats
    val pendingSensorCount: Int = 0,
    val totalSensorCount: Int = 0,
    val isLoading: Boolean = false
)

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeTelemetry()
    }

    private fun observeTelemetry() {
        viewModelScope.launch {
            com.samsung.android.health.sdk.sample.healthdiary.utils.TelemetryLogger.logs.collect { logs ->
                // Filter logs by source for different tabs
                val watchLogs = logs.filter { it.source == "WATCH" }
                val phoneLogs = logs.filter { it.source == "PHONE" }
                val habitLogs = logs.filter { it.source == "HABIT" }
                val apiLogs = logs.filter { it.source == "API" }
                
                _uiState.value = _uiState.value.copy(
                    systemLogs = logs,
                    watchLogs = watchLogs,
                    phoneLogs = phoneLogs,
                    habitLogs = habitLogs,
                    apiLogs = apiLogs
                )
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val batches = database.sensorBatchDao().getAll()
            val logs = database.uploadLogDao().getAll()
            val docs = database.medicalDocumentDao().getAll()
            
            // Get sensor data stats
            val pendingCount = database.sensorDataDao().getUnsyncedCount()
            val totalCount = database.sensorDataDao().getCount()

            _uiState.value = _uiState.value.copy(
                sensorBatches = batches,
                uploadLogs = logs,
                medicalDocuments = docs,
                pendingSensorCount = pendingCount,
                totalSensorCount = totalCount,
                isLoading = false
            )
        }
    }

    fun syncPendingData() {
        // TODO: Trigger actual sync worker
        loadData() // Refresh for now
    }
}
