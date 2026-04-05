package com.samsung.android.health.sdk.sample.healthdiary.views

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LinkedPatient
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.VitalsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vitals Screen - Monitoring & Notifications
 * 
 * NEW design focused on:
 * - Urgent health alerts
 * - GPS location tracking
 * - Recent notifications
 * - Day summary
 * - **Caregiver mode**: View linked patient's alerts
 * 
 * Note: Biometric data moved to BiometricsScreen
 */
@Composable
fun VitalsScreen(
    onNavigateToTracking: () -> Unit = {},
    onNavigateToBiometrics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { VitalsViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    // Stop polling when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }
    
    // Show error message
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Caregiver Mode Header
        if (uiState.isCaregiverMode) {
            CaregiverModeHeader(
                selectedPatient = uiState.selectedPatient,
                linkedPatients = uiState.linkedPatients,
                lastRefreshTime = uiState.lastRefreshTime,
                onPatientSelected = { viewModel.selectPatient(it) },
                onRefresh = { viewModel.refreshPatientData() }
            )
        }
        
        // 1. Urgent Alert Banner (conditional)
        uiState.currentAlert?.let { alert ->
            AlertBannerCard(
                alert = alert,
                onViewClick = {
                    Toast.makeText(context, "Ver detalles de alerta", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.dismissAlert() }
            )
        }
        
        // 2. Map Preview Card
        MapPreviewCard(
            location = uiState.lastLocation,
            onClick = {
                onNavigateToTracking()
                Toast.makeText(context, "Abrir mapa completo", Toast.LENGTH_SHORT).show()
            }
        )
        
        // 3. Notifications Section
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
                    text = "Últimas Notificaciones",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SandboxPrimary
                )
            }
            
            // Notifications list (show top 5)
            if (uiState.notifications.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay notificaciones recientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SandboxSecondary
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.notifications.take(5).forEach { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = {
                                viewModel.markNotificationAsRead(notification.id)
                                Toast.makeText(
                                    context,
                                    "Ver detalles de: ${notification.title}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
            
            // "Ver más" button (if more than 5 notifications)
            if (uiState.notifications.size > 5) {
                TextButton(
                    onClick = {
                        viewModel.loadMoreNotifications()
                        Toast.makeText(context, "Ver más notificaciones", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Ver más",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SandboxPrimary
                    )
                }
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Header shown in caregiver mode with patient selector and refresh button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaregiverModeHeader(
    selectedPatient: LinkedPatient?,
    linkedPatients: List<LinkedPatient>,
    lastRefreshTime: Long,
    onPatientSelected: (LinkedPatient) -> Unit,
    onRefresh: () -> Unit
) {
    var showPatientDropdown by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Modo Cuidador",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            if (linkedPatients.isEmpty()) {
                // No linked patients
                Text(
                    text = "No tienes pacientes vinculados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            } else {
                // Patient selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Patient dropdown
                    ExposedDropdownMenuBox(
                        expanded = showPatientDropdown,
                        onExpandedChange = { showPatientDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedPatient?.patientName ?: "Seleccionar paciente",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Viendo datos de") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPatientDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showPatientDropdown,
                            onDismissRequest = { showPatientDropdown = false }
                        ) {
                            linkedPatients.forEach { patient ->
                                DropdownMenuItem(
                                    text = { Text(patient.patientName) },
                                    onClick = {
                                        onPatientSelected(patient)
                                        showPatientDropdown = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Refresh button
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Last refresh time
                if (lastRefreshTime > 0) {
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    Text(
                        text = "Última actualización: ${timeFormat.format(Date(lastRefreshTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
