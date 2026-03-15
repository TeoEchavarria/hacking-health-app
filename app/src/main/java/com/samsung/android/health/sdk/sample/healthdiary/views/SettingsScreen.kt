package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.GoogleApiAvailability
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLegacyHome: () -> Unit = {}
) {
    val context = LocalContext.current
    var apiUrl by remember { mutableStateOf(DeviceConfig.getApiBaseUrl()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Connection State from Manager (read-only for diagnostics)
    val connectedDevice by ConnectionStateManager.connectedDevice.collectAsState()
    
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
    
    Scaffold(
        topBar = {
            SandboxTopBar(
                title = "Settings & Diagnostics",
                onNavigationClick = onNavigateBack,
                actions = {
                    SandboxIconButton(
                        icon = Icons.Default.Delete,
                        onClick = { ConnectionLogManager.clearLogs() },
                        contentDescription = "Clear Logs"
                    )
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
            // --- Developer Note Card ---
            SandboxCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Watch",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Watch Connection Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "💡 To connect or manage your watch, complete the onboarding flow when you first open the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Read-only connection status for diagnostics
                    if (connectedDevice != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Column {
                                Text(
                                    text = "Connected: ${connectedDevice!!.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ID: ${connectedDevice!!.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF44336))
                            )
                            Text(
                                text = "No watch connected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- API Config Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SandboxInput(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = "Base API URL",
                    modifier = Modifier.weight(1f)
                )
                SandboxButton(
                    text = "Save",
                    onClick = {
                        DeviceConfig.setApiBaseUrl(apiUrl)
                        scope.launch { snackbarHostState.showSnackbar("Settings saved") }
                    }
                )
            }

            // --- Previous Version Access ---
            SandboxCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToLegacyHome
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Previous Version",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Access the original feature-rich home screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate to Previous Version",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Live Log Console ---
            SandboxSection(
                title = "Live Diagnostic Log",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                SandboxCard(
                    modifier = Modifier.fillMaxWidth()
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
                                        else -> Color(0xFF424242) // Dark gray for readability on white bg
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
