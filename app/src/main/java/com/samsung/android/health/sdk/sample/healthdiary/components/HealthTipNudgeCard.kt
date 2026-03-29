package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Health Tip Nudge Card
 * 
 * Simple card with a health tip/nudge for the user
 * Matches the HTML mockup design with:
 * - Gradient background (white to primary-fixed)
 * - Lightbulb icon in primary color
 * - Tip text with user name
 * - Chevron arrow on the right
 */
@Composable
fun HealthTipNudgeCard(
    userName: String = "",
    tipText: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        SandboxPrimaryFixed.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = SandboxPrimaryFixed.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SandboxPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Tip text
            Text(
                text = if (userName.isNotEmpty()) {
                    "$userName, $tipText"
                } else {
                    tipText
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SandboxOnSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
            
            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SandboxPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
