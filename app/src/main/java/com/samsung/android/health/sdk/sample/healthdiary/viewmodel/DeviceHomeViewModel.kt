package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.DeviceRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairedDeviceEntity
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionState
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DeviceHomeUiState(
    val devices: List<PairedDeviceEntity> = emptyList(),
    val isLoading: Boolean = false,
    val showAliasDialog: DeviceInfo? = null,
    val error: String? = null
)

class DeviceHomeViewModel(context: Context) : ViewModel() {

    private val repository = DeviceRepository(context)

    private val _uiState = MutableStateFlow(DeviceHomeUiState())
    val uiState: StateFlow<DeviceHomeUiState> = _uiState.asStateFlow()

    init {
        loadDevices()
        observeDeviceConnections()
    }

    /**
     * Load all paired devices from database
     */
    private fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.getAllDevices().collectLatest { devices ->
                    _uiState.value = _uiState.value.copy(
                        devices = devices,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load devices: ${e.message}"
                )
            }
        }
    }

    /**
     * Observe ConnectionStateManager for new device connections
     * Show alias dialog when a new device connects
     */
    private fun observeDeviceConnections() {
        viewModelScope.launch {
            var previousState: ConnectionState? = null
            var previousDevice: DeviceInfo? = null

            ConnectionStateManager.connectionState.collectLatest { state ->
                val currentDevice = ConnectionStateManager.connectedDevice.value

                // Check if connection state changed to VERIFIED (handshake complete)
                if (state == ConnectionState.VERIFIED && 
                    previousState != ConnectionState.VERIFIED &&
                    currentDevice != null) {
                    
                    // Check if this device is new (not in database)
                    val deviceExists = repository.deviceExists(currentDevice.id)
                    val deviceExistsByNode = currentDevice.id.let { 
                        repository.deviceExistsByNodeId(it) 
                    }
                    
                    if (!deviceExists && !deviceExistsByNode) {
                        // New device detected - show alias dialog
                        _uiState.value = _uiState.value.copy(
                            showAliasDialog = currentDevice
                        )
                    } else {
                        // Existing device reconnected - update connection status
                        repository.updateConnectionStatus(currentDevice.id, state.name)
                    }
                }

                previousState = state
                previousDevice = currentDevice
            }
        }
    }

    /**
     * Save device alias when user confirms in dialog
     */
    fun saveDeviceAlias(alias: String) {
        viewModelScope.launch {
            val deviceInfo = _uiState.value.showAliasDialog
            if (deviceInfo != null) {
                try {
                    // Check if device already exists (by node ID from bound device config)
                    val boundNodeId = com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig.getBoundNodeId()
                    val existingDevice = if (boundNodeId != null) {
                        repository.getDeviceByNodeId(boundNodeId)
                    } else {
                        null
                    }

                    if (existingDevice != null) {
                        // Update existing device alias
                        repository.setDeviceAlias(existingDevice.deviceId, alias.ifBlank { null })
                    } else {
                        // Add new device with alias
                        repository.addDevice(
                            deviceId = deviceInfo.id,
                            deviceName = deviceInfo.name,
                            alias = alias.ifBlank { null },
                            boundNodeId = boundNodeId,
                            connectionStatus = "VERIFIED"
                        )
                    }
                    
                    // Close dialog
                    _uiState.value = _uiState.value.copy(showAliasDialog = null)
                    
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to save alias: ${e.message}",
                        showAliasDialog = null
                    )
                }
            }
        }
    }

    /**
     * Skip alias assignment for this device
     */
    fun skipAlias() {
        viewModelScope.launch {
            val deviceInfo = _uiState.value.showAliasDialog
            if (deviceInfo != null) {
                try {
                    // Check if device already exists
                    val boundNodeId = com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig.getBoundNodeId()
                    val existingDevice = if (boundNodeId != null) {
                        repository.getDeviceByNodeId(boundNodeId)
                    } else {
                        null
                    }

                    if (existingDevice == null) {
                        // Add device without alias
                        repository.addDevice(
                            deviceId = deviceInfo.id,
                            deviceName = deviceInfo.name,
                            alias = null,
                            boundNodeId = boundNodeId,
                            connectionStatus = "VERIFIED"
                        )
                    }
                    
                    // Close dialog
                    _uiState.value = _uiState.value.copy(showAliasDialog = null)
                    
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to save device: ${e.message}",
                        showAliasDialog = null
                    )
                }
            }
        }
    }

    /**
     * Dismiss alias dialog without saving
     */
    fun dismissAliasDialog() {
        _uiState.value = _uiState.value.copy(showAliasDialog = null)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Update device alias from device list
     */
    fun updateDeviceAlias(deviceId: String, newAlias: String) {
        viewModelScope.launch {
            try {
                repository.setDeviceAlias(deviceId, newAlias.ifBlank { null })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update alias: ${e.message}"
                )
            }
        }
    }

    /**
     * Remove a paired device
     */
    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            try {
                repository.removeDevice(deviceId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove device: ${e.message}"
                )
            }
        }
    }
}
