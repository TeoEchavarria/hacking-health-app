package com.samsung.android.health.sdk.sample.healthdiary.views

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.VitalsViewModel

/**
 * Vitals Screen
 * 
 * Biometric dashboard showing:
 * - Urgent alert banner (when applicable)
 * - Health tip card
 * - Biometric analysis (SpO2, Blood Pressure, Temperature)
 * - Day summary (steps + sleep)
 */
@Composable
fun VitalsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { VitalsViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Alert Banner (only if there's a critical alert)
        uiState.currentAlert?.let { alert ->
            AlertBannerCard(
                alert = alert,
                onViewClick = {
                    Toast.makeText(context, "Detalles de alerta próximamente", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.dismissAlert() }
            )
        }
        
        // Health Tip Card
        HealthTipCard(
            tip = uiState.healthTip,
            onActionClick = {
                Toast.makeText(context, "Próximamente", Toast.LENGTH_SHORT).show()
            }
        )
        
        // Biometric Analysis Section - Data from Watch
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Datos del Reloj",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SandboxPrimary
                )
                
                Text(
                    text = uiState.lastUpdateTime.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxSecondary,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Heart Rate (full width - primary metric from watch)
            BiometricCard(
                type = BiometricType.HEART_RATE,
                label = "Ritmo Cardíaco",
                reading = uiState.heartRate,
                onClick = {
                    Toast.makeText(context, "Historial de ritmo cardíaco próximamente", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        // Day Summary Card (Steps + Sleep from watch)
        DaySummaryCard(
            summary = uiState.daySummary
        )
        
        // Other Biometrics Section - Not available from watch
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Otros Indicadores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SandboxSecondary
            )
            
            Text(
                text = "Estos datos requieren dispositivos adicionales",
                style = MaterialTheme.typography.bodySmall,
                color = SandboxSecondary.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // SpO2 (full width)
            BiometricCard(
                type = BiometricType.SPO2,
                label = "Oxígeno (SpO2)",
                reading = uiState.spO2,
                onClick = {
                    Toast.makeText(context, "SpO2 no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
                }
            )
            
            // Blood Pressure (full width)
            BiometricCard(
                type = BiometricType.BLOOD_PRESSURE,
                label = "Presión Arterial",
                reading = uiState.bloodPressure,
                onClick = {
                    Toast.makeText(context, "Presión arterial no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
                }
            )
            
            // Temperature (full width)
            BiometricCard(
                type = BiometricType.TEMPERATURE,
                label = "Temperatura",
                reading = uiState.temperature,
                onClick = {
                    Toast.makeText(context, "Temperatura no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
