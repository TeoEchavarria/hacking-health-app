package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
 * Medication Reminder Card
 * 
 * Shows a medication reminder with:
 * - Medication icon and type
 * - Name and dosage
 * - Time and instructions
 * - Mark as taken button
 */
@Composable
fun MedicationReminderCard(
    medicationName: String,
    dosage: String,
    time: String,
    instructions: String,
    medicationType: MedicationType = MedicationType.PILL,
    isTaken: Boolean = false,
    onMarkAsTaken: () -> Unit = {},
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (medicationType) {
                                MedicationType.PILL -> SandboxPrimaryFixed.copy(alpha = 0.4f)
                                MedicationType.INJECTION -> SandboxTertiaryFixed.copy(alpha = 0.4f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (medicationType) {
                            MedicationType.PILL -> "💊"
                            MedicationType.INJECTION -> "💉"
                        },
                        fontSize = 24.sp
                    )
                }
                
                // Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$medicationName $dosage",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = SandboxOnSurface
                    )
                    
                    Text(
                        text = "$time • $instructions",
                        style = MaterialTheme.typography.bodySmall,
                        color = SandboxOnSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
            
            // Right: Check button
            IconButton(
                onClick = onMarkAsTaken,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isTaken) SandboxPrimary else SandboxSurfaceContainer
                    )
                    .border(
                        width = 1.dp,
                        color = if (isTaken) SandboxPrimary else SandboxOutlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Marcar como tomado",
                    tint = if (isTaken) Color.White else SandboxOnSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Medication type for different icon styles
 */
enum class MedicationType {
    PILL,
    INJECTION
}
