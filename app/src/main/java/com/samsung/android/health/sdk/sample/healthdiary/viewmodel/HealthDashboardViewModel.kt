package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.DeviceRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairedDeviceEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.WatchHeartRateEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.WatchSleepEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.WatchStepsEntity
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionState
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType
import com.samsung.android.health.sdk.sample.healthdiary.wearable.DeviceConnectionMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Connection status for UI display.
 * This reflects REAL-TIME transport state, not cached historical values.
 */
sealed class DeviceConnectionStatus {
    object Disconnected : DeviceConnectionStatus()
    object BluetoothOff : DeviceConnectionStatus()
    object Searching : DeviceConnectionStatus()
    object Connected : DeviceConnectionStatus()
    object Verified : DeviceConnectionStatus()
    
    val displayText: String
        get() = when (this) {
            is Disconnected -> "Disconnected"
            is BluetoothOff -> "Bluetooth Off"
            is Searching -> "Searching..."
            is Connected -> "Connected"
            is Verified -> "Connected & Verified"
        }
    
    val isConnected: Boolean
        get() = this is Connected || this is Verified
}

/**
 * UI State for the Health Dashboard screen.
 */
data class HealthDashboardUiState(
    // Device info
    val connectedDevice: PairedDeviceEntity? = null,
    val connectionStatus: DeviceConnectionStatus = DeviceConnectionStatus.Disconnected,
    val lastSyncTime: String? = null,
    
    // Health metrics (nullable = unavailable)
    val currentHeartRate: Int? = null,
    val heartRateTimestamp: String? = null,
    val todaySleepHours: Float? = null,
    val todayMaxSteps: Int? = null,
    
    // Loading states
    val isLoadingDevice: Boolean = true,
    val isLoadingMetrics: Boolean = true,
    
    // Error state
    val error: String? = null
)

/**
 * ViewModel for the Health Dashboard (new main screen).
 * 
 * Provides:
 * - Real-time device connection status
 * - Current heart rate from watch
 * - Today's sleep hours
 * - Today's steps (cumulative daily total)
 */
class HealthDashboardViewModel(private val context: Context) : ViewModel() {
    
    private val deviceRepository = DeviceRepository(context)
    private val healthRepository = WatchHealthIngestionRepository(context)
    
    private val _uiState = MutableStateFlow(HealthDashboardUiState())
    val uiState: StateFlow<HealthDashboardUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    init {
        observeConnectionState()
        observeDevices()
        observeHealthMetrics()
    }
    
    /**
     * Observe real-time connection state from DeviceConnectionMonitor.
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            // Combine multiple state flows for comprehensive connection status
            combine(
                ConnectionStateManager.connectionState,
                DeviceConnectionMonitor.bluetoothEnabled,
                DeviceConnectionMonitor.reachableNodes
            ) { connectionState, bluetoothEnabled, reachableNodes ->
                Triple(connectionState, bluetoothEnabled, reachableNodes)
            }.collect { (connectionState, bluetoothEnabled, reachableNodes) ->
                val status = when {
                    !bluetoothEnabled -> DeviceConnectionStatus.BluetoothOff
                    connectionState == ConnectionState.DISCONNECTED -> DeviceConnectionStatus.Disconnected
                    connectionState == ConnectionState.SCANNING -> DeviceConnectionStatus.Searching
                    connectionState == ConnectionState.CONNECTING -> DeviceConnectionStatus.Searching
                    connectionState == ConnectionState.CONNECTED -> DeviceConnectionStatus.Connected
                    connectionState == ConnectionState.VERIFIED -> DeviceConnectionStatus.Verified
                    else -> DeviceConnectionStatus.Disconnected
                }
                
                _uiState.value = _uiState.value.copy(connectionStatus = status)
            }
        }
    }
    
    /**
     * Observe paired devices from database.
     * Shows the first/primary device.
     */
    private fun observeDevices() {
        viewModelScope.launch {
            deviceRepository.getAllDevices().collect { devices ->
                val primaryDevice = devices.firstOrNull()
                
                _uiState.value = _uiState.value.copy(
                    connectedDevice = primaryDevice,
                    lastSyncTime = primaryDevice?.lastSyncTimestamp?.let { 
                        formatTimestamp(it) 
                    },
                    isLoadingDevice = false
                )
            }
        }
    }
    
    /**
     * Observe health metrics from watch data.
     */
    private fun observeHealthMetrics() {
        val today = dateFormat.format(Date())
        
        // Heart rate (latest reading)
        viewModelScope.launch {
            healthRepository.getLatestHeartRateFlow().collect { heartRate ->
                _uiState.value = _uiState.value.copy(
                    currentHeartRate = heartRate?.bpm,
                    heartRateTimestamp = heartRate?.measurementTimestamp?.let { 
                        formatTimestamp(it) 
                    },
                    isLoadingMetrics = false
                )
            }
        }
        
        // Sleep (today)
        viewModelScope.launch {
            healthRepository.getTodaySleepFlow(today).collect { sleep ->
                _uiState.value = _uiState.value.copy(
                    todaySleepHours = sleep?.sleepMinutes?.let { it / 60f }
                )
            }
        }
        
        // Steps (today)
        viewModelScope.launch {
            healthRepository.getTodayStepsFlow(today).collect { steps ->
                _uiState.value = _uiState.value.copy(
                    todayMaxSteps = steps?.steps
                )
            }
        }
    }
    
    /**
     * Refresh connection status by forcing a reachability check.
     */
    fun refreshConnectionStatus() {
        DeviceConnectionMonitor.forceCheck()
    }
    
    /**
     * Send a test message to the watch to verify connectivity.
     * The watch will display "You're connected" notification.
     */
    fun sendTestMessageToWatch() {
        viewModelScope.launch {
            try {
                val boundNodeId = DeviceConfig.getBoundNodeId()
                if (boundNodeId == null) {
                    _uiState.value = _uiState.value.copy(error = "No watch bound")
                    ConnectionLogManager.log(LogType.ERROR, "TestMessage", "No bound node ID found")
                    return@launch
                }
                
                ConnectionLogManager.log(LogType.INFO, "TestMessage", "Sending test message to watch: $boundNodeId")
                
                val messageClient = Wearable.getMessageClient(context)
                val result = messageClient.sendMessage(
                    boundNodeId,
                    "/notification/test",
                    "You're connected".toByteArray()
                ).await()
                
                ConnectionLogManager.log(LogType.SUCCESS, "TestMessage", "✅ Test message sent successfully (requestId: $result)")
                _uiState.value = _uiState.value.copy(error = null)
                
            } catch (e: Exception) {
                Log.e("HealthDashboardVM", "Failed to send test message", e)
                ConnectionLogManager.log(LogType.ERROR, "TestMessage", "❌ Failed to send: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to send test message: ${e.message}")
            }
        }
    }
    
    /**
     * Clear any error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Format timestamp to human-readable string.
     */
    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} min ago"
            diff < 86400_000 -> "Today at ${timeFormat.format(date)}"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
        }
    }
}
