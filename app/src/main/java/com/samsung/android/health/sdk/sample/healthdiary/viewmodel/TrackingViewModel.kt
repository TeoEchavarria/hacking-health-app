package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.LocationPointEntity
import com.samsung.android.health.sdk.sample.healthdiary.repository.TrackingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Tracking ViewModel
 * 
 * Manages state for the GPS tracking screen with:
 * - Current location and activity status
 * - 24-hour location history timeline
 * - Battery level monitoring
 * - Auto-refresh simulation
 */
class TrackingViewModel(context: Context) : ViewModel() {
    
    private val repository = TrackingRepository(context)
    
    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()
    
    init {
        // Initialize with mock data
        viewModelScope.launch {
            repository.initializeMockData()
            loadLocationHistory()
            startAutoTracking()
        }
    }
    
    /**
     * Load location history from repository
     */
    private fun loadLocationHistory() {
        viewModelScope.launch {
            repository.getLast24Hours()
                .collect { locations ->
                    val latest = locations.firstOrNull()
                    _uiState.update { state ->
                        state.copy(
                            currentLocation = latest,
                            locationHistory = locations,
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    /**
     * Start automatic tracking simulation
     * Adds a new location point every 60 seconds
     */
    private fun startAutoTracking() {
        viewModelScope.launch {
            while (true) {
                delay(60_000) // 60 seconds
                
                if (_uiState.value.isTracking) {
                    addSimulatedLocationPoint()
                }
            }
        }
    }
    
    /**
     * Add a simulated location point near the last position
     */
    private suspend fun addSimulatedLocationPoint() {
        val lastLocation = repository.getLatestLocation()
        val activityTypes = listOf("Walking", "Walking", "Still", "Walking") // Weighted random
        val randomActivity = activityTypes.random()
        
        val newLocation = repository.generateMockLocationNearLastPosition(
            lastLocation = lastLocation,
            activityType = randomActivity
        )
        
        repository.insertLocation(newLocation)
        
        // Update battery (simulate drain)
        _uiState.update { state ->
            state.copy(
                batteryLevel = maxOf(1, state.batteryLevel - Random.nextInt(0, 2)),
                currentActivity = randomActivity
            )
        }
        
        // Cleanup old locations (older than 24h)
        repository.cleanupOldLocations()
    }
    
    /**
     * Toggle tracking on/off
     */
    fun toggleTracking() {
        _uiState.update { state ->
            state.copy(isTracking = !state.isTracking)
        }
    }
    
    /**
     * Simulate emergency call action
     */
    fun callEmergency() {
        // Placeholder for future implementation
        // Could open dialer with emergency contact
    }
    
    /**
     * Simulate verify location action
     */
    fun verifyLocation() {
        // Placeholder for future implementation
        // Could trigger backend API call to verify location
    }
}

/**
 * UI State for Tracking Screen
 */
data class TrackingUiState(
    val currentLocation: LocationPointEntity? = null,
    val locationHistory: List<LocationPointEntity> = emptyList(),
    val isTracking: Boolean = true,
    val currentActivity: String = "Walking",
    val batteryLevel: Int = 84,
    val userName: String = "Marta García",
    val isLoading: Boolean = true
)
