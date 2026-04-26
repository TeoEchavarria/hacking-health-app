package com.samsung.android.health.sdk.sample.healthdiary.components.BPVoiceFlow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.api.models.BPClassificationResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.AudioParseResult
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Success confirmation showing saved BP reading with classification.
 */
@Composable
fun BPReadingConfirmation(
    parseResult: AudioParseResult,
    classification: BPClassificationResult,
    onDone: () -> Unit
) {
    val severityColor = when (classification.severity) {
        "urgent" -> MaterialTheme.colorScheme.error
        "high" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        "moderate" -> SandboxPrimary
        else -> SandboxSuccess
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SandboxSuccess
        )
        
        Text(
            text = "Lectura guardada",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SandboxOnSurface
        )
        
        // Reading display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SandboxSurfaceContainerLow,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${parseResult.systolic}/${parseResult.diastolic}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = SandboxOnSurface
                )
                
                Text(
                    text = "mmHg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SandboxOnSurfaceVariant
                )
                
                if (parseResult.pulse != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${parseResult.pulse} BPM",
                        style = MaterialTheme.typography.titleMedium,
                        color = SandboxOnSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Classification badge
                Surface(
                    color = severityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = classification.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = severityColor
                    )
                }
            }
        }
        
        // Additional info
        Text(
            text = "Clasificación según ${classification.guideline}",
            style = MaterialTheme.typography.bodySmall,
            color = SandboxOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Listo")
        }
    }
}
