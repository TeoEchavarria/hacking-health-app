package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Wearable Device Status Card
 * 
 * Shows:
 * - Watch icon in circular container
 * - Device name and model
 * - Animated connection status badge (green pulse when connected)
 */
@Composable
fun WearableStatusCard(
    deviceName: String = "Dispositivo Wearable",
    deviceModel: String = "Reloj Inteligente Pro",
    isConnected: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SandboxSurfaceContainerLowest
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Device Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Watch Icon Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SandboxPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = SandboxPrimary
                    )
                }
                
                // Device Info
                Column {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SandboxOnSurface
                    )
                    Text(
                        text = deviceModel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = SandboxOnSurfaceVariant
                    )
                }
            }
            
            // Right: Connection Status Badge
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                ConnectionStatusBadge(isConnected = isConnected)
            }
        }
    }
}

@Composable
private fun ConnectionStatusBadge(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isConnected) SandboxConnectedGreenLight else SandboxSurfaceVariant
    val borderColor = if (isConnected) SandboxConnectedGreenBorder else SandboxOutlineVariant
    val dotColor = if (isConnected) SandboxConnectedGreen else SandboxOnSurfaceVariant
    val text = if (isConnected) "Conectado" else "Desconectado"
    
    // Ping animation for the dot when connected
    val infiniteTransition = rememberInfiniteTransition(label = "ping")
    val pingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingScale"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingAlpha"
    )
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Animated dot
        Box(contentAlignment = Alignment.Center) {
            // Ping effect (only when connected)
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(pingScale)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = pingAlpha))
                )
            }
            // Static dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        
        // Status text
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isConnected) SandboxConnectedGreen else SandboxOnSurfaceVariant,
            letterSpacing = 0.5.sp
        )
    }
}
