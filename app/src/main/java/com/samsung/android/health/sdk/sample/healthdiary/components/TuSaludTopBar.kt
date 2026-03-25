package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
 * - Sensors/sync button on the right
 */
@Composable
fun TuSaludTopBar(
    userName: String = "",
    onSensorsClick: () -> Unit = {},
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
                // User Avatar - placeholder with initial
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
            
            // Right: Sensors Button
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
