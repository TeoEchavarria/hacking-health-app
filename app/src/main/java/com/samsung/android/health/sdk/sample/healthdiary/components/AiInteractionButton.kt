package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * AI Interaction Button
 * 
 * Central interaction area with:
 * - Greeting message with user's name
 * - Large circular microphone button (192dp)
 * - "Escuchar AI" label
 * - Optional waveform animation (for future use)
 */
@Composable
fun AiInteractionButton(
    userName: String = "",
    isListening: Boolean = false,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale animation when pressed
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Greeting Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (userName.isNotEmpty()) {
                    "¿Cómo te sientes, $userName?"
                } else {
                    "¿Cómo te sientes?"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SandboxOnSurface,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )
            
            Text(
                text = "Pulsa el botón para hablar conmigo. Estoy aquí para escucharte.",
                style = MaterialTheme.typography.bodyMedium,
                color = SandboxOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )
        }
        
        // Waveform (shown when listening - placeholder for now)
        if (isListening) {
            WaveformAnimation(modifier = Modifier.height(40.dp))
        }
        
        // Large Circular Button
        Box(
            modifier = Modifier
                .scale(scale)
                .size(192.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = SandboxPrimary.copy(alpha = 0.12f),
                    spotColor = SandboxPrimary.copy(alpha = 0.12f)
                )
                .clip(CircleShape)
                .background(Color.White)
                .border(8.dp, SandboxSurfaceContainerLow, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTap
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner pressed overlay
            if (isPressed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SandboxPrimary.copy(alpha = 0.05f))
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mic icon container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = SandboxPrimary.copy(alpha = 0.3f),
                            spotColor = SandboxPrimary.copy(alpha = 0.3f)
                        )
                        .clip(CircleShape)
                        .background(SandboxPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Hablar con AI",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Label
                Text(
                    text = "Escuchar AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SandboxPrimary,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

/**
 * Waveform animation component (placeholder)
 */
@Composable
private fun WaveformAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val heightFraction by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SandboxPrimary)
            )
        }
    }
}
