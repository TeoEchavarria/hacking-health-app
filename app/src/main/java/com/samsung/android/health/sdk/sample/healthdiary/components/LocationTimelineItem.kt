package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Location Timeline Item
 * 
 * Displays a single location point in the 24-hour timeline with:
 * - Colored dot indicator (current vs historical)
 * - Location name/address
 * - Time and additional details
 * - Connecting line to next item
 */
@Composable
fun LocationTimelineItem(
    address: String,
    timestamp: Long,
    isCurrent: Boolean = false,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isCurrent) SandboxPrimary else SandboxSecondaryLight
    val textColor = if (isCurrent) SandboxOnSurface else SandboxOnSurfaceVariant
    val timeColor = SandboxSecondary
    
    // Format timestamp
    val timeText = formatTimestamp(timestamp)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeline visual (dot + line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            // Dot
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 14.dp else 12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            
            // Connecting line (if not last item)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(SandboxPrimaryFixed)
                )
            }
        }
        
        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = timeColor,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Format timestamp to human-readable format
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Hace menos de 1 min"
        diff < 3600_000 -> {
            val minutes = (diff / 60_000).toInt()
            "Hace $minutes min"
        }
        diff < 86400_000 -> {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
        else -> {
            val formatter = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}
