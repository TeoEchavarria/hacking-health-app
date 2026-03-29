package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Calendar Grid Component
 * 
 * Displays a monthly calendar view with:
 * - Month navigation header
 * - Day names
 * - Calendar grid with event indicators
 */
@Composable
fun CalendarGrid(
    month: String = "Octubre 2024",
    currentDay: Int = 24,
    eventsOnDays: Map<Int, EventType> = emptyMap(),
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onDayClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header with month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SandboxSurfaceContainerLow)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Mes anterior",
                            tint = SandboxOnSurface
                        )
                    }
                    
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SandboxSurfaceContainerLow)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Mes siguiente",
                            tint = SandboxOnSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Day names header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SandboxOnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Calendar grid (October 2024 - static)
            CalendarGridContent(
                currentDay = currentDay,
                eventsOnDays = eventsOnDays,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
private fun CalendarGridContent(
    currentDay: Int,
    eventsOnDays: Map<Int, EventType>,
    onDayClick: (Int) -> Unit
) {
    // October 2024 starts on Tuesday (index 1)
    val daysInMonth = 31
    val startDayOffset = 1 // Tuesday
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var dayCounter = 1
        
        // 5 weeks for October 2024
        for (week in 0..4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayOfWeek in 0..6) {
                    val shouldShowDay = if (week == 0) {
                        dayOfWeek >= startDayOffset
                    } else {
                        dayCounter <= daysInMonth
                    }
                    
                    if (shouldShowDay && dayCounter <= daysInMonth) {
                        CalendarDayCell(
                            day = dayCounter,
                            isToday = dayCounter == currentDay,
                            eventType = eventsOnDays[dayCounter],
                            onClick = { onDayClick(dayCounter) },
                            modifier = Modifier.weight(1f)
                        )
                        dayCounter++
                    } else {
                        // Empty cell or previous month days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            if (week == 0 && dayOfWeek < startDayOffset) {
                                // Show previous month's last days
                                val prevMonthDay = 30 - (startDayOffset - dayOfWeek - 1)
                                Text(
                                    text = prevMonthDay.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SandboxOnSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    eventType: EventType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isToday) SandboxPrimary else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isToday) Color.White else SandboxOnSurface
            )
            
            // Event indicator dot
            if (eventType != null && !isToday) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            when (eventType) {
                                EventType.APPOINTMENT -> SandboxPrimary
                                EventType.MEDICATION -> SandboxTertiary
                            }
                        )
                )
            } else if (eventType != null && isToday) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * Event type for calendar days
 */
enum class EventType {
    APPOINTMENT,
    MEDICATION
}
