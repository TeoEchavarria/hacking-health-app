package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.HeadlineFont
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxPrimary
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxSecondaryContainer

/**
 * Tu Salud Top App Bar
 * 
 * Fixed header with:
 * - User avatar (circular)
 * - "Tu Salud" brand name
 * - Connection status indicator
 * - Sensors/sync button on the right
 */
@Composable
fun TuSaludTopBar(
    userName: String = "",
    isConnected: Boolean = false,
    onSensorsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 2.dp),
        color = Color.White.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User Avatar - clickeable to open profile
                IconButton(
                    onClick = onAvatarClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(SandboxSecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SandboxPrimary
                        )
                    }
                }
                
                // Brand Name
                Text(
                    text = "Tu Salud",
                    fontFamily = HeadlineFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = SandboxPrimary,
                    letterSpacing = (-0.5).sp
                )
            }
            
            // Right: Connection Status + Sensors Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Indicator
                ConnectionStatusBadge(isConnected = isConnected)
                
                // Sensors Button
                IconButton(
                    onClick = onSensorsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Sensors",
                        tint = SandboxPrimary
                    )
                }
            }
        }
    }
}

/**
 * Connection Status Badge
 * Shows a pulsing green dot with "Conectado" text when connected
 */
@Composable
private fun ConnectionStatusBadge(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isConnected) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0FFF4)) // green-50
            .border(1.dp, Color(0xFFD1F4DA), RoundedCornerShape(12.dp)) // green-100
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulsing dot
        Box(contentAlignment = Alignment.Center) {
            // Pulse ring
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha)) // green-400
            )
            // Solid dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)) // green-500
            )
        }
        
        // Status text
        Text(
            text = "CONECTADO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF15803D), // green-700
            letterSpacing = 0.5.sp
        )
    }
}
