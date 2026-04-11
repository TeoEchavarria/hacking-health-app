package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.api.models.MedicationWithTakes
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Bottom sheet showing medication history for a specific day.
 * 
 * Shows list of medications with their taken status for the selected date.
 * Patient can mark/unmark medications if viewing today and isEditable is true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayMedicationHistorySheet(
    dateText: String,
    medications: List<MedicationWithTakes>,
    isLoading: Boolean,
    isToday: Boolean = false,
    isEditable: Boolean = true,
    onDismiss: () -> Unit,
    onMarkAsTaken: (medicationId: String, currentlyTaken: Boolean) -> Unit = { _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = SandboxOutlineVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Box(modifier = Modifier.size(width = 40.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📅",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Medicamentos del $dateText",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SandboxOnSurface
                )
                
                if (isToday && isEditable) {
                    Text(
                        text = "Toca para marcar como tomado",
                        style = MaterialTheme.typography.bodySmall,
                        color = SandboxOnSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(color = SandboxOutlineVariant)
            
            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SandboxPrimary)
                    }
                }
                medications.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💊",
                                fontSize = 48.sp
                            )
                            Text(
                                text = "Sin medicamentos",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = SandboxOnSurface
                            )
                            Text(
                                text = "No hay medicamentos registrados para esta fecha",
                                style = MaterialTheme.typography.bodySmall,
                                color = SandboxOnSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    // Medication list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(medications) { medWithTakes ->
                            DayMedicationItem(
                                medication = medWithTakes,
                                isEditable = isToday && isEditable,
                                onToggle = { onMarkAsTaken(medWithTakes.medication.id, medWithTakes.isTakenToday) }
                            )
                        }
                        
                        // Summary
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            MedicationDaySummary(
                                taken = medications.count { it.isTakenToday },
                                total = medications.size
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual medication item in the day history sheet.
 */
@Composable
private fun DayMedicationItem(
    medication: MedicationWithTakes,
    isEditable: Boolean,
    onToggle: () -> Unit
) {
    val med = medication.medication
    val isTaken = medication.isTakenToday
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken) SandboxPrimaryFixed.copy(alpha = 0.3f) else SandboxSurfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (med.medicationType.lowercase()) {
                                "injection" -> SandboxTertiaryFixed.copy(alpha = 0.5f)
                                else -> SandboxPrimaryFixed.copy(alpha = 0.5f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (med.medicationType.lowercase()) {
                            "injection" -> "💉"
                            else -> "💊"
                        },
                        fontSize = 20.sp
                    )
                }
                
                Column {
                    Text(
                        text = "${med.name} ${med.dosage}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = SandboxOnSurface
                    )
                    Text(
                        text = "${med.time} • ${med.instructions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SandboxOnSurfaceVariant
                    )
                }
            }
            
            // Status icon/button
            if (isEditable) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isTaken) SandboxPrimary else SandboxSurfaceContainer
                        )
                ) {
                    Icon(
                        imageVector = if (isTaken) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (isTaken) "Tomado" else "No tomado",
                        tint = if (isTaken) Color.White else SandboxOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Non-editable status indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isTaken) SandboxPrimary.copy(alpha = 0.8f) else SandboxError.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTaken) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (isTaken) "Tomado" else "No tomado",
                        tint = if (isTaken) Color.White else SandboxError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Summary card showing adherence for the day.
 */
@Composable
private fun MedicationDaySummary(
    taken: Int,
    total: Int
) {
    val percentage = if (total > 0) (taken * 100 / total) else 0
    val isComplete = taken == total
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) SandboxPrimaryFixed else SandboxSurfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isComplete) "¡Completado!" else "Progreso del día",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isComplete) SandboxPrimary else SandboxOnSurface
                )
                Text(
                    text = "$taken de $total medicamentos tomados",
                    style = MaterialTheme.typography.bodySmall,
                    color = SandboxOnSurfaceVariant
                )
            }
            
            // Percentage badge
            Surface(
                color = if (isComplete) SandboxPrimary else SandboxSecondary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
