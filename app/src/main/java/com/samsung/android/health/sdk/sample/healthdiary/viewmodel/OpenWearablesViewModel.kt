package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.repository.HealthProviderInfo
import com.samsung.android.health.sdk.sample.healthdiary.repository.OpenWearablesRepository
import com.samsung.android.health.sdk.sample.healthdiary.repository.OpenWearablesState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for OpenWearables screen
 */
data class OpenWearablesUiState(
    val isLoading: Boolean = false,
    val providers: List<HealthProviderInfo> = emptyList(),
    val selectedProvider: String? = null,
    val isAuthorized: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val errorMessage: String? = null,
    val healthData: Map<String, Any?>? = null
)

/**
 * ViewModel for OpenWearables health data integration.
 * Manages connection to Samsung Health or Health Connect via OpenWearables SDK.
 */
@HiltViewModel
class OpenWearablesViewModel @Inject constructor(
    private val repository: OpenWearablesRepository
) : ViewModel() {
    
    companion object {
        // Health data types to sync
        val HEALTH_DATA_TYPES = listOf("steps", "heartRate", "sleep", "oxygenSaturation")
    }
    
    private val _uiState = MutableStateFlow(OpenWearablesUiState())
    val uiState: StateFlow<OpenWearablesUiState> = _uiState.asStateFlow()
    
    val connectionState: StateFlow<OpenWearablesState> = repository.connectionState
    val syncStatus: StateFlow<Map<String, Any?>> = repository.syncStatus
    
    init {
        initialize()
    }
    
    /**
     * Initialize the ViewModel and repository
     */
    fun initialize() {
        repository.initialize()
        loadProviders()
    }
    
    /**
     * Set the current activity for permission requests
     */
    fun setActivity(activity: Activity?) {
        repository.setActivity(activity)
    }
    
    /**
     * Load available health data providers
     */
    fun loadProviders() {
        val providers = repository.getAvailableProviders()
        _uiState.value = _uiState.value.copy(providers = providers)
    }
    
    /**
     * Select a health data provider
     */
    fun selectProvider(providerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val success = repository.setProvider(providerId)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    selectedProvider = providerId,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to select provider: $providerId"
                )
            }
        }
    }
    
    /**
     * Connect to OpenWearables with user credentials
     */
    fun connect(userId: String, accessToken: String, refreshToken: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            repository.signIn(userId, accessToken, refreshToken)
                .onSuccess {
                    requestAuthorization()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Connection failed"
                    )
                }
        }
    }
    
    /**
     * Request authorization for health data types
     */
    fun requestAuthorization() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            repository.requestAuthorization(HEALTH_DATA_TYPES)
                .onSuccess { granted ->
                    _uiState.value = _uiState.value.copy(
                        isAuthorized = granted,
                        isLoading = false
                    )
                    if (granted) {
                        startSync()
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Authorization failed"
                    )
                }
        }
    }
    
    /**
     * Check current authorization status
     */
    fun checkAuthorization() {
        viewModelScope.launch {
            repository.checkAuthorization(HEALTH_DATA_TYPES)
                .onSuccess { authorized ->
                    _uiState.value = _uiState.value.copy(isAuthorized = authorized)
                }
        }
    }
    
    /**
     * Start background sync
     */
    fun startSync(syncDaysBack: Int? = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)
            
            repository.startBackgroundSync(syncDaysBack)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSyncing = false)
                    updateSyncStatus()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    /**
     * Stop background sync
     */
    fun stopSync() {
        viewModelScope.launch {
            repository.stopBackgroundSync()
            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }
    
    /**
     * Trigger immediate sync
     */
    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)
            
            repository.syncNow()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncTime = java.time.LocalDateTime.now().toString()
                    )
                    loadLatestData()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    /**
     * Load latest health data
     */
    fun loadLatestData() {
        viewModelScope.launch {
            repository.readLatestData(HEALTH_DATA_TYPES)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(healthData = data)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to load health data: ${error.message}"
                    )
                }
        }
    }
    
    /**
     * Update sync status from repository
     */
    fun updateSyncStatus() {
        repository.updateSyncStatus()
    }
    
    /**
     * Disconnect from OpenWearables
     */
    fun disconnect() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.value = OpenWearablesUiState(providers = _uiState.value.providers)
        }
    }
    
    /**
     * Reset sync (triggers full re-sync)
     */
    fun resetSync() {
        repository.resetSyncAnchors()
        viewModelScope.launch {
            startSync()
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Notify SDK of foreground state
     */
    fun onForeground() {
        repository.onForeground()
    }
    
    /**
     * Notify SDK of background state
     */
    fun onBackground() {
        repository.onBackground()
    }
}
