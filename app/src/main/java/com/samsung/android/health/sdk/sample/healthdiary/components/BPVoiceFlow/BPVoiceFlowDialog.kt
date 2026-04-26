package com.samsung.android.health.sdk.sample.healthdiary.components.BPVoiceFlow

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.samsung.android.health.sdk.sample.healthdiary.api.models.BPClassificationResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CrisisResult
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BPVoiceUiState
import com.samsung.android.health.sdk.sample.healthdiary.voice.AudioRecordingService

/**
 * Full-screen dialog for the blood pressure voice flow.
 *
 * Displays different content based on the current state:
 * - Recording: Mic animation + timer (0s/30s)
 * - Uploading: Loading spinner
 * - Low confidence: Manual edit form
 * - Crisis: Urgent alert
 * - Success: Confirmation with BP values
 */
@Composable
fun BPVoiceFlowDialog(
    state: BPVoiceUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onConfirm: () -> Unit,
    onEditValues: (systolic: Int, diastolic: Int, pulse: Int?) -> Unit,
    onOpenLanguageSettings: () -> Unit = {},
    onStopRecording: () -> Unit = onDismiss
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.isInProgress
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "bpFlowContent"
            ) { currentState ->
                when {
                    currentState.isRecording -> RecordingContent(
                        durationMs = currentState.recordingDurationMs,
                        maxDurationMs = AudioRecordingService.MAX_DURATION_MS,
                        onStop = onStopRecording,
                        onCancel = onDismiss
                    )
                    
                    currentState.isUploading -> UploadingContent()
                    
                    currentState.showCrisisDialog -> BPCrisisAlert(
                        crisis = currentState.crisisDetected!!,
                        parseResult = currentState.parseResult,
                        onAcknowledge = onConfirm,
                        onDismiss = onDismiss
                    )
                    
                    currentState.showLowConfidenceDialog -> LowConfidenceCard(
                        parseResult = currentState.parseResult,
                        validationError = currentState.validationError,
                        onRetry = onRetry,
                        onEditConfirm = onEditValues,
                        onDismiss = onDismiss
                    )
                    
                    currentState.showSuccessDialog && !currentState.submissionSuccess -> ConfirmReadingContent(
                        parseResult = currentState.parseResult!!,
                        classification = currentState.classification,
                        onConfirm = onConfirm,
                        onEdit = { onEditValues(
                            currentState.parseResult.systolic ?: 120,
                            currentState.parseResult.diastolic ?: 80,
                            currentState.parseResult.pulse
                        ) },
                        onRetry = onRetry
                    )
                    
                    currentState.submissionSuccess -> BPReadingConfirmation(
                        parseResult = currentState.parseResult!!,
                        classification = currentState.classification!!,
                        onDone = onDismiss
                    )
                    
                    currentState.error != null -> ErrorContent(
                        error = currentState.error,
                        needsLanguagePack = currentState.needsLanguagePack,
                        onRetry = onRetry,
                        onDismiss = onDismiss,
                        onOpenSettings = onOpenLanguageSettings
                    )
                    
                    else -> {
                        // Default/idle state - should not be shown in dialog
                        Box(Modifier.padding(24.dp)) {
                            Text("Preparando...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingContent(
    durationMs: Long,
    maxDurationMs: Long,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    // Pulsing animation for the mic icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // Format duration as "Xs / 30s"
    val durationSec = (durationMs / 1000).toInt()
    val maxSec = (maxDurationMs / 1000).toInt()
    val progress = durationMs.toFloat() / maxDurationMs.toFloat()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated mic icon with pulse
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
        }
        
        Text(
            text = "Grabando...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SandboxOnSurface
        )
        
        // Timer display
        Text(
            text = "${durationSec}s / ${maxSec}s",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (durationSec >= maxSec - 5) MaterialTheme.colorScheme.error else SandboxPrimary
        )
        
        // Progress bar
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (durationSec >= maxSec - 5) MaterialTheme.colorScheme.error else SandboxPrimary,
        )
        
        Text(
            text = "Di tu presión arterial, por ejemplo:\n\"Mi presión es 120 sobre 80, pulso 72\"",
            style = MaterialTheme.typography.bodyMedium,
            color = SandboxOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Stop button (primary action)
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = SandboxPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Detener y Procesar", fontWeight = FontWeight.Bold)
        }
        
        // Cancel button
        TextButton(onClick = onCancel) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun UploadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = SandboxPrimary
        )
        
        Text(
            text = "Procesando audio...",
            style = MaterialTheme.typography.bodyLarge,
            color = SandboxOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Transcribiendo y extrayendo valores",
            style = MaterialTheme.typography.bodySmall,
            color = SandboxOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConfirmReadingContent(
    parseResult: com.samsung.android.health.sdk.sample.healthdiary.api.models.AudioParseResult,
    classification: BPClassificationResult?,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SandboxPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = SandboxPrimary
            )
        }
        
        Text(
            text = "¿Confirmar medición?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SandboxOnSurface
        )
        
        // BP Values card
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
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${parseResult.systolic ?: "--"}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = SandboxOnSurface
                    )
                    Text(
                        text = " / ",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SandboxOnSurfaceVariant
                    )
                    Text(
                        text = "${parseResult.diastolic ?: "--"}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = SandboxOnSurface
                    )
                    Text(
                        text = " mmHg",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SandboxOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                if (parseResult.pulse != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${parseResult.pulse} BPM",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SandboxOnSurfaceVariant
                        )
                    }
                }
                
                // Classification badge
                if (classification != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = when (classification.severity) {
                            "normal" -> SandboxPrimary.copy(alpha = 0.2f)
                            "elevated" -> Color(0xFFFFA726).copy(alpha = 0.2f)
                            "high" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else -> SandboxSurfaceContainerLow
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = classification.stage,
                            style = MaterialTheme.typography.labelMedium,
                            color = when (classification.severity) {
                                "normal" -> SandboxPrimary
                                "elevated" -> Color(0xFFF57C00)
                                "high" -> MaterialTheme.colorScheme.error
                                else -> SandboxOnSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        
        // Transcription preview
        if (parseResult.transcription.isNotBlank()) {
            Text(
                text = "Escuchamos: \"${parseResult.transcription}\"",
                style = MaterialTheme.typography.bodySmall,
                color = SandboxOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirmar y Guardar", fontWeight = FontWeight.Bold)
        }
        
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Editar")
            }
            
            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Repetir")
            }
        }
    }
}

@Composable
private fun ParsingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = SandboxPrimary
        )
        
        Text(
            text = "Procesando medición...",
            style = MaterialTheme.typography.bodyLarge,
            color = SandboxOnSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    needsLanguagePack: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = if (needsLanguagePack) Icons.Default.Language else Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = if (needsLanguagePack) SandboxPrimary else MaterialTheme.colorScheme.error
        )
        
        Text(
            text = if (needsLanguagePack) "Idioma Español Requerido" else "Error",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SandboxOnSurface
        )
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = SandboxOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (needsLanguagePack) {
            Text(
                text = "Para usar reconocimiento de voz, necesitas descargar el paquete de idioma español en la configuración de tu dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = SandboxOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
            
            if (needsLanguagePack) {
                Button(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Configuración")
                }
            } else {
                Button(onClick = onRetry) {
                    Text("Reintentar")
                }
            }
        }
    }
}
