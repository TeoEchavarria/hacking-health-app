package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.model.LocationData
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Map Preview Card
 * 
 * Shows a visual map preview with the current GPS location.
 * Clickable to navigate to full map/tracking screen.
 */
@Composable
fun MapPreviewCard(
    location: LocationData?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(RoundedCornerShape(32.dp))
            .shadow(2.dp, RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
    ) {
        // Map background with visual representation
        if (location != null) {
            MapVisualBackground(
                latitude = location.latitude,
                longitude = location.longitude
            )
        } else {
            // Placeholder when no location
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SandboxSurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SandboxSecondary
                    )
                    Text(
                        text = "Ubicación no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SandboxSecondary
                    )
                }
            }
        }
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )
        
        // Content overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Explore icon (top right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(8.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Explorar mapa",
                    tint = SandboxPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Location info (bottom)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Column {
                        Text(
                            text = location?.address ?: "Ubicación GPS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Text(
                            text = if (location != null) {
                                "Última actualización: ${location.getTimeAgo()}"
                            } else {
                                "Sin datos de ubicación"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // "Rastrear" badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Rastrear",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Visual map background using Canvas drawing
 * Creates a stylized map appearance without requiring external APIs
 */
@Composable
private fun MapVisualBackground(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF42A5F5)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Draw "streets" as a grid pattern
            val gridColor = Color.White.copy(alpha = 0.3f)
            val gridSpacing = 60f
            
            // Vertical lines
            var x = (longitude % 1 * gridSpacing).toFloat()
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 2f
                )
                x += gridSpacing
            }
            
            // Horizontal lines
            var y = (latitude % 1 * gridSpacing).toFloat()
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 2f
                )
                y += gridSpacing
            }
            
            // Draw curved "roads"
            val roadColor = Color.White.copy(alpha = 0.4f)
            val path = Path()
            
            // Curved road 1
            path.moveTo(0f, height * 0.3f)
            path.cubicTo(
                width * 0.3f, height * 0.2f,
                width * 0.6f, height * 0.5f,
                width, height * 0.4f
            )
            drawPath(
                path = path,
                color = roadColor,
                style = Stroke(width = 4f)
            )
            
            // Curved road 2
            val path2 = Path()
            path2.moveTo(0f, height * 0.7f)
            path2.cubicTo(
                width * 0.4f, height * 0.8f,
                width * 0.7f, height * 0.6f,
                width, height * 0.75f
            )
            drawPath(
                path = path2,
                color = roadColor,
                style = Stroke(width = 4f)
            )
            
            // Draw location marker (red pin)
            val markerX = width / 2
            val markerY = height / 2
            val markerRadius = 12f
            
            // Outer glow
            drawCircle(
                color = Color.Red.copy(alpha = 0.3f),
                radius = markerRadius * 2,
                center = Offset(markerX, markerY)
            )
            
            // Main marker
            drawCircle(
                color = Color.Red,
                radius = markerRadius,
                center = Offset(markerX, markerY)
            )
            
            // Inner dot
            drawCircle(
                color = Color.White,
                radius = markerRadius * 0.4f,
                center = Offset(markerX, markerY)
            )
            
            // Draw "buildings" as small rectangles
            val buildingColor = Color.White.copy(alpha = 0.2f)
            for (i in 0..8) {
                val bx = (width * 0.15f) + (i % 3) * (width * 0.3f)
                val by = (height * 0.2f) + (i / 3) * (height * 0.25f)
                drawRect(
                    color = buildingColor,
                    topLeft = Offset(bx, by),
                    size = androidx.compose.ui.geometry.Size(25f, 25f)
                )
            }
        }
    }
}
