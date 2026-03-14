package com.samsung.android.health.sdk.sample.healthdiary.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    VERIFIED
}

data class DeviceInfo(
    val name: String,
    val id: String,
    val isNearby: Boolean,
    val alias: String? = null,
    val lastSyncTimestamp: Long? = null
)

object ConnectionStateManager {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: StateFlow<DeviceInfo?> = _connectedDevice.asStateFlow()

    private val _lastHandshakeTimestamp = MutableStateFlow<Long>(0)
    val lastHandshakeTimestamp: StateFlow<Long> = _lastHandshakeTimestamp.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices.asStateFlow()

    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
        ConnectionLogManager.log(LogType.INFO, "ConnectionState", "State changed to $state")
    }

    fun setConnectedDevice(device: DeviceInfo?) {
        _connectedDevice.value = device
        if (device != null) {
            ConnectionLogManager.log(LogType.INFO, "ConnectionState", "Device selected: ${device.name} (${device.id})")
        }
    }

    fun updateHandshake(timestamp: Long) {
        _lastHandshakeTimestamp.value = timestamp
        setConnectionState(ConnectionState.VERIFIED)
        ConnectionLogManager.log(LogType.SUCCESS, "ConnectionState", "Handshake verified at $timestamp")
    }

    fun updateDiscoveredDevices(devices: List<DeviceInfo>) {
        _discoveredDevices.value = devices
    }
}
