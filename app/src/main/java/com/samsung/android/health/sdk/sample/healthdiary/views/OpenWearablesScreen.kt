package com.samsung.android.health.sdk.sample.healthdiary.views

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.health.sdk.sample.healthdiary.repository.HealthProviderInfo
import com.samsung.android.health.sdk.sample.healthdiary.repository.OpenWearablesState
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.OpenWearablesViewModel

/**
 * OpenWearables Health Connect Screen
 * 
 * Provides UI for:
 * - Selecting health data provider (Samsung Health / Health Connect)
 * - Connecting to OpenWearables backend
 * - Managing health data sync
 * - Viewing sync status and latest health data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenWearablesScreen(
    modifier: Modifier = Modifier,
    viewModel: OpenWearablesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    
    // Handle error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Health Connect",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = SandboxPrimary
        )
        
        Text(
            text = "Conecta tus dispositivos wearables para sincronizar datos de salud automáticamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = SandboxSecondary
        )
        
        // Connection Status Card
        ConnectionStatusCard(
            connectionState = connectionState,
            onConnect = { viewModel.requestAuthorization() },
            onDisconnect = { viewModel.disconnect() }
        )
        
        // Provider Selection
        if (uiState.providers.isNotEmpty()) {
            ProviderSelectionSection(
                providers = uiState.providers,
                selectedProvider = uiState.selectedProvider,
                onProviderSelected = { viewModel.selectProvider(it) }
            )
        }
        
        // Sync Controls (only when connected)
        if (connectionState is OpenWearablesState.Connected || uiState.isAuthorized) {
            SyncControlsSection(
                isSyncing = uiState.isSyncing,
                lastSyncTime = uiState.lastSyncTime,
                onSyncNow = { viewModel.syncNow() },
                onStartSync = { viewModel.startSync() },
                onStopSync = { viewModel.stopSync() },
                onResetSync = { viewModel.resetSync() }
            )
        }
        
        // Health Data Preview
        uiState.healthData?.let { data ->
            HealthDataPreviewSection(healthData = data)
        }
        
        // Sync Status Details
        if (syncStatus.isNotEmpty()) {
            SyncStatusSection(syncStatus = syncStatus)
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SandboxPrimary)
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: OpenWearablesState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is OpenWearablesState.Connected -> SandboxSuccess.copy(alpha = 0.1f)
                is OpenWearablesState.Error -> SandboxError.copy(alpha = 0.1f)
                is OpenWearablesState.Syncing -> SandboxWarning.copy(alpha = 0.1f)
                else -> SandboxSurface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Icon
            Icon(
                imageVector = when (connectionState) {
                    is OpenWearablesState.Connected -> Icons.Default.CheckCircle
                    is OpenWearablesState.Syncing -> Icons.Default.Sync
                    is OpenWearablesState.Connecting -> Icons.Default.Sync
                    is OpenWearablesState.Error -> Icons.Default.Error
                    else -> Icons.Default.WatchOff
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = when (connectionState) {
                    is OpenWearablesState.Connected -> SandboxSuccess
                    is OpenWearablesState.Error -> SandboxError
                    is OpenWearablesState.Syncing, is OpenWearablesState.Connecting -> SandboxWarning
                    else -> SandboxSecondary
                }
            )
            
            // Status Text
            Text(
                text = when (connectionState) {
                    is OpenWearablesState.Connected -> "Conectado a ${connectionState.provider}"
                    is OpenWearablesState.Syncing -> "Sincronizando..."
                    is OpenWearablesState.Connecting -> "Conectando..."
                    is OpenWearablesState.Error -> "Error: ${connectionState.message}"
                    is OpenWearablesState.Disconnected -> "Desconectado"
                    is OpenWearablesState.NotInitialized -> "No inicializado"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            // Action Button
            when (connectionState) {
                is OpenWearablesState.Connected, is OpenWearablesState.Syncing -> {
                    OutlinedButton(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SandboxError
                        )
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Desconectar")
                    }
                }
                is OpenWearablesState.Disconnected, is OpenWearablesState.Error, is OpenWearablesState.NotInitialized -> {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SandboxPrimary
                        )
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Conectar Wearable")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ProviderSelectionSection(
    providers: List<HealthProviderInfo>,
    selectedProvider: String?,
    onProviderSelected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Fuente de Datos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SandboxPrimary
        )
        
        providers.forEach { provider ->
            ProviderCard(
                provider = provider,
                isSelected = provider.id == selectedProvider,
                onSelect = { onProviderSelected(provider.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderCard(
    provider: HealthProviderInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = if (provider.isAvailable) onSelect else ({}),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> SandboxPrimary.copy(alpha = 0.1f)
                !provider.isAvailable -> SandboxSurface.copy(alpha = 0.5f)
                else -> SandboxSurface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(width = 2.dp)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = when (provider.id) {
                    "samsung" -> Icons.Default.Watch
                    "google" -> Icons.Default.FitnessCenter
                    else -> Icons.Default.DeviceHub
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (provider.isAvailable) SandboxPrimary else SandboxSecondary.copy(alpha = 0.5f)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (provider.isAvailable) SandboxOnSurface else SandboxSecondary
                )
                Text(
                    text = if (provider.isAvailable) "Disponible" else "No disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (provider.isAvailable) SandboxSuccess else SandboxSecondary
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = SandboxPrimary
                )
            }
        }
    }
}

@Composable
private fun SyncControlsSection(
    isSyncing: Boolean,
    lastSyncTime: String?,
    onSyncNow: () -> Unit,
    onStartSync: () -> Unit,
    onStopSync: () -> Unit,
    onResetSync: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sincronización",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SandboxPrimary
            )
            
            lastSyncTime?.let {
                Text(
                    text = "Última: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = SandboxSecondary
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSyncNow,
                enabled = !isSyncing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SandboxPrimary)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SandboxOnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSyncing) "Sincronizando..." else "Sincronizar Ahora")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onStartSync,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Iniciar Auto")
            }
            
            OutlinedButton(
                onClick = onStopSync,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Detener")
            }
        }
        
        TextButton(
            onClick = onResetSync,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reiniciar Sincronización", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun HealthDataPreviewSection(healthData: Map<String, Any?>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Datos Recientes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SandboxPrimary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SandboxSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                healthData.forEach { (key, value) ->
                    HealthDataRow(
                        label = formatDataLabel(key),
                        value = formatDataValue(value),
                        icon = getIconForDataType(key)
                    )
                }
                
                if (healthData.isEmpty()) {
                    Text(
                        text = "No hay datos disponibles. Sincroniza para obtener los datos más recientes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SandboxSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthDataRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = SandboxPrimary
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SandboxPrimary
        )
    }
}

@Composable
private fun SyncStatusSection(syncStatus: Map<String, Any?>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Estado de Sincronización",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SandboxSecondary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SandboxSurface.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                syncStatus.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.labelSmall,
                            color = SandboxSecondary
                        )
                        Text(
                            text = value?.toString() ?: "-",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatDataLabel(key: String): String {
    return when (key) {
        "steps" -> "Pasos"
        "heartRate" -> "Ritmo Cardíaco"
        "sleep" -> "Sueño"
        "oxygenSaturation" -> "SpO2"
        "calories" -> "Calorías"
        "distance" -> "Distancia"
        else -> key.replaceFirstChar { it.uppercase() }
    }
}

private fun formatDataValue(value: Any?): String {
    return when (value) {
        null -> "-"
        is Number -> {
            if (value.toDouble() == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                String.format("%.1f", value.toDouble())
            }
        }
        is Map<*, *> -> {
            // For complex data like sleep, extract duration or summary
            value["duration"]?.toString() ?: value["value"]?.toString() ?: "-"
        }
        else -> value.toString()
    }
}

private fun getIconForDataType(key: String): ImageVector {
    return when (key) {
        "steps" -> Icons.Default.DirectionsWalk
        "heartRate" -> Icons.Default.Favorite
        "sleep" -> Icons.Default.Bedtime
        "oxygenSaturation" -> Icons.Default.Air
        "calories" -> Icons.Default.LocalFireDepartment
        "distance" -> Icons.Default.Straighten
        else -> Icons.Default.Analytics
    }
}
