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
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HeartRateHistoryViewModel
import com.samsung.android.health.sdk.sample.healthdiary.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateHistoryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { HeartRateHistoryViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Heart Rate History") },
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
            // Header: Últimas 24 horas
            Text(
                text = "Últimas 24 horas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
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
                        text = "No heart rate data available for the selected period.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                SandboxCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Average Heart Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        HeartRateChart(data = uiState.dataPoints)
                        
                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ChartLegendItem(color = Color(0xFFFF5252), label = "Max")
                            ChartLegendItem(color = Color(0xFF4CAF50), label = "Avg")
                            ChartLegendItem(color = Color(0xFF64B5F6), label = "Min")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartRateChart(data: List<com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HeartRateDataPoint>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(vertical = 16.dp)
    ) {
        if (data.isEmpty()) return@Canvas
        
        val maxBpm = data.mapNotNull { it.maxBpm }.maxOrNull() ?: 100
        val minBpm = data.mapNotNull { it.minBpm }.minOrNull() ?: 50
        val range = (maxBpm - minBpm).coerceAtLeast(20)
        
        val padding = 40f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2
        val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)
        
        // Draw avg line
        val avgPath = Path()
        data.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val y = if (point.avgBpm != null) {
                size.height - padding - ((point.avgBpm!! - minBpm).toFloat() / range * chartHeight)
            } else return@forEachIndexed
            
            if (index == 0) avgPath.moveTo(x, y) else avgPath.lineTo(x, y)
        }
        drawPath(avgPath, Color(0xFF4CAF50), style = Stroke(width = 3f))
        
        // Draw data points
        data.forEachIndexed { index, point ->
            val x = padding + index * stepX
            
            point.maxBpm?.let { max ->
                val y = size.height - padding - ((max - minBpm).toFloat() / range * chartHeight)
                drawCircle(Color(0xFFFF5252), radius = 4f, center = Offset(x, y))
            }
            point.minBpm?.let { min ->
                val y = size.height - padding - ((min - minBpm).toFloat() / range * chartHeight)
                drawCircle(Color(0xFF64B5F6), radius = 4f, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .padding(2.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color)
            }
        }
        Text(text = label, fontSize = 12.sp)
    }
}
