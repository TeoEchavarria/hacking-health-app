package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.SleepHistoryViewModel
import com.samsung.android.health.sdk.sample.healthdiary.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepHistoryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { SleepHistoryViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Range Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SandboxButton(
                    text = "7 Days",
                    onClick = { viewModel.setRange(7) },
                    modifier = Modifier.weight(1f),
                    variant = if (uiState.selectedRange == 7) ButtonVariant.Primary else ButtonVariant.Secondary
                )
                SandboxButton(
                    text = "14 Days",
                    onClick = { viewModel.setRange(14) },
                    modifier = Modifier.weight(1f),
                    variant = if (uiState.selectedRange == 14) ButtonVariant.Primary else ButtonVariant.Secondary
                )
                SandboxButton(
                    text = "30 Days",
                    onClick = { viewModel.setRange(30) },
                    modifier = Modifier.weight(1f),
                    variant = if (uiState.selectedRange == 30) ButtonVariant.Primary else ButtonVariant.Secondary
                )
            }
            
            // Summary Stats
            if (!uiState.isLoading && uiState.dataPoints.isNotEmpty()) {
                SandboxCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f hours", uiState.averageHours),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            text = "Average Sleep",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Chart
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SandboxLoader()
                }
            } else if (uiState.error != null) {
                SandboxCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (uiState.dataPoints.isEmpty()) {
                SandboxCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No sleep data available for the selected period.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                SandboxCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Sleep Duration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        SleepLineChart(data = uiState.dataPoints)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepLineChart(data: List<com.samsung.android.health.sdk.sample.healthdiary.viewmodel.SleepDataPoint>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(vertical = 16.dp)
    ) {
        if (data.isEmpty()) return@Canvas
        
        val maxHours = data.maxOfOrNull { it.hours } ?: 12f
        val padding = 40f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2
        val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)
        
        // Draw line
        val path = Path()
        data.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val y = size.height - padding - (point.hours / maxHours * chartHeight)
            
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFF9C27B0), style = Stroke(width = 3f))
        
        // Draw data points
        data.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val y = size.height - padding - (point.hours / maxHours * chartHeight)
            
            drawCircle(
                color = Color(0xFF9C27B0),
                radius = 5f,
                center = Offset(x, y)
            )
        }
        
        // Goal line (8 hours)
        val goalY = size.height - padding - (8f / maxHours * chartHeight)
        if (goalY > padding && goalY < size.height - padding) {
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(padding, goalY),
                end = Offset(size.width - padding, goalY),
                strokeWidth = 2f
            )
        }
    }
}
