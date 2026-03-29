package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Map Mock View
 * 
 * Displays a static map background with:
 * - Gradient overlay for content readability
 * - Animated pulse indicator at center
 * - User avatar at pulse center
 * 
 * Uses a placeholder image or remote map tile as background.
 */
@Composable
fun MapMockView(
    userAvatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Map (static image)
        // Using a placeholder gray background that represents a map
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8EAF0),
                            Color(0xFFF0F1F5),
                            Color(0xFFE8EAF0)
                        )
                    )
                )
        )
        
        // Optional: Load actual map tile image
        // Uncomment to use real map image
        /*
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://api.mapbox.com/styles/v1/mapbox/light-v10/static/-3.6844,40.4153,13,0/400x600@2x?access_token=YOUR_TOKEN")
                .crossfade(true)
                .build(),
            contentDescription = "Map",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(1.dp), // Slight blur for aesthetic
            alpha = 0.6f
        )
        */
        
        // Gradient Overlay (fade top/bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SandboxBackground.copy(alpha = 0.8f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            SandboxBackground.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        
        // Pulse Indicator at Center
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LocationPulseIndicator(userAvatarUrl = userAvatarUrl)
        }
    }
}

/**
 * Location Pulse Indicator
 * 
 * Animated pulse rings around user avatar
 */
@Composable
private fun LocationPulseIndicator(
    userAvatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulse ring
        Box(
            modifier = Modifier
                .size(96.dp * scale)
                .clip(CircleShape)
                .background(SandboxPrimary.copy(alpha = alpha * 0.3f))
        )
        
        // Avatar/Pin Container
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(4.dp, SandboxPrimary)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (userAvatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userAvatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    // Placeholder avatar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SandboxPrimary.copy(alpha = 0.1f))
                    )
                }
            }
        }
    }
}
