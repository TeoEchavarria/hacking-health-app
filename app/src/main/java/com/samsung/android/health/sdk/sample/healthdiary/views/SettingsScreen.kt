package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.common.GoogleApiAvailability
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionState
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.DeviceInfo
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var apiUrl by remember { mutableStateOf(DeviceConfig.getApiBaseUrl()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Connection State from Manager
    val connectionState by ConnectionStateManager.connectionState.collectAsState()
    val connectedDevice by ConnectionStateManager.connectedDevice.collectAsState()
    val lastHandshake by ConnectionStateManager.lastHandshakeTimestamp.collectAsState()
    
    // Local state for discovery
    var discoveredNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    
    // Logs
    val logs by ConnectionLogManager.logs.collectAsState()

    // Permission State
    val permissionsToCheck = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var anyDenied = false
        permissions.entries.forEach { 
            if (!it.value) {
                ConnectionLogManager.log(LogType.ERROR, "Settings", "Permission denied: ${it.key}")
                anyDenied = true
            } else {
                ConnectionLogManager.log(LogType.SUCCESS, "Settings", "Permission granted: ${it.key}")
            }
        }
        if (!anyDenied) {
             ConnectionLogManager.log(LogType.SUCCESS, "Settings", "All required permissions granted.")
        }
    }

    // Check watch connection on screen load
    LaunchedEffect(Unit) {
        // Log GMS Availability
        val gms = GoogleApiAvailability.getInstance()
        val status = gms.isGooglePlayServicesAvailable(context)
        if (status == com.google.android.gms.common.ConnectionResult.SUCCESS) {
            ConnectionLogManager.log(LogType.SUCCESS, "Settings", "Google Play Services available.")
        } else {
            ConnectionLogManager.log(LogType.ERROR, "Settings", "Google Play Services NOT available: Code $status")
        }
        
        permissionLauncher.launch(permissionsToCheck)
    }

    // Load bound device on start
    LaunchedEffect(Unit) {
        val boundId = DeviceConfig.getBoundNodeId()
        if (boundId != null) {
            ConnectionStateManager.setConnectionState(ConnectionState.CONNECTING)
            // Try to find this node again
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = nodeClient.connectedNodes.await()
                val found = nodes.find { it.id == boundId }
                if (found != null) {
                    ConnectionStateManager.setConnectedDevice(DeviceInfo(found.displayName, found.id, found.isNearby))
                    ConnectionStateManager.setConnectionState(ConnectionState.CONNECTED)
                } else {
                    ConnectionLogManager.log(LogType.ERROR, "Settings", "Bound device $boundId not found in connected nodes.")
                    ConnectionStateManager.setConnectionState(ConnectionState.DISCONNECTED)
                }
            } catch (e: Exception) {
                ConnectionLogManager.log(LogType.ERROR, "Settings", "Error restoring connection: ${e.message}")
            }
        }
    }

    fun initiateHandshake(nodeId: String) {
        scope.launch {
            ConnectionLogManager.log(LogType.INFO, "Settings", "Initiating Handshake with $nodeId...")
            try {
                val dataClient = Wearable.getDataClient(context)
                val putDataMapReq = PutDataMapRequest.create("/handshake_request")
                putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
                putDataMapReq.dataMap.putString("source", "phone_manual")
                val putDataReq = putDataMapReq.asPutDataRequest()
                putDataReq.setUrgent()
                
                dataClient.putDataItem(putDataReq).await()
                ConnectionLogManager.log(LogType.TRAFFIC, "Settings", "📤 Handshake Request sent")
                snackbarHostState.showSnackbar("Handshake request sent")
            } catch (e: Exception) {
                ConnectionLogManager.log(LogType.ERROR, "Settings", "❌ Handshake failed: ${e.message}")
            }
        }
    }
    
    fun bindDevice(node: Node) {
        scope.launch {
            DeviceConfig.setBoundNodeId(node.id)
            ConnectionStateManager.setConnectedDevice(DeviceInfo(node.displayName, node.id, node.isNearby))
            ConnectionStateManager.setConnectionState(ConnectionState.CONNECTED)
            ConnectionLogManager.log(LogType.INFO, "Settings", "Bound to device: ${node.displayName}")
            snackbarHostState.showSnackbar("Bound to ${node.displayName}")
            
            // Auto-trigger handshake
            initiateHandshake(node.id)
        }
    }

    fun scanForDevices() {
        scope.launch {
            isScanning = true
            scanError = null
            ConnectionLogManager.log(LogType.INFO, "Settings", "Scanning for wearable devices...")
            ConnectionStateManager.setConnectionState(ConnectionState.SCANNING)
            
            try {
                // Method 1: Get all connected nodes (System level)
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = nodeClient.connectedNodes.await()
                
                // Method 2: Get nodes with specific capability (App level)
                val capabilityClient = Wearable.getCapabilityClient(context)
                // Add specific debug log for capabilities
                try {
                    val allCaps = capabilityClient.getAllCapabilities(com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE).await()
                    allCaps.forEach { (name, capabilityInfo) ->
                        ConnectionLogManager.log(LogType.INFO, "Settings", "Found Capability: $name - Nodes: ${capabilityInfo.nodes.size}")
                    }
                } catch (e: Exception) {
                     ConnectionLogManager.log(LogType.ERROR, "Settings", "Failed to list capabilities: ${e.message}")
                }

                val capabilityInfo = capabilityClient.getCapability("sensor_data_sender", com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE).await()
                val capableNodes = capabilityInfo.nodes
                
                // Combine or prioritize
                discoveredNodes = nodes
                
                if (nodes.isEmpty()) {
                    ConnectionLogManager.log(LogType.ERROR, "Settings", "No devices found via NodeClient. Check Permissions & Galaxy Wearable App connection.")
                } else {
                    nodes.forEach { node ->
                        val isCapable = capableNodes.any { it.id == node.id }
                        ConnectionLogManager.log(LogType.SUCCESS, "Settings", "Found: ${node.displayName} (Capable: $isCapable)")
                    }
                }
                
                ConnectionStateManager.setConnectionState(if (connectedDevice != null) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED)
                
            } catch (e: Exception) {
                scanError = e.message
                ConnectionLogManager.log(LogType.ERROR, "Settings", "Scan failed: ${e.message}")
                ConnectionStateManager.setConnectionState(ConnectionState.DISCONNECTED)
            }
            isScanning = false
        }
    }
    
    fun unbindDevice() {
        DeviceConfig.setBoundNodeId(null)
        ConnectionStateManager.setConnectedDevice(null)
        ConnectionStateManager.setConnectionState(ConnectionState.DISCONNECTED)
        ConnectionLogManager.log(LogType.INFO, "Settings", "Unbound device")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { ConnectionLogManager.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Connection Status Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status Indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            ConnectionState.VERIFIED -> Color(0xFF4CAF50) // Green
                                            ConnectionState.CONNECTED -> Color(0xFFFFC107) // Amber
                                            ConnectionState.CONNECTING, ConnectionState.SCANNING -> Color(0xFF2196F3) // Blue
                                            else -> Color(0xFFF44336) // Red
                                        }
                                    )
                            )
                            Text(
                                text = when (connectionState) {
                                    ConnectionState.VERIFIED -> "Verified & Active"
                                    ConnectionState.CONNECTED -> "Connected (No Handshake)"
                                    ConnectionState.CONNECTING -> "Connecting..."
                                    ConnectionState.SCANNING -> "Scanning..."
                                    else -> "Disconnected"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (connectedDevice != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Active Device:", style = MaterialTheme.typography.labelSmall)
                        Text(connectedDevice!!.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("ID: ${connectedDevice!!.id}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        
                        if (lastHandshake > 0) {
                            Text(
                                "Last Handshake: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(lastHandshake))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        } else {
                             Text("No handshake verified yet", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             Button(
                                onClick = { initiateHandshake(connectedDevice!!.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Handshake")
                            }
                            OutlinedButton(
                                onClick = { unbindDevice() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Unbind")
                            }
                        }
                    } else {
                         Text("No device bound.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // --- Discovery Section ---
            if (connectedDevice == null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Available Devices", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { scanForDevices() }, enabled = !isScanning) {
                                if (isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Scan")
                                }
                            }
                        }
                        
                        if (discoveredNodes.isEmpty() && !isScanning) {
                             Text("No devices found. Make sure Bluetooth is on.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }
                        
                        discoveredNodes.forEach { node ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(node.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text(node.id, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Button(onClick = { bindDevice(node) }) {
                                    Text("Bind")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            // --- API Config Section ---
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("Base API URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Button(
                        onClick = {
                            DeviceConfig.setApiBaseUrl(apiUrl)
                            scope.launch { snackbarHostState.showSnackbar("Settings saved") }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )

            // --- Live Log Console ---
            Text("Live Diagnostic Log", style = MaterialTheme.typography.titleMedium)
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize(),
                    reverseLayout = false 
                ) {
                    items(logs) { log ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                text = log.formattedTime,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(60.dp)
                            )
                            Text(
                                text = "[${log.tag}] ",
                                color = Color(0xFFBB86FC),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = log.message,
                                color = when(log.type) {
                                    LogType.ERROR -> Color(0xFFFF5252)
                                    LogType.SUCCESS -> Color(0xFF4CAF50)
                                    LogType.TRAFFIC -> Color(0xFF64B5F6)
                                    else -> Color.White
                                },
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

suspend fun testConnection(urlString: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("$urlString/docs") // Using /docs as a health check
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                "Success: $responseCode"
            } else {
                "Error: $responseCode"
            }
        } catch (e: Exception) {
            "Failed: ${e.message}"
        }
    }
}
