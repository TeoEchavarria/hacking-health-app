package com.samsung.android.health.sdk.sample.healthdiary.views

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.MedicalDocumentEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.SensorBatchEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.UploadLogEntity
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogEntry
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogsViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogsUiState
import com.samsung.android.health.sdk.sample.healthdiary.worker.UploadHealthMonitor
import com.samsung.android.health.sdk.sample.healthdiary.worker.UploadHealthState
import com.samsung.android.health.sdk.sample.healthdiary.worker.UploadScheduler
import com.samsung.android.health.sdk.sample.healthdiary.worker.UploadWatchdog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadHealthState by UploadHealthMonitor.healthState.collectAsState()
    val pagerState = rememberPagerState { 5 }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val titles = listOf("Health", "System", "Sensor Data", "Upload Logs", "Documents")

    Scaffold(
        topBar = {
            SandboxTopBar(
                title = "Logs & Documents",
                onNavigationClick = onNavigateBack,
                actions = {
                    SandboxIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.syncPendingData() },
                        contentDescription = "Sync"
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            SandboxTabRow(
                selectedTabIndex = pagerState.currentPage,
                tabs = titles,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )

            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> UploadHealthPanel(uploadHealthState, context)
                    1 -> SystemLogList(uiState.phoneLogs) // Phone system logs
                    2 -> SensorDataTab(uiState) // Watch sensor data logs + stats
                    3 -> UploadLogsTab(uiState.apiLogs) // API upload logs
                    4 -> DocumentList(uiState.medicalDocuments)
                }
            }
        }
    }
}

@Composable
fun SystemLogList(logs: List<LogEntry>) {
    if (logs.isEmpty()) {
        EmptyTabContent("No system logs yet", "Phone system events will appear here")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                ListItem(
                    headlineContent = { Text(log.event, fontWeight = FontWeight.Medium) },
                    supportingContent = { 
                        Text(log.details) 
                    },
                    trailingContent = {
                        Text(log.getFormattedTime(), style = MaterialTheme.typography.bodySmall)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color(0xFFE8F5E9) // Green for phone
                    )
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun SensorDataTab(uiState: LogsUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats Card
        item {
            SandboxCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sensor Data Queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.pendingSensorCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.pendingSensorCount > 0) Color(0xFFFF5722) else Color(0xFF4CAF50)
                        )
                        Text("Pending", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.totalSensorCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Total", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        // Watch logs header
        item {
            Text(
                text = "Watch → Phone Events",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Watch logs
        if (uiState.watchLogs.isEmpty()) {
            item {
                SandboxEmptyState(
                    title = "No sensor data received from watch yet",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(uiState.watchLogs) { log ->
                ListItem(
                    headlineContent = { Text(log.event, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(log.details) },
                    trailingContent = {
                        Text(log.getFormattedTime(), style = MaterialTheme.typography.bodySmall)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color(0xFFE3F2FD) // Blue for watch
                    )
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun UploadLogsTab(apiLogs: List<LogEntry>) {
    if (apiLogs.isEmpty()) {
        EmptyTabContent("No upload logs yet", "API upload attempts will appear here")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apiLogs) { log ->
                val isSuccess = log.event.contains("Success", ignoreCase = true)
                val isError = log.event.contains("Error", ignoreCase = true) || 
                              log.event.contains("Failed", ignoreCase = true)
                
                ListItem(
                    headlineContent = { 
                        Text(
                            log.event, 
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isSuccess -> Color(0xFF4CAF50)
                                isError -> Color(0xFFF44336)
                                else -> Color.Unspecified
                            }
                        ) 
                    },
                    supportingContent = { Text(log.details) },
                    trailingContent = {
                        Text(log.getFormattedTime(), style = MaterialTheme.typography.bodySmall)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = when {
                            isSuccess -> Color(0xFFE8F5E9) // Green
                            isError -> Color(0xFFFFEBEE) // Red
                            else -> Color(0xFFFFF3E0) // Orange
                        }
                    )
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EmptyTabContent(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SandboxEmptyState(
            title = title,
            message = subtitle
        )
    }
}

@Composable
fun DocumentList(documents: List<MedicalDocumentEntity>) {
    if (documents.isEmpty()) {
        EmptyTabContent("No documents yet", "Medical documents will appear here")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(documents) { doc ->
                ListItem(
                    headlineContent = { Text(doc.filename) },
                    supportingContent = { 
                        Text("Size: ${doc.fileSize / 1024} KB | Uploaded: ${formatTime(doc.uploadTimestamp)}") 
                    },
                    trailingContent = {
                        if (doc.processed) {
                            Text("Processed", color = Color.Green)
                        } else {
                            Text("Unprocessed", color = Color.Gray)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun UploadHealthPanel(state: UploadHealthState, context: Context) {
    val now = System.currentTimeMillis()
    val isStalled = UploadHealthMonitor.isStalled()
    val isCircuitOpen = UploadHealthMonitor.isCircuitOpen()
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Card
        item {
            SandboxCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCircuitOpen || isStalled) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = if (isCircuitOpen) Color.Red else Color(0xFFF57C00)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when {
                            isCircuitOpen -> "⚠️ Circuit Breaker OPEN"
                            isStalled -> "⚠️ Upload Pipeline STALLED"
                            else -> "✅ Upload Pipeline HEALTHY"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isCircuitOpen) {
                    Text(
                        text = "Too many consecutive failures. Manual intervention may be required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
                if (isStalled) {
                    Text(
                        text = "No upload attempts detected recently. Watchdog will attempt recovery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF57C00)
                    )
                }
            }
        }
        
        // Metrics Card
        item {
            SandboxCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Upload Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                MetricRow("Pending Records", "${state.pendingRecordCount}")
                MetricRow("Total Sent", "${state.totalRecordsSent}")
                MetricRow("Consecutive Failures", "${state.consecutiveFailures}")
                
                if (state.lastAttemptTime > 0) {
                    MetricRow("Last Attempt", formatTimeAgo(now - state.lastAttemptTime))
                }
                if (state.lastSuccessTime > 0) {
                    MetricRow("Last Success", formatTimeAgo(now - state.lastSuccessTime))
                }
                if (state.lastFailureTime > 0) {
                    MetricRow("Last Failure", formatTimeAgo(now - state.lastFailureTime))
                    if (state.lastFailureReason.isNotEmpty()) {
                        MetricRow("Failure Reason", state.lastFailureReason)
                    }
                }
                if (state.lastScheduledTime > 0) {
                    MetricRow("Last Scheduled", formatTimeAgo(now - state.lastScheduledTime))
                }
            }
        }
        
        // Actions Card
        item {
            SandboxCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SandboxButton(
                        text = "Force Upload",
                        onClick = { UploadScheduler.forceImmediateUpload(context) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    SandboxButton(
                        text = "Force Check",
                        onClick = { UploadWatchdog.forceCheck() },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isCircuitOpen) {
                    SandboxButton(
                        text = "Reset Circuit Breaker",
                        onClick = { UploadHealthMonitor.resetCircuitBreaker() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Secondary
                    )
                }
            }
        }
        
        // Health Summary
        item {
            SandboxCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Diagnostic Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = UploadHealthMonitor.getHealthSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTimeAgo(durationMs: Long): String {
    val seconds = durationMs / 1000
    return when {
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s ago"
        seconds < 86400 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m ago"
        else -> "${seconds / 86400}d ago"
    }
}
