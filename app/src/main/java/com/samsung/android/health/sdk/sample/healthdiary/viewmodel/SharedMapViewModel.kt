package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LocationData
import com.samsung.android.health.sdk.sample.healthdiary.components.MapMarker
import com.samsung.android.health.sdk.sample.healthdiary.repository.LocationShareRepository
import com.samsung.android.health.sdk.sample.healthdiary.service.LocationSyncService
import com.samsung.android.health.sdk.sample.healthdiary.utils.LocationProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI State for the shared map screen.
 */
data class SharedMapUiState(
    val isLoading: Boolean = true,
    val myLocation: Location? = null,
    val pairedUserLocation: LocationData? = null,
    val pairedUserName: String? = null,
    val errorMessage: String? = null,
    val isSharingEnabled: Boolean = true,
    val isLocationSyncRunning: Boolean = false,
    val lastUpdateTime: Long = 0
) {
    /**
     * Get markers for the map.
     */
    fun getMarkers(): List<MapMarker> {
        val markers = mutableListOf<MapMarker>()
        
        myLocation?.let { location ->
            markers.add(
                MapMarker(
                    id = "me",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    title = "Mi ubicación",
                    snippet = "Tú estás aquí",
                    isCurrentUser = true
                )
            )
        }
        
        pairedUserLocation?.let { location ->
            markers.add(
                MapMarker(
                    id = "paired",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    title = pairedUserName ?: "Usuario vinculado",
                    snippet = "Última actualización: ${getTimeAgo(location.updatedAt)}",
                    isCurrentUser = false
                )
            )
        }
        
        return markers
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "hace un momento"
            diff < 3600_000 -> "hace ${diff / 60_000} min"
            diff < 86400_000 -> "hace ${diff / 3600_000} horas"
            else -> "hace más de un día"
        }
    }
}

/**
 * ViewModel for the shared map screen.
 *
 * Manages:
 * - Current user's GPS location (from LocationProvider)
 * - Paired user's location (from API via polling)
 * - Location sharing toggle
 * - Location sync service control
 */
class SharedMapViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SharedMapViewModel"
        private const val POLLING_INTERVAL_MS = 30_000L  // 30 seconds
    }

    private val context = application.applicationContext
    private val locationProvider = LocationProvider(context)
    private val locationRepository = LocationShareRepository(context)

    private val _uiState = MutableStateFlow(SharedMapUiState())
    val uiState: StateFlow<SharedMapUiState> = _uiState.asStateFlow()

    private var locationUpdateJob: Job? = null
    private var pollingJob: Job? = null

    init {
        checkSharingStatus()
        checkSyncServiceStatus()
    }

    /**
     * Start tracking locations (both local GPS and paired user via API).
     */
    fun startTracking() {
        Log.i(TAG, "Starting location tracking")
        
        startLocalLocationUpdates()
        startPairedLocationPolling()
    }

    /**
     * Stop tracking locations.
     */
    fun stopTracking() {
        Log.i(TAG, "Stopping location tracking")
        
        locationUpdateJob?.cancel()
        pollingJob?.cancel()
    }

    /**
     * Toggle location sharing preference.
     */
    fun toggleSharing(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            locationRepository.toggleSharing(enabled)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSharingEnabled = response.sharingEnabled
                    )
                    
                    // Start/stop sync service based on sharing preference
                    if (response.sharingEnabled) {
                        startLocationSyncService()
                    } else {
                        stopLocationSyncService()
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }

    /**
     * Center map on current user's location.
     */
    fun centerOnMyLocation() {
        viewModelScope.launch {
            try {
                val location = locationProvider.getCurrentLocation()
                _uiState.value = _uiState.value.copy(myLocation = location)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current location", e)
            }
        }
    }

    /**
     * Refresh paired user's location immediately.
     */
    fun refreshPairedLocation() {
        viewModelScope.launch {
            fetchPairedLocation()
        }
    }

    /**
     * Clear any displayed error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Start the location sync service (foreground service).
     */
    fun startLocationSyncService() {
        if (locationProvider.hasLocationPermission()) {
            LocationSyncService.start(context)
            _uiState.value = _uiState.value.copy(isLocationSyncRunning = true)
        }
    }

    /**
     * Stop the location sync service.
     */
    fun stopLocationSyncService() {
        LocationSyncService.stop(context)
        _uiState.value = _uiState.value.copy(isLocationSyncRunning = false)
    }

    private fun startLocalLocationUpdates() {
        locationUpdateJob?.cancel()
        
        locationUpdateJob = viewModelScope.launch {
            // First, try to get last known location quickly
            locationProvider.getLastLocation()?.let { lastLocation ->
                _uiState.value = _uiState.value.copy(
                    myLocation = lastLocation,
                    isLoading = false
                )
            }
            
            // Then start continuous updates for active viewing
            locationProvider.startActiveLocationUpdates()
                .catch { e ->
                    Log.e(TAG, "Error in location updates", e)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error obteniendo ubicación: ${e.message}"
                    )
                }
                .collectLatest { location ->
                    _uiState.value = _uiState.value.copy(
                        myLocation = location,
                        isLoading = false
                    )
                }
        }
    }

    private fun startPairedLocationPolling() {
        pollingJob?.cancel()
        
        pollingJob = viewModelScope.launch {
            while (true) {
                fetchPairedLocation()
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    private suspend fun fetchPairedLocation() {
        locationRepository.getPairedLocation()
            .onSuccess { response ->
                if (response.found && response.location != null) {
                    _uiState.value = _uiState.value.copy(
                        pairedUserLocation = response.location,
                        pairedUserName = response.location.userName,
                        lastUpdateTime = System.currentTimeMillis(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        pairedUserLocation = null,
                        pairedUserName = null,
                        isLoading = false
                    )
                    Log.d(TAG, "Paired location not available: ${response.message}")
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Error fetching paired location: ${error.message}")
                // Don't show error for polling failures, just log them
            }
    }

    private fun checkSharingStatus() {
        viewModelScope.launch {
            locationRepository.getSharingStatus()
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isSharingEnabled = response.sharingEnabled
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Error checking sharing status: ${error.message}")
                }
        }
    }

    private fun checkSyncServiceStatus() {
        val isRunning = LocationSyncService.isRunning(context)
        _uiState.value = _uiState.value.copy(isLocationSyncRunning = isRunning)
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
