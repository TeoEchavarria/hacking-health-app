package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BiometricReading
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthAlertStatus

/**
 * Biometric type for styling
 */
enum class BiometricType {
    SPO2,
    BLOOD_PRESSURE,
    TEMPERATURE,
    HEART_RATE
}

/**
 * Biometric Card
 * 
 * Compact card displaying a single biometric reading with:
 * - Icon in colored container
 * - Label + status badge
 * - Value + secondary text
 * - Border color changes based on status
 */
@Composable
fun BiometricCard(
    type: BiometricType,
    label: String,
    reading: BiometricReading,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val icon = getIconForType(type)
    val iconContainerColor = getIconContainerColor(type, reading.status)
    val iconTint = getIconTint(type, reading.status)
    val borderColor = getBorderColor(reading.status)
    val badgeColors = getBadgeColors(reading.status)
    val valueColor = getValueColor(reading.status)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (reading.status == HealthAlertStatus.CRITICAL) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Label + Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SandboxSecondary,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )
                    
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(badgeColors.first)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = reading.statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColors.second,
                            fontSize = 10.sp
                        )
                    }
                }
                
                // Value + Secondary Text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = reading.value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = valueColor,
                        fontSize = 18.sp
                    )
                    
                    Text(
                        text = "• ${reading.secondaryText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reading.status == HealthAlertStatus.CRITICAL) {
                            valueColor.copy(alpha = 0.6f)
                        } else {
                            SandboxSecondary
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun getIconForType(type: BiometricType): ImageVector {
    return when (type) {
        BiometricType.SPO2 -> Icons.Default.Air
        BiometricType.BLOOD_PRESSURE -> Icons.Default.Speed
        BiometricType.TEMPERATURE -> Icons.Default.DeviceThermostat
        BiometricType.HEART_RATE -> Icons.Default.Favorite
    }
}

@Composable
private fun getIconContainerColor(type: BiometricType, status: HealthAlertStatus): Color {
    return when {
        status == HealthAlertStatus.CRITICAL -> SandboxErrorContainer.copy(alpha = 0.4f)
        type == BiometricType.SPO2 -> SandboxPrimaryFixed.copy(alpha = 0.3f)
        type == BiometricType.BLOOD_PRESSURE -> SandboxTertiaryFixed.copy(alpha = 0.3f)
        type == BiometricType.TEMPERATURE -> Color(0xFFDBEAFE) // Light blue
        type == BiometricType.HEART_RATE -> SandboxTertiaryFixed.copy(alpha = 0.3f)
        else -> SandboxSurfaceVariant
    }
}

@Composable
private fun getIconTint(type: BiometricType, status: HealthAlertStatus): Color {
    return when {
        status == HealthAlertStatus.CRITICAL -> SandboxError
        type == BiometricType.SPO2 -> SandboxPrimary
        type == BiometricType.BLOOD_PRESSURE -> SandboxTertiary
        type == BiometricType.TEMPERATURE -> Color(0xFF1D4ED8) // Blue-700
        type == BiometricType.HEART_RATE -> SandboxTertiary
        else -> SandboxOnSurfaceVariant
    }
}

@Composable
private fun getBorderColor(status: HealthAlertStatus): Color {
    return when (status) {
        HealthAlertStatus.OPTIMAL -> SandboxSurfaceContainer
        HealthAlertStatus.WARNING -> SandboxWarning.copy(alpha = 0.3f)
        HealthAlertStatus.CRITICAL -> SandboxError.copy(alpha = 0.3f)
    }
}

@Composable
private fun getBadgeColors(status: HealthAlertStatus): Pair<Color, Color> {
    return when (status) {
        HealthAlertStatus.OPTIMAL -> Pair(SandboxSuccessLight, SandboxSuccessDark)
        HealthAlertStatus.WARNING -> Pair(SandboxWarningLight, SandboxWarningDark)
        HealthAlertStatus.CRITICAL -> Pair(SandboxErrorContainer.copy(alpha = 0.2f), SandboxError)
    }
}

@Composable
private fun getValueColor(status: HealthAlertStatus): Color {
    return when (status) {
        HealthAlertStatus.CRITICAL -> SandboxError
        else -> SandboxOnSurface
    }
}
