package com.samsung.android.health.sdk.sample.healthdiary.views

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.model.DaySummary
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Calendar Screen - Medical Calendar
 * 
 * Health calendar with:
 * - Monthly calendar view
 * - Medication reminders
 * - Upcoming appointments
 * - Biometric analysis summary
 * - Day summary
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Mock data states
    var medicationsTaken by remember { mutableStateOf(setOf<Int>()) }
    
    // Mock calendar events
    val calendarEvents = remember {
        mapOf(
            2 to EventType.MEDICATION,
            4 to EventType.APPOINTMENT,
            23 to EventType.APPOINTMENT,
            24 to EventType.APPOINTMENT // Today
        )
    }
    
    // Mock medications
    val medications = remember {
        listOf(
            MedicationData(
                id = 1,
                name = "Atorvastatina",
                dosage = "20mg",
                time = "08:00 AM",
                instructions = "Con el desayuno",
                type = MedicationType.PILL
            ),
            MedicationData(
                id = 2,
                name = "Insulina Basal",
                dosage = "",
                time = "09:30 PM",
                instructions = "Antes de dormir",
                type = MedicationType.INJECTION
            )
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header Section
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calendario Médico",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SandboxOnSurface
                    )
                    Text(
                        text = "Hoy es Jueves, 24 de Octubre",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SandboxOnSurfaceVariant
                    )
                }
            }
            
            // New Appointment Button
            Button(
                onClick = {
                    Toast.makeText(context, "Nueva cita - Próximamente", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SandboxPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nueva Cita",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // Two Column Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: Calendar + Medications
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Monthly Calendar
                CalendarGrid(
                    month = "Octubre 2024",
                    currentDay = 24,
                    eventsOnDays = calendarEvents,
                    onPreviousMonth = {
                        Toast.makeText(context, "Mes anterior", Toast.LENGTH_SHORT).show()
                    },
                    onNextMonth = {
                        Toast.makeText(context, "Mes siguiente", Toast.LENGTH_SHORT).show()
                    },
                    onDayClick = { day ->
                        Toast.makeText(context, "Día $day seleccionado", Toast.LENGTH_SHORT).show()
                    }
                )
                
                // Medications Section
                MedicationsSection(
                    medications = medications,
                    takenIds = medicationsTaken,
                    onMarkAsTaken = { id ->
                        medicationsTaken = if (medicationsTaken.contains(id)) {
                            medicationsTaken - id
                        } else {
                            medicationsTaken + id
                        }
                        val med = medications.find { it.id == id }
                        Toast.makeText(
                            context,
                            "${med?.name} marcado como tomado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
        
        // Full Width Sections
        
        // Upcoming Appointment
        AppointmentHeroCard(
            appointmentType = "Chequeo de Cardiología",
            timeRange = "4:00 PM - 5:00 PM",
            doctorName = "Dr. Alejandro Méndez",
            location = "Centro Médico Sanitas",
            onNavigateClick = {
                Toast.makeText(context, "Abrir navegación", Toast.LENGTH_SHORT).show()
            },
            onMoreClick = {
                Toast.makeText(context, "Más opciones", Toast.LENGTH_SHORT).show()
            }
        )
        
        // Biometric Analysis Section
        BiometricAnalysisSection()
        
        // Day Summary
        DaySummaryCard(
            summary = DaySummary(
                steps = 2450,
                sleepHours = 7.7f,
                description = "Estado general estable. Tendencia de presión sugiere evitar esfuerzos intensos."
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Medications Section
 */
@Composable
private fun MedicationsSection(
    medications: List<MedicationData>,
    takenIds: Set<Int>,
    onMarkAsTaken: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = SandboxPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Recordatorios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Pending badge
            val pending = medications.size - takenIds.size
            if (pending > 0) {
                Surface(
                    color = SandboxPrimaryFixed,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$pending Pendiente${if (pending > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SandboxPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        // Medication cards
        medications.forEach { med ->
            MedicationReminderCard(
                medicationName = med.name,
                dosage = med.dosage,
                time = med.time,
                instructions = med.instructions,
                medicationType = med.type,
                isTaken = takenIds.contains(med.id),
                onMarkAsTaken = { onMarkAsTaken(med.id) }
            )
        }
    }
}

/**
 * Biometric Analysis Section
 */
@Composable
private fun BiometricAnalysisSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Análisis Biométrico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = SandboxPrimary
            )
            Text(
                text = "Hace 15 min",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = SandboxSecondary,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
            )
        }
        
        // Biometric cards
        BiometricCompactCard(
            icon = "🫁", // Lungs emoji
            metricName = "Oxígeno (SpO2)",
            value = "98%",
            status = BiometricStatus.OPTIMAL,
            statusLabel = "Óptimo",
            trend = "Estable",
            backgroundColor = SandboxPrimaryFixed,
            iconColor = SandboxPrimary
        )
        
        BiometricCompactCard(
            icon = "💓", // Beating heart emoji
            metricName = "Variabilidad (VFC)",
            value = "42ms",
            status = BiometricStatus.WARNING,
            statusLabel = "Aviso",
            trend = "Fatiga",
            backgroundColor = Color(0xFFFFF3E0),
            iconColor = Color(0xFFEF6C00)
        )
        
        BiometricCompactCard(
            icon = "🌡️", // Thermometer emoji
            metricName = "Presión Arterial",
            value = "145/92",
            status = BiometricStatus.CRITICAL,
            statusLabel = "Alta",
            trend = "Urgente",
            backgroundColor = SandboxErrorContainer,
            iconColor = SandboxError
        )
        
        BiometricCompactCard(
            icon = "🌡️", // Thermometer emoji
            metricName = "Temperatura",
            value = "36.7°C",
            status = BiometricStatus.NORMAL,
            statusLabel = "Normal",
            trend = "Estable",
            backgroundColor = Color(0xFFE3F2FD),
            iconColor = Color(0xFF1976D2)
        )
    }
}

/**
 * Medication data model
 */
private data class MedicationData(
    val id: Int,
    val name: String,
    val dosage: String,
    val time: String,
    val instructions: String,
    val type: MedicationType
)
