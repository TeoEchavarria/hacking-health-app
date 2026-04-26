package com.samsung.android.health.sdk.sample.healthdiary.components.BPVoiceFlow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CrisisResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.AudioParseResult
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Crisis alert component for urgent blood pressure readings.
 *
 * Shows a prominent warning with:
 * - Crisis type and severity
 * - BP reading that triggered it
 * - Clear guidance for the user
 * - Action buttons
 */
@Composable
fun BPCrisisAlert(
    crisis: CrisisResult,
    parseResult: AudioParseResult?,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning icon
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // Title
        Text(
            text = crisis.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        // BP Reading display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${parseResult?.systolic ?: "--"}/${parseResult?.diastolic ?: "--"}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "mmHg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SandboxOnSurfaceVariant
                )
                if (parseResult?.pulse != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${parseResult.pulse} BPM",
                        style = MaterialTheme.typography.titleMedium,
                        color = SandboxOnSurface
                    )
                }
            }
        }
        
        // Crisis guidance
        Text(
            text = crisis.body,
            style = MaterialTheme.typography.bodyMedium,
            color = SandboxOnSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAcknowledge,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Entendido, guardar lectura")
            }
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
