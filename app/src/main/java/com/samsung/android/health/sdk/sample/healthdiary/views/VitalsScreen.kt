package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxOnSurfaceVariant
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxPrimary

/**
 * Vitals Screen - Placeholder
 * 
 * Health vitals dashboard will be added here.
 * Currently shows a placeholder message.
 */
@Composable
fun VitalsScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MonitorHeart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SandboxPrimary.copy(alpha = 0.5f)
            )
            
            Text(
                text = "Vitales",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "El panel de signos vitales estará disponible próximamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = SandboxOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
