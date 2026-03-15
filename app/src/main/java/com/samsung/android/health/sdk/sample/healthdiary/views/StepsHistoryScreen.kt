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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.StepsHistoryViewModel
import com.samsung.android.health.sdk.sample.healthdiary.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsHistoryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { StepsHistoryViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steps History") },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SandboxCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.totalSteps}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "Total Steps",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    SandboxCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.averageSteps}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF)
                            )
                            Text(
                                text = "Daily Average",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
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
                        text = "No steps data available for the selected period.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                SandboxCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Daily Steps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        StepsBarChart(data = uiState.dataPoints)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsBarChart(data: List<com.samsung.android.health.sdk.sample.healthdiary.viewmodel.StepsDataPoint>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(vertical = 16.dp)
    ) {
        if (data.isEmpty()) return@Canvas
        
        val maxSteps = data.maxOfOrNull { it.steps } ?: 1000
        val padding = 40f
        val chartHeight = size.height - padding * 2
        val barWidth = (size.width - padding * 2) / data.size * 0.7f
        val spacing = (size.width - padding * 2) / data.size
        
        data.forEachIndexed { index, point ->
            val barHeight = (point.steps.toFloat() / maxSteps * chartHeight).coerceAtLeast(2f)
            val x = padding + index * spacing + (spacing - barWidth) / 2
            val y = size.height - padding - barHeight
            
            drawRoundRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
        
        // Goal line (10,000 steps)
        val goalY = size.height - padding - (10000f / maxSteps * chartHeight)
        if (goalY > padding) {
            drawLine(
                color = Color(0xFFFF9800),
                start = Offset(padding, goalY),
                end = Offset(size.width - padding, goalY),
                strokeWidth = 2f
            )
        }
    }
}
