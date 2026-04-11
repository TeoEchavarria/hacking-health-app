package com.samsung.android.health.sdk.sample.healthdiary.views

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxButton
import com.samsung.android.health.sdk.sample.healthdiary.components.ButtonVariant
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.AddMedicationViewModel

/**
 * Screen for adding a new medication reminder.
 * 
 * Features:
 * - Name input (required)
 * - Dosage input
 * - Time picker
 * - Instructions input
 * - Medication type selection (pill/injection)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    onNavigateBack: () -> Unit,
    patientId: String? = null,
    viewModel: AddMedicationViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.time.split(":").getOrNull(0)?.toIntOrNull() ?: 8,
        initialMinute = uiState.time.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )
    
    // Set patient ID if provided (caregiver mode)
    LaunchedEffect(patientId) {
        viewModel.setPatientId(patientId)
    }
    
    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Medicamento creado exitosamente", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }
    
    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Añadir Medicamento",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💊",
                    fontSize = 64.sp
                )
            }
            
            // Name field (required)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nombre del medicamento *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxOnSurface
                )
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    placeholder = { Text("Ej: Metformina") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it, color = SandboxError) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SandboxPrimary,
                        unfocusedBorderColor = SandboxOutlineVariant
                    )
                )
            }
            
            // Dosage field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Dosis",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxOnSurface
                )
                OutlinedTextField(
                    value = uiState.dosage,
                    onValueChange = { viewModel.updateDosage(it) },
                    placeholder = { Text("Ej: 500mg") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SandboxPrimary,
                        unfocusedBorderColor = SandboxOutlineVariant
                    )
                )
            }
            
            // Time picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Hora de la toma *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxOnSurface
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SandboxSurfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏰",
                                fontSize = 24.sp
                            )
                            Text(
                                text = uiState.time,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SandboxPrimary
                            )
                        }
                        Text(
                            text = "Cambiar",
                            style = MaterialTheme.typography.labelMedium,
                            color = SandboxSecondary
                        )
                    }
                }
                
                uiState.timeError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = SandboxError
                    )
                }
            }
            
            // Medication Type Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de medicamento",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxOnSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MedicationTypeOption(
                        icon = "💊",
                        label = "Pastilla",
                        isSelected = uiState.medicationType == "pill",
                        onClick = { viewModel.updateMedicationType("pill") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    MedicationTypeOption(
                        icon = "💉",
                        label = "Inyección",
                        isSelected = uiState.medicationType == "injection",
                        onClick = { viewModel.updateMedicationType("injection") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Instructions field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Instrucciones",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxOnSurface
                )
                OutlinedTextField(
                    value = uiState.instructions,
                    onValueChange = { viewModel.updateInstructions(it) },
                    placeholder = { Text("Ej: Tomar después de las comidas") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SandboxPrimary,
                        unfocusedBorderColor = SandboxOutlineVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Save button
            SandboxButton(
                text = if (uiState.isLoading) "Guardando..." else "Guardar Medicamento",
                onClick = { viewModel.createMedication() },
                enabled = !uiState.isLoading,
                variant = ButtonVariant.Primary,
                fullWidth = true,
                icon = Icons.Default.Check
            )
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = timePickerState.hour.toString().padStart(2, '0')
                        val minute = timePickerState.minute.toString().padStart(2, '0')
                        viewModel.updateTime("$hour:$minute")
                        showTimePicker = false
                    }
                ) {
                    Text("Confirmar", color = SandboxPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            },
            title = {
                Text(
                    "Selecciona la hora",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

/**
 * Option button for medication type selection.
 */
@Composable
private fun MedicationTypeOption(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = SandboxPrimary,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SandboxPrimaryFixed else SandboxSurfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) SandboxPrimary else SandboxOnSurface
            )
        }
    }
}
