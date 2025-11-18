package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.ApiConstants
import com.samsung.android.health.sdk.sample.healthdiary.api.models.DateRange
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SyncResponse
import com.samsung.android.health.sdk.sample.healthdiary.repository.SyncRepository
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SyncViewModel(private val syncRepository: SyncRepository) : ViewModel() {
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _syncState.emit(SyncState.Error(exception.message ?: "Unknown error"))
            _errorMessage.emit(exception.message ?: "Unknown error occurred")
        }
    }
    
    fun syncAll(forceRefresh: Boolean = false) {
        syncData(ApiConstants.SYNC_TYPE_ALL, null, forceRefresh)
    }
    
    fun syncHealth(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        forceRefresh: Boolean = false
    ) {
        val dateRange = if (startDate != null && endDate != null) {
            DateRange(
                startDate = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        } else null
        
        syncData(ApiConstants.SYNC_TYPE_HEALTH, dateRange, forceRefresh)
    }
    
    fun syncFood(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        forceRefresh: Boolean = false
    ) {
        val dateRange = if (startDate != null && endDate != null) {
            DateRange(
                startDate = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        } else null
        
        syncData(ApiConstants.SYNC_TYPE_FOOD, dateRange, forceRefresh)
    }
    
    private fun syncData(
        syncType: String,
        dateRange: DateRange?,
        forceRefresh: Boolean
    ) {
        if (!syncRepository.validateToken()) {
            _syncState.value = SyncState.Error("No authentication token available")
            _errorMessage.value = "Please set your access token before syncing"
            return
        }
        
        viewModelScope.launch(AppConstants.SCOPE_IO_DISPATCHERS + exceptionHandler) {
            _syncState.emit(SyncState.Loading)
            _errorMessage.emit(null)
            
            val result = syncRepository.syncData(syncType, dateRange, forceRefresh)
            
            result.fold(
                onSuccess = { response ->
                    _syncState.emit(SyncState.Success(response))
                    _errorMessage.emit(null)
                },
                onFailure = { exception ->
                    val errorMsg = when {
                        exception.message?.contains("401") == true -> "Unauthorized: Invalid token"
                        exception.message?.contains("400") == true -> "Bad Request: Invalid parameters"
                        exception.message?.contains("500") == true -> "Server Error: Please try again later"
                        exception.message?.contains("Network") == true -> "Network error: Check your connection"
                        else -> exception.message ?: "Sync failed"
                    }
                    _syncState.emit(SyncState.Error(errorMsg))
                    _errorMessage.emit(errorMsg)
                }
            )
        }
    }
    
    fun resetState() {
        _syncState.value = SyncState.Idle
        _errorMessage.value = null
    }
    
    sealed class SyncState {
        object Idle : SyncState()
        object Loading : SyncState()
        data class Success(val response: SyncResponse) : SyncState()
        data class Error(val message: String) : SyncState()
    }
}

