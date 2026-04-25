package com.samsung.android.health.sdk.sample.healthdiary.wearable.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
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
import kotlin.math.cos
import kotlin.math.sin

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
 * Watch onboarding/pairing screen with Bluetooth permissions and device search
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WatchOnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Repository for persisting device to database
    val deviceRepository = remember { DeviceRepository(context) }
    
    // Bluetooth permissions
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    val permissionsState = rememberMultiplePermissionsState(permissionsToRequest)
    
    // Bluetooth adapter state
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager?.adapter }
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }
    
    // Connection state
    var connectionState by remember { mutableStateOf<WatchConnectionState?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    
    // Bluetooth enable launcher
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
    }
    
    // Start search when permissions granted and Bluetooth enabled
    LaunchedEffect(permissionsState.allPermissionsGranted, isBluetoothEnabled) {
        if (permissionsState.allPermissionsGranted && isBluetoothEnabled && connectionState == null) {
            isSearching = true
            delay(500)
            connectionState = searchForConnectedWatch(context)
            isSearching = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Background decorations
        BackgroundDecorations()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top navigation
            TopNavigationBar(onClose = onSkip)
            
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Header with sonar animation
                SearchingHeader(isSearching = isSearching || connectionState is WatchConnectionState.Searching)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Permissions section
                if (!permissionsState.allPermissionsGranted || !isBluetoothEnabled) {
                    Text(
                        text = "PERMISOS REQUERIDOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PermissionsSection(
                        permissionsState = permissionsState,
                        isBluetoothEnabled = isBluetoothEnabled,
                        onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() },
                        onEnableBluetooth = {
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            bluetoothEnableLauncher.launch(enableBtIntent)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Devices section
                if (permissionsState.allPermissionsGranted && isBluetoothEnabled) {
                    Text(
                        text = "DISPOSITIVOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DevicesSection(
                        connectionState = connectionState,
                        isSearching = isSearching,
                        onRetry = {
                            scope.launch {
                                isSearching = true
                                connectionState = searchForConnectedWatch(context)
                                isSearching = false
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        
        // Bottom action button - positioned at the bottom for better UX
        if (connectionState is WatchConnectionState.Connected) {
            BottomActionButton(
                modifier = Modifier.align(Alignment.BottomCenter),
                onConnect = {
                    scope.launch {
                        val connectedNode = (connectionState as? WatchConnectionState.Connected)?.node
                        if (connectedNode != null) {
                            // 1. Save to Room database
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
                            
                            // 3. Save legacy config
                            DeviceConfig.setBoundNodeId(connectedNode.id)
                        }
                        
                        // Save onboarding completion
                        val prefs = context.getSharedPreferences("watch_onboarding", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("onboarding_completed", true)
                            .putBoolean("health_sync_enabled", true)
                            .apply()
                        Log.i(TAG, "Onboarding completed")
                        
                        onComplete()
                    }
                }
            )
        }
    }
}

@Composable
private fun BackgroundDecorations() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top left blob
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .alpha(0.3f)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2
            )
        }
        
        // Bottom right blob
        Canvas(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .alpha(0.2f)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2
            )
        }
    }
}

@Composable
private fun TopNavigationBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Santuario Digital",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchingHeader(isSearching: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sonar animation
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            SonarAnimation(isAnimating = isSearching)
            
            // Watch icon in center
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = "Watch",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isSearching) "Buscando Tu Reloj..." else "Configurar Reloj",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Estamos localizando dispositivos cercanos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SonarAnimation(isAnimating: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar")
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )
    
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )
    
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )
    
    if (isAnimating) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF6366F1),
                    radius = size.minDimension / 2 * scale1,
                    alpha = alpha1
                )
                drawCircle(
                    color = Color(0xFF8B5CF6),
                    radius = size.minDimension / 2 * scale2,
                    alpha = alpha2
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionsSection(
    permissionsState: MultiplePermissionsState,
    isBluetoothEnabled: Boolean,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Bluetooth permission card
        val bluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsState.permissions.any { 
                it.permission == Manifest.permission.BLUETOOTH_SCAN && it.status.isGranted 
            } && permissionsState.permissions.any { 
                it.permission == Manifest.permission.BLUETOOTH_CONNECT && it.status.isGranted 
            }
        } else {
            true // Bluetooth permissions not required on older Android
        }
        
        PermissionCard(
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth",
            description = "Conexión con el dispositivo",
            isGranted = bluetoothGranted && isBluetoothEnabled,
            onClick = {
                if (!bluetoothGranted) {
                    onRequestPermissions()
                } else if (!isBluetoothEnabled) {
                    onEnableBluetooth()
                }
            }
        )
        
        // Location permission card
        val locationGranted = permissionsState.permissions.any { 
            it.permission == Manifest.permission.ACCESS_FINE_LOCATION && it.status.isGranted 
        }
        
        PermissionCard(
            icon = Icons.Default.LocationOn,
            title = "Ubicación",
            description = "Para encontrar el reloj",
            isGranted = locationGranted,
            onClick = onRequestPermissions
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = if (!isGranted) onClick else ({})
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = if (isGranted) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Grant",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DevicesSection(
    connectionState: WatchConnectionState?,
    isSearching: Boolean,
    onRetry: () -> Unit
) {
    when {
        isSearching || connectionState is WatchConnectionState.Searching -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerDeviceItem()
                ShimmerDeviceItem()
            }
        }
        
        connectionState is WatchConnectionState.Connected -> {
            DeviceItem(node = connectionState.node)
        }
        
        connectionState is WatchConnectionState.NotFound -> {
            EmptyDeviceState(onRetry = onRetry)
        }
        
        connectionState is WatchConnectionState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connectionState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
        }
        
        else -> {
            // Initial state - no action yet
        }
    }
}

@Composable
private fun DeviceItem(node: Node) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = "Watch",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = node.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Signal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Señal excelente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Connected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ShimmerDeviceItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = shimmerAlpha),
                        CircleShape
                    )
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = shimmerAlpha),
                            RoundedCornerShape(4.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = shimmerAlpha),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun EmptyDeviceState(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "No devices",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No se encontraron dispositivos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Asegúrate de que tu reloj esté emparejado y cerca",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun BottomActionButton(
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Conectar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
