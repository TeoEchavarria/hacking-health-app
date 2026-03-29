package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.model.DaySummary

/**
 * Day Summary Card
 * 
 * Shows a summary of daily activity with:
 * - Title and description
 * - Grid with steps and sleep data
 * - Glass effect containers
 * - Decorative blur circle
 */
@Composable
fun DaySummaryCard(
    summary: DaySummary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(SandboxPrimary)
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .size(256.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 64.dp, y = 64.dp)
                .blur(48.dp)
                .background(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(128.dp)
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Title
            Text(
                text = "Resumen del Día",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = summary.description,
                style = MaterialTheme.typography.bodyMedium,
                color = SandboxPrimaryFixed.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Activity (Steps)
                SummaryStatItem(
                    label = "Actividad",
                    value = summary.steps?.let { 
                        String.format("%,d pasos", it) 
                    } ?: "Sin datos",
                    modifier = Modifier.weight(1f)
                )
                
                // Sleep
                SummaryStatItem(
                    label = "Sueño",
                    value = summary.sleepHours?.let {
                        val hours = it.toInt()
                        val minutes = ((it - hours) * 60).toInt()
                        "${hours}h ${minutes}m"
                    } ?: "Sin datos",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
