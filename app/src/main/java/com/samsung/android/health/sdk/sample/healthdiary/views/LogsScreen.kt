package com.samsung.android.health.sdk.sample.healthdiary.views

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.MedicalDocumentEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.SensorBatchEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.UploadLogEntity
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogsViewModel
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
            TopAppBar(
                title = { Text("Logs & Documents") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncPendingData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> UploadHealthPanel(uploadHealthState, context)
                    1 -> SystemLogList(uiState.systemLogs)
                    2 -> SensorBatchList(uiState.sensorBatches)
                    3 -> UploadLogList(uiState.uploadLogs)
                    4 -> DocumentList(uiState.medicalDocuments)
                }
            }
        }
    }
}

@Composable
fun SystemLogList(logs: List<com.samsung.android.health.sdk.sample.healthdiary.utils.LogEntry>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logs) { log ->
            ListItem(
                headlineContent = { Text("[${log.source}] ${log.event}") },
                supportingContent = { 
                    Text(log.details) 
                },
                trailingContent = {
                    Text(log.getFormattedTime(), style = MaterialTheme.typography.bodySmall)
                },
                colors = ListItemDefaults.colors(
                    containerColor = when (log.source) {
                        "WATCH" -> Color(0xFFE3F2FD)
                        "PHONE" -> Color(0xFFE8F5E9)
                        "API" -> Color(0xFFFFF3E0)
                        else -> Color.Transparent
                    }
                )
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun SensorBatchList(batches: List<SensorBatchEntity>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(batches) { batch ->
            ListItem(
                headlineContent = { Text("Sensor: ${batch.sensorType}") },
                supportingContent = { 
                    Text("Samples: ${batch.sampleCount} | Time: ${formatTime(batch.timestamp)}") 
                },
                trailingContent = {
                    if (batch.uploaded) {
                        Text("Uploaded", color = Color.Green)
                    } else {
                        Text("Pending", color = Color.Red)
                    }
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun UploadLogList(logs: List<UploadLogEntity>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logs) { log ->
            ListItem(
                headlineContent = { Text("Status: ${log.status}") },
                supportingContent = { 
                    Text("Entity: ${log.entityType} | Time: ${formatTime(log.timestamp)}") 
                },
                trailingContent = {
                    Text("${log.responseCode ?: "N/A"}")
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun DocumentList(documents: List<MedicalDocumentEntity>) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCircuitOpen -> Color(0xFFFFCDD2) // Red
                        isStalled -> Color(0xFFFFF9C4) // Yellow
                        else -> Color(0xFFC8E6C9) // Green
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
        }
        
        // Metrics Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
        }
        
        // Actions Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                        Button(
                            onClick = { UploadScheduler.forceImmediateUpload(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Force Upload")
                        }
                        
                        Button(
                            onClick = { UploadWatchdog.forceCheck() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Force Check")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isCircuitOpen) {
                        Button(
                            onClick = { UploadHealthMonitor.resetCircuitBreaker() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                        ) {
                            Text("Reset Circuit Breaker")
                        }
                    }
                }
            }
        }
        
        // Health Summary
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
