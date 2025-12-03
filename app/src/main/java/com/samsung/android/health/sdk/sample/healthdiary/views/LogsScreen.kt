package com.samsung.android.health.sdk.sample.healthdiary.views

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.MedicalDocumentEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.SensorBatchEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.UploadLogEntity
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogsViewModel
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
    val pagerState = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    val titles = listOf("System", "Sensor Data", "Upload Logs", "Documents")

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
                    0 -> SystemLogList(uiState.systemLogs)
                    1 -> SensorBatchList(uiState.sensorBatches)
                    2 -> UploadLogList(uiState.uploadLogs)
                    3 -> DocumentList(uiState.medicalDocuments)
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
