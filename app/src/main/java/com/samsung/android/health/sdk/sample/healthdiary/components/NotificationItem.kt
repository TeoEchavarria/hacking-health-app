package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.samsung.android.health.sdk.sample.healthdiary.model.Notification
import com.samsung.android.health.sdk.sample.healthdiary.model.NotificationType
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Notification Item Component
 * 
 * Displays a single notification with icon, title, message, and timestamp.
 */
@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (iconBgColor, iconColor) = getNotificationColors(notification.type)
    val icon = getNotificationIcon(notification.type)
    val timeAgo = formatNotificationTime(notification.timestamp)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SandboxOnSurface,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = timeAgo.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = SandboxSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }
            
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = SandboxOnSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Get notification icon based on type
 */
private fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.MEDICATION -> Icons.Default.Medication
        NotificationType.ACHIEVEMENT -> Icons.Default.CheckCircle
        NotificationType.WARNING -> Icons.Default.Warning
        NotificationType.APPOINTMENT -> Icons.Default.CalendarToday
        NotificationType.REPORT -> Icons.Default.Update
        NotificationType.HEALTH_TIP -> Icons.Default.Lightbulb
        NotificationType.VITALS -> Icons.Default.Favorite
    }
}

/**
 * Get notification colors based on type
 */
@Composable
private fun getNotificationColors(type: NotificationType): Pair<Color, Color> {
    return when (type) {
        NotificationType.MEDICATION -> Color(0xFFEFF6FF) to Color(0xFF2563EB) // blue-50, blue-600
        NotificationType.ACHIEVEMENT -> Color(0xFFF0FDF4) to Color(0xFF16A34A) // green-50, green-600
        NotificationType.WARNING -> Color(0xFFFFFBEB) to Color(0xFFD97706) // amber-50, amber-600
        NotificationType.APPOINTMENT -> Color(0xFFFAF5FF) to Color(0xFF9333EA) // purple-50, purple-600
        NotificationType.REPORT -> SandboxPrimaryFixed.copy(alpha = 0.3f) to SandboxPrimary
        NotificationType.HEALTH_TIP -> Color(0xFFF0FDFA) to Color(0xFF0D9488) // teal-50, teal-600
        NotificationType.VITALS -> Color(0xFFFEF2F2) to Color(0xFFDC2626) // red-50, red-600
    }
}

/**
 * Format notification timestamp to relative time
 */
private fun formatNotificationTime(timestamp: Long): String {
    val elapsed = System.currentTimeMillis() - timestamp
    val minutes = elapsed / 60_000
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "${minutes.toInt()}m"
        hours < 24 -> "${hours.toInt()}h"
        days < 7 -> if (days.toInt() == 1) "Ayer" else "${days.toInt()}d"
        else -> "${(days / 7).toInt()}sem"
    }
}
