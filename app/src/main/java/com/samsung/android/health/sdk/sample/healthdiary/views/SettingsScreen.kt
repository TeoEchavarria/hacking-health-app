package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by ConnectionLogManager.logs.collectAsState()
    var testingConnection by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            SandboxTopBar(
                title = "Diagnostic Logs",
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
                .fillMaxSize()
        ) {
            // Test Watch Connection Button
            SandboxButton(
                text = if (testingConnection) "Testing..." else "Test Watch Connection",
                onClick = {
                    testingConnection = true
                    scope.launch {
                        val result = testWatchConnectivity(context)
                        testingConnection = false
                        snackbarHostState.showSnackbar(
                            message = result,
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                enabled = !testingConnection,
                icon = Icons.Default.Wifi
            )
            
            // Live Log Console
            SandboxCard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(12.dp)
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
                                    else -> Color(0xFF424242)
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

/**
 * Test watch connectivity by sending a ping message and waiting for pong.
 * Returns a user-friendly status message.
 */
suspend fun testWatchConnectivity(context: android.content.Context): String {
    return withContext(Dispatchers.IO) {
        try {
            val boundNodeId = DeviceConfig.getBoundNodeId()
            if (boundNodeId == null) {
                ConnectionLogManager.log(LogType.ERROR, "WatchTest", "No bound node ID found")
                return@withContext "⚠️ No watch bound - Complete onboarding first"
            }
            
            ConnectionLogManager.log(LogType.INFO, "WatchTest", "Sending ping to watch: $boundNodeId")
            
            val messageClient = Wearable.getMessageClient(context)
            
            // Send ping with timeout
            val result = withTimeoutOrNull(5000L) {
                try {
                    messageClient.sendMessage(
                        boundNodeId,
                        "/ping",
                        "ping".toByteArray()
                    ).await()
                    true
                } catch (e: Exception) {
                    ConnectionLogManager.log(LogType.ERROR, "WatchTest", "Ping failed: ${e.message}")
                    false
                }
            }
            
            if (result == true) {
                ConnectionLogManager.log(LogType.SUCCESS, "WatchTest", "✅ Ping sent successfully")
                "✅ Connected - Watch is responding"
            } else if (result == false) {
                "❌ Not Responding - Check watch connection"
            } else {
                ConnectionLogManager.log(LogType.ERROR, "WatchTest", "Ping timeout")
                "❌ Timeout - Watch may be offline"
            }
            
        } catch (e: Exception) {
            ConnectionLogManager.log(LogType.ERROR, "WatchTest", "Test failed: ${e.message}")
            "❌ Error: ${e.message}"
        }
    }
}
