package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HeartRateHistoryViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HourlyHeartRateData
import com.samsung.android.health.sdk.sample.healthdiary.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HeartRateHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Historial de Frecuencia Cardíaca")
                        uiState.patientName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Actualizar")
                        }
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
            } else if (uiState.hourlyData.isEmpty() && uiState.dataPoints.isEmpty()) {
                SandboxCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No hay datos de frecuencia cardíaca disponibles para el período seleccionado.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                SandboxCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Frecuencia Cardíaca por Hora",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        HourlyHeartRateChart(data = uiState.hourlyData)
                        
                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            ChartLegendItem(color = Color(0xFFE91E63), label = "Rango (Mín - Máx)")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hourly heart rate chart with vertical bars showing min-max range.
 * X-axis: Hours (00:00 - 23:00)
 * Y-axis: BPM values
 */
@Composable
private fun HourlyHeartRateChart(data: List<HourlyHeartRateData>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        val leftPadding = 50f  // Space for Y-axis labels
        val rightPadding = 20f
        val topPadding = 20f
        val bottomPadding = 50f // Space for X-axis labels
        
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding
        
        // Calculate BPM range
        val allMinBpm = data.minOfOrNull { it.minBpm } ?: 50
        val allMaxBpm = data.maxOfOrNull { it.maxBpm } ?: 120
        val bpmRange = (allMaxBpm - allMinBpm).coerceAtLeast(20)
        val bpmMin = (allMinBpm - 10).coerceAtLeast(40)
        val bpmMax = allMaxBpm + 10
        val adjustedRange = bpmMax - bpmMin
        
        // Draw Y-axis
        drawLine(
            color = Color.Gray,
            start = Offset(leftPadding, topPadding),
            end = Offset(leftPadding, topPadding + chartHeight),
            strokeWidth = 2f
        )
        
        // Draw X-axis
        drawLine(
            color = Color.Gray,
            start = Offset(leftPadding, topPadding + chartHeight),
            end = Offset(leftPadding + chartWidth, topPadding + chartHeight),
            strokeWidth = 2f
        )
        
        // Draw Y-axis labels (BPM values)
        val yLabelCount = 5
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 28f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        
        for (i in 0..yLabelCount) {
            val bpmValue = bpmMin + (adjustedRange * i / yLabelCount)
            val y = topPadding + chartHeight - (chartHeight * i / yLabelCount)
            
            // Draw horizontal grid line
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + chartWidth, y),
                strokeWidth = 1f
            )
            
            // Draw label
            drawContext.canvas.nativeCanvas.drawText(
                "$bpmValue",
                leftPadding - 8f,
                y + 10f,
                paint
            )
        }
        
        // Draw X-axis labels (hours) and bars
        val xLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        // Create 24-hour slots
        val barWidth = chartWidth / 24f
        val barColor = Color(0xFFE91E63) // Pink/Red for heart rate
        
        for (hour in 0..23) {
            val x = leftPadding + (hour * barWidth) + (barWidth / 2)
            
            // Draw X-axis label every 4 hours
            if (hour % 4 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%02d:00", hour),
                    x,
                    topPadding + chartHeight + 35f,
                    xLabelPaint
                )
            }
            
            // Find data for this hour
            val hourData = data.find { it.hourIndex == hour }
            
            if (hourData != null) {
                // Calculate Y positions for min and max
                val yMax = topPadding + chartHeight - ((hourData.maxBpm - bpmMin).toFloat() / adjustedRange * chartHeight)
                val yMin = topPadding + chartHeight - ((hourData.minBpm - bpmMin).toFloat() / adjustedRange * chartHeight)
                
                // Draw vertical bar (min to max)
                drawLine(
                    color = barColor,
                    start = Offset(x, yMax),
                    end = Offset(x, yMin),
                    strokeWidth = 8f
                )
                
                // Draw caps on the bar
                drawLine(
                    color = barColor,
                    start = Offset(x - 4f, yMax),
                    end = Offset(x + 4f, yMax),
                    strokeWidth = 3f
                )
                drawLine(
                    color = barColor,
                    start = Offset(x - 4f, yMin),
                    end = Offset(x + 4f, yMin),
                    strokeWidth = 3f
                )
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
