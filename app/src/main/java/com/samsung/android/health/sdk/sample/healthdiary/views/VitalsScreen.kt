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
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.VitalsViewModel

/**
 * Vitals Screen - Monitoring & Notifications
 * 
 * NEW design focused on:
 * - Urgent health alerts
 * - GPS location tracking
 * - Recent notifications
 * - Day summary
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
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
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
