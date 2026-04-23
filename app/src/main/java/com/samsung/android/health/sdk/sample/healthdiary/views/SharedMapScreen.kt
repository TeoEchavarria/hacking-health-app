package com.samsung.android.health.sdk.sample.healthdiary.views

import android.Manifest
import android.widget.Toast
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.samsung.android.health.sdk.sample.healthdiary.components.MapMarker
import com.samsung.android.health.sdk.sample.healthdiary.components.OsmMapView
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.SharedMapViewModel

/**
 * Shared Map Screen
 * 
 * Full-screen OpenStreetMap showing:
 * - Current user's location (blue marker)
 * - Paired user's location (red marker)
 * - Controls for centering and sharing toggle
 * 
 * Features:
 * - Real-time GPS updates
 * - Polling for paired user's location every 30 seconds
 * - Location sharing toggle
 * - Background sync service control
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SharedMapScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: SharedMapViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // Location permission
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    
    // Handle lifecycle for tracking
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (locationPermissionState.status.isGranted) {
                        viewModel.startTracking()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopTracking()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Show error toast
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Main content based on permission state
        if (!locationPermissionState.status.isGranted) {
            LocationPermissionContent(
                showRationale = locationPermissionState.status.shouldShowRationale,
                onRequestPermission = { locationPermissionState.launchPermissionRequest() },
                onNavigateBack = onNavigateBack
            )
        } else {
            // Map content
            SharedMapContent(
                markers = uiState.getMarkers(),
                isLoading = uiState.isLoading,
                isSharingEnabled = uiState.isSharingEnabled,
                isLocationSyncRunning = uiState.isLocationSyncRunning,
                pairedUserName = uiState.pairedUserName,
                onCenterOnMe = { viewModel.centerOnMyLocation() },
                onRefresh = { viewModel.refreshPairedLocation() },
                onToggleSharing = { viewModel.toggleSharing(it) },
                onToggleSyncService = { enabled ->
                    if (enabled) {
                        viewModel.startLocationSyncService()
                    } else {
                        viewModel.stopLocationSyncService()
                    }
                },
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
private fun SharedMapContent(
    markers: List<MapMarker>,
    isLoading: Boolean,
    isSharingEnabled: Boolean,
    isLocationSyncRunning: Boolean,
    pairedUserName: String?,
    onCenterOnMe: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSharing: (Boolean) -> Unit,
    onToggleSyncService: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // OpenStreetMap
        OsmMapView(
            markers = markers,
            centerOnMarker = "me",
            zoomLevel = 16.0,
            modifier = Modifier.fillMaxSize()
        )
        
        // Top bar with back button and title
        TopBar(
            onNavigateBack = onNavigateBack,
            pairedUserName = pairedUserName,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Control buttons (right side)
        ControlButtons(
            isSharingEnabled = isSharingEnabled,
            isLocationSyncRunning = isLocationSyncRunning,
            onCenterOnMe = onCenterOnMe,
            onRefresh = onRefresh,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
        
        // Bottom info card
        BottomInfoCard(
            isSharingEnabled = isSharingEnabled,
            isLocationSyncRunning = isLocationSyncRunning,
            pairedUserName = pairedUserName,
            onToggleSharing = onToggleSharing,
            onToggleSyncService = onToggleSyncService,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                color = SandboxPrimary
            )
        }
    }
}

@Composable
private fun TopBar(
    onNavigateBack: () -> Unit,
    pairedUserName: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = SandboxOnSurface
            )
        }
        
        // Title
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Ubicación Compartida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SandboxOnSurface
            )
            if (pairedUserName != null) {
                Text(
                    text = "Con $pairedUserName",
                    style = MaterialTheme.typography.bodySmall,
                    color = SandboxSecondary
                )
            }
        }
    }
}

@Composable
private fun ControlButtons(
    isSharingEnabled: Boolean,
    isLocationSyncRunning: Boolean,
    onCenterOnMe: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Center on me
        FloatingActionButton(
            onClick = onCenterOnMe,
            containerColor = Color.White,
            contentColor = SandboxPrimary,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Mi ubicación"
            )
        }
        
        // Refresh paired location
        FloatingActionButton(
            onClick = onRefresh,
            containerColor = Color.White,
            contentColor = SandboxPrimary,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Actualizar"
            )
        }
    }
}

@Composable
private fun BottomInfoCard(
    isSharingEnabled: Boolean,
    isLocationSyncRunning: Boolean,
    pairedUserName: String?,
    onToggleSharing: (Boolean) -> Unit,
    onToggleSyncService: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sharing toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSharingEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (isSharingEnabled) SandboxPrimary else SandboxSecondary
                    )
                    Column {
                        Text(
                            text = "Compartir mi ubicación",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isSharingEnabled) "Visible para $pairedUserName" else "Oculto",
                            style = MaterialTheme.typography.bodySmall,
                            color = SandboxSecondary
                        )
                    }
                }
                
                Switch(
                    checked = isSharingEnabled,
                    onCheckedChange = onToggleSharing,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SandboxPrimary,
                        checkedTrackColor = SandboxPrimaryContainer
                    )
                )
            }
            
            Divider(color = SandboxOutline.copy(alpha = 0.3f))
            
            // Sync service toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = if (isLocationSyncRunning) SandboxPrimary else SandboxSecondary
                    )
                    Column {
                        Text(
                            text = "Sincronización continua",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isLocationSyncRunning) "Activa (cada 5 min)" else "Desactivada",
                            style = MaterialTheme.typography.bodySmall,
                            color = SandboxSecondary
                        )
                    }
                }
                
                Switch(
                    checked = isLocationSyncRunning,
                    onCheckedChange = onToggleSyncService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SandboxPrimary,
                        checkedTrackColor = SandboxPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun LocationPermissionContent(
    showRationale: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = SandboxSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Permiso de ubicación requerido",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SandboxOnSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (showRationale) {
                "Para compartir tu ubicación con tu familiar vinculado, necesitamos acceso a tu GPS."
            } else {
                "Activa el permiso de ubicación en la configuración de la app para usar esta función."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = SandboxSecondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = SandboxPrimary
            )
        ) {
            Text("Conceder permiso")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onNavigateBack) {
            Text("Volver", color = SandboxSecondary)
        }
    }
}
