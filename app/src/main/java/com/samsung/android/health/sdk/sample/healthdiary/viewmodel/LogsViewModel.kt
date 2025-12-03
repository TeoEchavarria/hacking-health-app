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
                _uiState.value = _uiState.value.copy(systemLogs = logs)
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val batches = database.sensorBatchDao().getAll() // Assuming getAll() exists and is suspend
            val logs = database.uploadLogDao().getAll() // Assuming getAll() exists
            val docs = database.medicalDocumentDao().getAll()

            _uiState.value = _uiState.value.copy(
                sensorBatches = batches,
                uploadLogs = logs,
                medicalDocuments = docs,
                isLoading = false
            )
        }
    }

    fun syncPendingData() {
        // TODO: Trigger actual sync worker
        loadData() // Refresh for now
    }
}
