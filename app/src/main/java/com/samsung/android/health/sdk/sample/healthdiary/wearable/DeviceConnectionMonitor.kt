package com.samsung.android.health.sdk.sample.healthdiary.wearable

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionState
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.DeviceInfo
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * DeviceConnectionMonitor - Real-time monitoring of watch connection state.
 * 
 * This singleton monitors:
 * 1. Bluetooth adapter state (on/off)
 * 2. Wear OS node availability (via CapabilityClient listener)
 * 3. Periodic reachability checks (via NodeClient.getConnectedNodes)
 * 
 * It updates ConnectionStateManager and PairedDeviceEntity based on REAL transport state,
 * not cached/historical values.
 * 
 * Connection State Flow:
 * - DISCONNECTED: Bluetooth off OR no reachable nodes
 * - CONNECTING: Bluetooth on, searching for nodes
 * - CONNECTED: Node found and reachable
 * - VERIFIED: Handshake completed successfully
 */
object DeviceConnectionMonitor {
    
    private const val TAG = "DeviceConnectionMonitor"
    private const val CAPABILITY_WATCH_APP = "sensor_data_sender"
    private const val CAPABILITY_HEALTH_SYNC = "wear_health_sync"
    private const val REACHABILITY_CHECK_INTERVAL_MS = 10_000L // 10 seconds
    
    private var context: Context? = null
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Real-time reachable nodes
    private val _reachableNodes = MutableStateFlow<List<Node>>(emptyList())
    val reachableNodes: StateFlow<List<Node>> = _reachableNodes.asStateFlow()
    
    // Bluetooth state
    private val _bluetoothEnabled = MutableStateFlow(false)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()
    
    // Last reachability check timestamp
    private val _lastReachabilityCheck = MutableStateFlow(0L)
    val lastReachabilityCheck: StateFlow<Long> = _lastReachabilityCheck.asStateFlow()
    
    private var capabilityListener: CapabilityClient.OnCapabilityChangedListener? = null
    private var bluetoothReceiver: BroadcastReceiver? = null
    
    /**
     * Initialize and start monitoring.
     * Call this from Application.onCreate() or MainActivity.onCreate()
     */
    fun initialize(appContext: Context) {
        if (context != null) {
            Log.d(TAG, "Already initialized")
            return
        }
        
        context = appContext.applicationContext
        Log.i(TAG, "Initializing DeviceConnectionMonitor")
        ConnectionLogManager.log(LogType.INFO, TAG, "Initializing connection monitor")
        
        // Check initial Bluetooth state
        checkBluetoothState()
        
        // Register Bluetooth state receiver
        registerBluetoothReceiver()
        
        // Register capability listener
        registerCapabilityListener()
        
        // Start periodic reachability checks
        startPeriodicReachabilityCheck()
        
        // Do initial check
        scope.launch {
            checkReachability()
        }
    }
    
    /**
     * Stop monitoring and cleanup resources.
     * Call this when app is being destroyed.
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down DeviceConnectionMonitor")
        
        monitorJob?.cancel()
        monitorJob = null
        
        unregisterBluetoothReceiver()
        unregisterCapabilityListener()
        
        context = null
    }
    
    /**
     * Force a reachability check now.
     * Call this when user triggers a manual refresh.
     */
    fun forceCheck() {
        scope.launch {
            checkReachability()
        }
    }
    
    // ============ BLUETOOTH STATE MONITORING ============
    
    private fun checkBluetoothState() {
        val ctx = context ?: return
        try {
            val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
            val isEnabled = adapter?.isEnabled == true
            
            _bluetoothEnabled.value = isEnabled
            Log.d(TAG, "Bluetooth state: ${if (isEnabled) "ENABLED" else "DISABLED"}")
            ConnectionLogManager.log(
                LogType.INFO, 
                TAG, 
                "Bluetooth state: ${if (isEnabled) "ENABLED" else "DISABLED"}"
            )
            
            if (!isEnabled) {
                // Bluetooth is off - immediately mark as disconnected
                updateConnectionState(ConnectionState.DISCONNECTED, emptyList())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission", e)
        }
    }
    
    private fun registerBluetoothReceiver() {
        val ctx = context ?: return
        
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    handleBluetoothStateChange(state)
                }
            }
        }
        
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        ctx.registerReceiver(bluetoothReceiver, filter)
        Log.d(TAG, "Bluetooth receiver registered")
    }
    
    private fun unregisterBluetoothReceiver() {
        val ctx = context ?: return
        bluetoothReceiver?.let {
            try {
                ctx.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering Bluetooth receiver", e)
            }
        }
        bluetoothReceiver = null
    }
    
    private fun handleBluetoothStateChange(state: Int) {
        val stateName = when (state) {
            BluetoothAdapter.STATE_OFF -> "OFF"
            BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
            BluetoothAdapter.STATE_ON -> "ON"
            BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
            else -> "UNKNOWN ($state)"
        }
        
        Log.i(TAG, "Bluetooth state changed: $stateName")
        ConnectionLogManager.log(LogType.INFO, TAG, "Bluetooth state changed: $stateName")
        
        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                _bluetoothEnabled.value = false
                _reachableNodes.value = emptyList()
                updateConnectionState(ConnectionState.DISCONNECTED, emptyList())
            }
            BluetoothAdapter.STATE_ON -> {
                _bluetoothEnabled.value = true
                // Bluetooth just turned on - start searching
                updateConnectionState(ConnectionState.SCANNING, emptyList())
                scope.launch {
                    delay(2000) // Give Bluetooth time to stabilize
                    checkReachability()
                }
            }
        }
    }
    
    // ============ CAPABILITY LISTENER ============
    
    private fun registerCapabilityListener() {
        val ctx = context ?: return
        
        capabilityListener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            handleCapabilityChanged(capabilityInfo)
        }
        
        try {
            // Listen for watch app capability
            Wearable.getCapabilityClient(ctx)
                .addListener(capabilityListener!!, CAPABILITY_WATCH_APP)
            
            // Also listen for health sync capability
            Wearable.getCapabilityClient(ctx)
                .addListener(capabilityListener!!, CAPABILITY_HEALTH_SYNC)
                
            Log.d(TAG, "Capability listeners registered")
            ConnectionLogManager.log(LogType.INFO, TAG, "Capability listeners registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register capability listener", e)
        }
    }
    
    private fun unregisterCapabilityListener() {
        val ctx = context ?: return
        capabilityListener?.let { listener ->
            try {
                Wearable.getCapabilityClient(ctx).removeListener(listener)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing capability listener", e)
            }
        }
        capabilityListener = null
    }
    
    private fun handleCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val nodes = capabilityInfo.nodes.toList()
        Log.i(TAG, "Capability changed: ${capabilityInfo.name}, nodes: ${nodes.map { it.displayName }}")
        ConnectionLogManager.log(
            LogType.INFO, 
            TAG, 
            "Capability '${capabilityInfo.name}' nodes: ${nodes.size}"
        )
        
        // Update reachable nodes
        scope.launch {
            checkReachability()
        }
    }
    
    // ============ PERIODIC REACHABILITY CHECK ============
    
    private fun startPeriodicReachabilityCheck() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                delay(REACHABILITY_CHECK_INTERVAL_MS)
                if (_bluetoothEnabled.value) {
                    checkReachability()
                }
            }
        }
        Log.d(TAG, "Periodic reachability check started (interval: ${REACHABILITY_CHECK_INTERVAL_MS}ms)")
    }
    
    /**
     * Check actual node reachability via NodeClient.
     * This is the ground truth for connection state.
     */
    private suspend fun checkReachability() {
        val ctx = context ?: return
        
        if (!_bluetoothEnabled.value) {
            Log.d(TAG, "Skipping reachability check - Bluetooth disabled")
            return
        }
        
        try {
            val nodeClient = Wearable.getNodeClient(ctx)
            val connectedNodes = nodeClient.connectedNodes.await()
            
            _lastReachabilityCheck.value = System.currentTimeMillis()
            _reachableNodes.value = connectedNodes
            
            Log.d(TAG, "Reachability check: ${connectedNodes.size} nodes found")
            connectedNodes.forEach { node ->
                Log.d(TAG, "  - ${node.displayName} (${node.id}) nearby=${node.isNearby}")
            }
            
            // Determine connection state based on nodes
            val boundNodeId = DeviceConfig.getBoundNodeId()
            val boundNode = connectedNodes.find { it.id == boundNodeId }
            
            when {
                connectedNodes.isEmpty() -> {
                    updateConnectionState(ConnectionState.DISCONNECTED, connectedNodes)
                }
                boundNode != null -> {
                    // Our bound device is reachable
                    updateConnectionState(ConnectionState.CONNECTED, connectedNodes)
                }
                else -> {
                    // Nodes available but not our bound device
                    updateConnectionState(ConnectionState.CONNECTED, connectedNodes)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Reachability check failed", e)
            ConnectionLogManager.log(LogType.ERROR, TAG, "Reachability check failed: ${e.message}")
        }
    }
    
    // ============ STATE UPDATES ============
    
    private fun updateConnectionState(state: ConnectionState, nodes: List<Node>) {
        val previousState = ConnectionStateManager.connectionState.value
        
        if (previousState != state) {
            Log.i(TAG, "Connection state: $previousState -> $state")
            ConnectionLogManager.log(LogType.INFO, TAG, "State: $previousState -> $state")
        }
        
        ConnectionStateManager.setConnectionState(state)
        
        // Update connected device info
        val boundNodeId = DeviceConfig.getBoundNodeId()
        val boundNode = nodes.find { it.id == boundNodeId }
        
        if (boundNode != null && state == ConnectionState.CONNECTED) {
            ConnectionStateManager.setConnectedDevice(
                DeviceInfo(
                    name = boundNode.displayName,
                    id = boundNode.id,
                    isNearby = boundNode.isNearby
                )
            )
        } else if (state == ConnectionState.DISCONNECTED) {
            // Don't clear the device info completely, just indicate disconnected
            // This preserves the device identity while showing disconnected state
        }
        
        // Update database connection status
        updateDatabaseConnectionStatus(state, boundNodeId)
    }
    
    private fun updateDatabaseConnectionStatus(state: ConnectionState, boundNodeId: String?) {
        val ctx = context ?: return
        
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(ctx)
                val pairedDeviceDao = db.pairedDeviceDao()
                
                // Update all paired devices' connection status based on reachability
                val nodes = _reachableNodes.value
                val allDevices = pairedDeviceDao.getAllSync()
                
                for (device in allDevices) {
                    val isReachable = nodes.any { it.id == device.boundNodeId }
                    val newStatus = when {
                        !_bluetoothEnabled.value -> "DISCONNECTED"
                        isReachable -> "CONNECTED"
                        else -> "DISCONNECTED"
                    }
                    
                    if (device.connectionStatus != newStatus) {
                        Log.d(TAG, "Updating ${device.deviceName} status: ${device.connectionStatus} -> $newStatus")
                        pairedDeviceDao.updateConnectionStatus(device.deviceId, newStatus)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update database connection status", e)
            }
        }
    }
    
    /**
     * Check if a specific node is currently reachable.
     */
    fun isNodeReachable(nodeId: String): Boolean {
        return _reachableNodes.value.any { it.id == nodeId }
    }
    
    /**
     * Get the best (nearest) reachable node.
     */
    fun getBestNode(): Node? {
        val nodes = _reachableNodes.value
        return nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
    }
}
