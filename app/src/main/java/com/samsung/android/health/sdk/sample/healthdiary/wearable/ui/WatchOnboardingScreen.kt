package com.samsung.android.health.sdk.sample.healthdiary.wearable.ui

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.DeviceRepository
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionState
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.DeviceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "WatchOnboardingScreen"

/**
 * Connection state for the watch
 */
sealed class WatchConnectionState {
    object Searching : WatchConnectionState()
    data class Connected(val node: Node) : WatchConnectionState()
    object NotFound : WatchConnectionState()
    data class Error(val message: String) : WatchConnectionState()
}

/**
 * Watch onboarding/pairing screen that detects connected watches
 * and allows users to set up the phone-watch data pipeline.
 */
@Composable
fun WatchOnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Repository for persisting device to database
    val deviceRepository = remember { DeviceRepository(context) }
    
    var connectionState by remember { mutableStateOf<WatchConnectionState>(WatchConnectionState.Searching) }
    var isHealthSyncEnabled by remember { mutableStateOf(false) }
    var showHealthPermissionHint by remember { mutableStateOf(false) }
    
    // Search for connected watch on launch
    LaunchedEffect(Unit) {
        connectionState = searchForConnectedWatch(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Header
        Text(
            text = "Connect Your Watch",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sync steps, heart rate, and sleep data from your Galaxy Watch",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Connection status card
        WatchConnectionCard(
            connectionState = connectionState,
            onRetry = {
                connectionState = WatchConnectionState.Searching
                scope.launch {
                    connectionState = searchForConnectedWatch(context)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Health sync toggle (only show when connected)
        AnimatedVisibility(
            visible = connectionState is WatchConnectionState.Connected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Health Data Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically sync health data from your watch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isHealthSyncEnabled,
                            onCheckedChange = { enabled ->
                                isHealthSyncEnabled = enabled
                                if (enabled) {
                                    showHealthPermissionHint = true
                                }
                            }
                        )
                    }
                }
                
                // Permission hint
                AnimatedVisibility(visible = showHealthPermissionHint) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "💡 Make sure to grant Health permissions on your watch for full functionality",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
            
            Button(
                onClick = {
                    // Persist device to canonical state sources
                    scope.launch {
                        val connectedNode = (connectionState as? WatchConnectionState.Connected)?.node
                        if (connectedNode != null) {
                            // 1. Save to Room database (what Home screen observes)
                            deviceRepository.addDevice(
                                deviceId = connectedNode.id,
                                deviceName = connectedNode.displayName,
                                boundNodeId = connectedNode.id,
                                connectionStatus = "CONNECTED"
                            )
                            Log.i(TAG, "Persisted device to DeviceRepository: ${connectedNode.displayName}")
                            
                            // 2. Update in-memory ConnectionStateManager
                            ConnectionStateManager.setConnectedDevice(
                                DeviceInfo(connectedNode.displayName, connectedNode.id, connectedNode.isNearby)
                            )
                            ConnectionStateManager.setConnectionState(ConnectionState.CONNECTED)
                            Log.i(TAG, "Updated ConnectionStateManager to CONNECTED")
                            
                            // 3. Save legacy config (compatibility)
                            DeviceConfig.setBoundNodeId(connectedNode.id)
                        }
                        
                        // Save sync preference to SharedPreferences
                        val prefs = context.getSharedPreferences("watch_onboarding", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("onboarding_completed", true)
                            .putBoolean("health_sync_enabled", isHealthSyncEnabled)
                            .apply()
                        Log.i(TAG, "Onboarding completed: healthSyncEnabled=$isHealthSyncEnabled")
                        
                        onComplete()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = connectionState is WatchConnectionState.Connected
            ) {
                Text("Continue")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WatchConnectionCard(
    connectionState: WatchConnectionState,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is WatchConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is WatchConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (connectionState) {
                is WatchConnectionState.Searching -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Searching for watch...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                is WatchConnectionState.Connected -> {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Connected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Watch Connected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectionState.node.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                is WatchConnectionState.NotFound -> {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = "Watch",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Watch Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Make sure your watch is paired and nearby",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Try Again")
                    }
                }
                
                is WatchConnectionState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connection Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectionState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

/**
 * Search for a connected Wear OS watch with our app installed
 */
private suspend fun searchForConnectedWatch(context: Context): WatchConnectionState {
    return try {
        // Add a small delay to show searching state
        delay(1000)
        
        val capabilityClient = Wearable.getCapabilityClient(context)
        
        // Look for devices with our app's capability
        val capabilityInfo = capabilityClient
            .getCapability("wear_health_sync", CapabilityClient.FILTER_REACHABLE)
            .await()
        
        val connectedNodes = capabilityInfo.nodes
        
        if (connectedNodes.isNotEmpty()) {
            // Prefer nearby node if available
            val bestNode = connectedNodes.firstOrNull { it.isNearby } ?: connectedNodes.first()
            Log.i(TAG, "Found connected watch: ${bestNode.displayName} (${bestNode.id})")
            WatchConnectionState.Connected(bestNode)
        } else {
            // Fallback: check for any connected node
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            
            if (nodes.isNotEmpty()) {
                val bestNode = nodes.firstOrNull { it.isNearby } ?: nodes.first()
                Log.i(TAG, "Found generic connected node: ${bestNode.displayName}")
                WatchConnectionState.Connected(bestNode)
            } else {
                Log.i(TAG, "No connected watches found")
                WatchConnectionState.NotFound
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error searching for watch", e)
        WatchConnectionState.Error(e.localizedMessage ?: "Unknown error")
    }
}

/**
 * Check if watch onboarding has been completed
 */
fun isWatchOnboardingComplete(context: Context): Boolean {
    val prefs = context.getSharedPreferences("watch_onboarding", Context.MODE_PRIVATE)
    return prefs.getBoolean("onboarding_completed", false)
}

/**
 * Check if health sync is enabled
 */
fun isHealthSyncEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("watch_onboarding", Context.MODE_PRIVATE)
    return prefs.getBoolean("health_sync_enabled", true) // Default to true
}
