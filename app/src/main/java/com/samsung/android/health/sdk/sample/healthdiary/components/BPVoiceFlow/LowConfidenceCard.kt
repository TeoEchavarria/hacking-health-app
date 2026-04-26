package com.samsung.android.health.sdk.sample.healthdiary.components.BPVoiceFlow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.api.models.AudioParseResult
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Card shown when LLM has low confidence or validation fails.
 *
 * Allows user to:
 * - See what was detected and the transcription
 * - Manually edit values
 * - Retry voice input
 */
@Composable
fun LowConfidenceCard(
    parseResult: AudioParseResult?,
    validationError: String?,
    onRetry: () -> Unit,
    onEditConfirm: (systolic: Int, diastolic: Int, pulse: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var systolicText by remember { mutableStateOf(parseResult?.systolic?.toString() ?: "") }
    var diastolicText by remember { mutableStateOf(parseResult?.diastolic?.toString() ?: "") }
    var pulseText by remember { mutableStateOf(parseResult?.pulse?.toString() ?: "") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = SandboxPrimary
        )
        
        // Title
        Text(
            text = if (parseResult?.systolic != null) "Verificar lectura" else "No se detectó presión",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SandboxOnSurface,
            textAlign = TextAlign.Center
        )
        
        // Error/explanation message
        Text(
            text = validationError 
                ?: "No pudimos interpretar con seguridad tu medición. Por favor verifica los valores o intenta de nuevo.",
            style = MaterialTheme.typography.bodyMedium,
            color = SandboxOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // Show transcription if available
        if (parseResult?.transcription?.isNotEmpty() == true) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SandboxSurfaceContainerLow.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Lo que escuchamos:",
                        style = MaterialTheme.typography.labelSmall,
                        color = SandboxOnSurfaceVariant
                    )
                    Text(
                        text = "\"${parseResult.transcription}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SandboxOnSurface,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
        
        // Detected values or edit form
        if (isEditing || parseResult?.systolic == null) {
            // Edit form
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SandboxSurfaceContainerLow,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = systolicText,
                        onValueChange = { systolicText = it.filter { c -> c.isDigit() } },
                        label = { Text("Sistólica (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = diastolicText,
                        onValueChange = { diastolicText = it.filter { c -> c.isDigit() } },
                        label = { Text("Diastólica (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = pulseText,
                        onValueChange = { pulseText = it.filter { c -> c.isDigit() } },
                        label = { Text("Pulso (BPM) - Opcional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            // Confirm button
            Button(
                onClick = {
                    val s = systolicText.toIntOrNull()
                    val d = diastolicText.toIntOrNull()
                    val p = pulseText.toIntOrNull()
                    if (s != null && d != null) {
                        onEditConfirm(s, d, p)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = systolicText.toIntOrNull() != null && diastolicText.toIntOrNull() != null
            ) {
                Text("Confirmar valores")
            }
        } else {
            // Show detected values
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SandboxSurfaceContainerLow,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Valores detectados:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SandboxOnSurfaceVariant
                    )
                    Text(
                        text = "${parseResult.systolic}/${parseResult.diastolic}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = SandboxOnSurface
                    )
                    if (parseResult.pulse != null) {
                        Text(
                            text = "${parseResult.pulse} BPM",
                            style = MaterialTheme.typography.titleMedium,
                            color = SandboxOnSurfaceVariant
                        )
                    }
                }
            }
            
            // Edit button
            OutlinedButton(
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar valores")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }
            
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reintentar voz")
            }
        }
    }
}
