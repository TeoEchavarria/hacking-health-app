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
 * 
 * @param month The month name to display (e.g., "Abril 2026")
 * @param currentDay The current day of the month to highlight (-1 for no highlight)
 * @param daysInMonth Total days in the month (28-31)
 * @param startDayOffset Day of week the month starts on (0=Monday, 6=Sunday)
 * @param eventsOnDays Map of day numbers to event types
 */
@Composable
fun CalendarGrid(
    month: String = "Octubre 2024",
    currentDay: Int = 24,
    daysInMonth: Int = 31,
    startDayOffset: Int = 0,
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
            
            // Calendar grid (dynamic)
            CalendarGridContent(
                currentDay = currentDay,
                daysInMonth = daysInMonth,
                startDayOffset = startDayOffset,
                eventsOnDays = eventsOnDays,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
private fun CalendarGridContent(
    currentDay: Int,
    daysInMonth: Int,
    startDayOffset: Int,
    eventsOnDays: Map<Int, EventType>,
    onDayClick: (Int) -> Unit
) {
    // Calculate number of weeks needed
    val totalCells = startDayOffset + daysInMonth
    val weeksNeeded = (totalCells + 6) / 7 // Ceiling division
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var dayCounter = 1
        
        for (week in 0 until weeksNeeded) {
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
                        val currentDayValue = dayCounter
                        CalendarDayCell(
                            day = dayCounter,
                            isToday = dayCounter == currentDay,
                            eventType = eventsOnDays[dayCounter],
                            onClick = { onDayClick(currentDayValue) },
                            modifier = Modifier.weight(1f)
                        )
                        dayCounter++
                    } else {
                        // Empty cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
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
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            when (eventType) {
                                EventType.APPOINTMENT -> SandboxPrimary
                                EventType.MEDICATION -> SandboxTertiary
                                EventType.MEDICATION_COMPLETE -> SandboxPrimary
                                EventType.MEDICATION_PENDING -> SandboxError
                            }
                        )
                )
            } else if (eventType != null && isToday) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            when (eventType) {
                                EventType.MEDICATION_COMPLETE -> SandboxPrimaryFixed
                                EventType.MEDICATION_PENDING -> Color.White
                                else -> Color.White
                            }
                        )
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
    MEDICATION,           // Generic medication (for backward compat)
    MEDICATION_COMPLETE,  // All medications taken
    MEDICATION_PENDING    // Some medications not taken
}
