package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthTip

/**
 * Health Tip Card
 * 
 * Displays personalized health tips with a gradient background,
 * tip icon, description, and action button.
 */
@Composable
fun HealthTipCard(
    tip: HealthTip,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        SandboxPrimaryFixed.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        // Decorative circle (blur effect)
        Box(
            modifier = Modifier
                .size(128.dp)
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = (-32).dp)
                .blur(40.dp)
                .background(
                    color = SandboxPrimary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(64.dp)
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header: Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SandboxPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column {
                    Text(
                        text = tip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SandboxPrimary
                    )
                    if (tip.subtitle.isNotEmpty()) {
                        Text(
                            text = tip.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = SandboxSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = tip.description,
                style = MaterialTheme.typography.bodyMedium,
                color = SandboxOnSurfaceVariant,
                lineHeight = 22.sp,
                modifier = Modifier.widthIn(max = 320.dp)
            )
            
            // Action Button (only show if actionText is not empty)
            if (tip.actionText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = SandboxPrimary.copy(alpha = 0.2f),
                            spotColor = SandboxPrimary.copy(alpha = 0.2f)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SandboxPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = tip.actionText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
