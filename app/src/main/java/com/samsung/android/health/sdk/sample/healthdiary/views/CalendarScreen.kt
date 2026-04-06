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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientHealthSummaryResponse
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.model.DaySummary
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BiometricAnalysisUiState
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BiometricsViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.CalendarViewModel

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
    modifier: Modifier = Modifier,
    calendarViewModel: CalendarViewModel = viewModel()
) {
    val context = LocalContext.current
    val biometricsViewModel = remember { BiometricsViewModel(context) }
    val biometricState by calendarViewModel.biometricState.collectAsState()
    val biometricsUiState by biometricsViewModel.uiState.collectAsState()
    
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
        
        // Alert Banner (only if there's a critical alert from BiometricsViewModel)
        biometricsUiState.currentAlert?.let { alert ->
            AlertBannerCard(
                alert = alert,
                onViewClick = {
                    Toast.makeText(context, "Detalles de alerta próximamente", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { biometricsViewModel.dismissAlert() }
            )
        }
        
        // Health Tip Card (personalized health tip from BiometricsViewModel)
        HealthTipCard(
            tip = biometricsUiState.healthTip,
            onActionClick = {
                Toast.makeText(context, "Próximamente", Toast.LENGTH_SHORT).show()
            }
        )
        
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
        
        // Biometric Analysis Section (Heart Rate only with history)
        BiometricAnalysisSection(
            state = biometricState,
            onRefresh = { calendarViewModel.refreshBiometricData() },
            onHeartRateClick = {
                // TODO: Navigate to heart rate history or show bottom sheet
                Toast.makeText(context, "Ver historial de frecuencia cardíaca", Toast.LENGTH_SHORT).show()
            }
        )
        
        // Day Summary - Shows Steps and Sleep from real data
        DaySummaryCard(
            summary = DaySummary(
                steps = biometricState.healthSummary?.steps?.total,
                sleepHours = biometricState.healthSummary?.sleep?.totalMinutes?.let { it / 60f },
                description = generateDaySummaryDescription(biometricState)
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
 * 
 * Shows health metrics from the patient's wearable device.
 * - Heart Rate: clickable to see 24h history
 * - Steps & Sleep: moved to Day Summary section
 * - Unavailable metrics: SpO2, Blood Pressure, Temperature (shown with device limitation message)
 */
@Composable
private fun BiometricAnalysisSection(
    state: BiometricAnalysisUiState,
    onRefresh: () -> Unit = {},
    onHeartRateClick: () -> Unit = {}
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
            Column {
                Text(
                    text = "Análisis Biométrico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SandboxPrimary
                )
                if (state.patientName != null && state.role == "caregiver") {
                    Text(
                        text = "de ${state.patientName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SandboxSecondary
                    )
                }
            }
            Text(
                text = state.lastUpdatedText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = SandboxSecondary,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
            )
        }
        
        if (state.isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SandboxPrimary)
            }
        } else if (state.error != null) {
            // Error state
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SandboxErrorContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SandboxError
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Reintentar", color = SandboxError)
                    }
                }
            }
        } else {
            val summary = state.healthSummary
            
            // Heart Rate - Clickable for history
            if (summary?.hasHeartRate() == true) {
                BiometricClickableCard(
                    icon = "💓",
                    metricName = "Frecuencia Cardíaca",
                    value = "${summary.heartRate.average} BPM",
                    status = when {
                        summary.heartRate.average!! > 100 -> BiometricStatus.WARNING
                        summary.heartRate.average < 50 -> BiometricStatus.WARNING
                        else -> BiometricStatus.OPTIMAL
                    },
                    statusLabel = when {
                        summary.heartRate.average > 100 -> "Alta"
                        summary.heartRate.average < 50 -> "Baja"
                        else -> "Normal"
                    },
                    trend = "Min: ${summary.heartRate.min} • Max: ${summary.heartRate.max}",
                    hint = "Toca para ver historial 24h",
                    backgroundColor = SandboxPrimaryFixed,
                    iconColor = SandboxPrimary,
                    onClick = onHeartRateClick
                )
            } else {
                BiometricUnavailableCard(
                    icon = "💓",
                    metricName = "Frecuencia Cardíaca",
                    reason = "Sin datos del reloj"
                )
            }
            
            // Unavailable metrics - device limitation
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "MÉTRICAS NO DISPONIBLES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SandboxSecondary,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            BiometricUnavailableCard(
                icon = "🫁",
                metricName = "Oxígeno (SpO2)",
                reason = "NO DISPONIBLE PARA TU DISPOSITIVO"
            )
            
            BiometricUnavailableCard(
                icon = "🩺",
                metricName = "Presión Arterial",
                reason = "NO DISPONIBLE PARA TU DISPOSITIVO"
            )
            
            BiometricUnavailableCard(
                icon = "🌡️",
                metricName = "Temperatura",
                reason = "NO DISPONIBLE PARA TU DISPOSITIVO"
            )
        }
    }
}

/**
 * Card for unavailable biometric metrics.
 */
@Composable
private fun BiometricUnavailableCard(
    icon: String,
    metricName: String,
    reason: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SandboxSurfaceContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SandboxSurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    color = Color.Gray
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = metricName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SandboxSecondary,
                    letterSpacing = 1.2.sp,
                    fontSize = 10.sp
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = SandboxSecondary
                )
            }
        }
    }
}

/**
 * Generate description for day summary based on health data
 */
private fun generateDaySummaryDescription(state: BiometricAnalysisUiState): String {
    val summary = state.healthSummary ?: return "Sin datos disponibles aún. Conecta tu reloj para ver tu resumen diario."
    
    val parts = mutableListOf<String>()
    
    // Heart rate status
    summary.heartRate.average?.let { hr ->
        parts.add(when {
            hr > 100 -> "Frecuencia cardíaca elevada"
            hr < 50 -> "Frecuencia cardíaca baja"
            else -> "Frecuencia cardíaca normal"
        })
    }
    
    // Steps status
    summary.steps.total?.let { steps ->
        parts.add(when {
            steps >= 10000 -> "Excelente actividad física"
            steps >= 5000 -> "Buena actividad física"
            steps >= 2000 -> "Actividad física moderada"
            else -> "Poca actividad física hoy"
        })
    }
    
    // Sleep status
    summary.sleep.totalMinutes?.let { minutes ->
        val hours = minutes / 60
        parts.add(when {
            hours >= 7 -> "Sueño reparador"
            hours >= 5 -> "Sueño regular"
            else -> "Sueño insuficiente"
        })
    }
    
    return if (parts.isNotEmpty()) {
        parts.joinToString(". ") + "."
    } else {
        "Estado general estable."
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
