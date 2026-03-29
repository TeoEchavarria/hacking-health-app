package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Biometric Compact Card
 * 
 * Compact horizontal card showing biometric metric with:
 * - Icon and background color
 * - Metric name and status badge
 * - Value and trend indicator
 */
@Composable
fun BiometricCompactCard(
    icon: String,
    metricName: String,
    value: String,
    status: BiometricStatus,
    statusLabel: String,
    trend: String? = null,
    backgroundColor: Color = SandboxPrimaryFixed,
    iconColor: Color = SandboxPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        border = when (status) {
            BiometricStatus.CRITICAL -> androidx.compose.foundation.BorderStroke(2.dp, SandboxError.copy(alpha = 0.3f))
            else -> androidx.compose.foundation.BorderStroke(1.dp, SandboxSurfaceContainer)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header row: Metric name + Status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = metricName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SandboxSecondary,
                        letterSpacing = 1.2.sp,
                        fontSize = 10.sp
                    )
                    
                    StatusBadge(status = status, label = statusLabel)
                }
                
                // Value row
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (status) {
                            BiometricStatus.CRITICAL -> SandboxError
                            else -> SandboxOnSurface
                        }
                    )
                    
                    if (trend != null) {
                        Text(
                            text = "• $trend",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (status) {
                                BiometricStatus.CRITICAL -> SandboxError.copy(alpha = 0.6f)
                                else -> SandboxSecondary
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: BiometricStatus,
    label: String
) {
    val (backgroundColor, textColor) = when (status) {
        BiometricStatus.OPTIMAL -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        BiometricStatus.WARNING -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        BiometricStatus.CRITICAL -> SandboxErrorContainer.copy(alpha = 0.3f) to SandboxError
        BiometricStatus.NORMAL -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Biometric status levels
 */
enum class BiometricStatus {
    OPTIMAL,
    NORMAL,
    WARNING,
    CRITICAL
}
