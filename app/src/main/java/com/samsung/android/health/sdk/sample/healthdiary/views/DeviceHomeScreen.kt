package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairedDeviceEntity
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.DeviceHomeViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogoutState
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Device-Focused Home Screen
 * 
 * New simplified home screen showing only connected watch devices.
 * Replaces the previous feature-rich home screen.
 * 
 * Features:
 * - List of paired devices with aliases
 * - Connection status indicators
 * - Device alias management
 * - Empty state with call-to-action
 */
@Composable
fun DeviceHomeScreen(
    onNavigateToLogs: () -> Unit,
    onNavigateToTxAgent: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onUploadPdf: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToSandboxGallery: (() -> Unit)? = null,
    profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val viewModel = remember { DeviceHomeViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val logoutState by profileViewModel.logoutState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Handle logout success
    LaunchedEffect(logoutState) {
        if (logoutState is LogoutState.Success) {
            profileViewModel.resetState()
            onLogout()
        }
    }

    // Show error snackbar if needed
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with logout button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                
                SandboxHeader(
                    title = "Health Diary v1.3.3",
                    variant = HeaderVariant.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                // Logout button
                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device List or Empty State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SandboxLoader(variant = LoaderVariant.Large)
                }
            } else if (uiState.devices.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SandboxEmptyState(
                        title = "No devices connected yet",
                        message = "Connect your watch device to start tracking health data",
                        icon = Icons.Default.Watch,
                        action = {
                            SandboxButton(
                                text = "Connect a Device",
                                onClick = onNavigateToSettings,
                                icon = Icons.Default.Add,
                                fullWidth = false
                            )
                        }
                    )
                }
            } else {
                // Device List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.devices, key = { it.deviceId }) { device ->
                        DeviceCard(
                            device = device,
                            onEditAlias = { deviceId, newAlias ->
                                viewModel.updateDeviceAlias(deviceId, newAlias)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Floating action: Connect new device
                SandboxButton(
                    text = "Connect Another Device",
                    onClick = onNavigateToSettings,
                    icon = Icons.Default.Add,
                    variant = ButtonVariant.Secondary,
                    fullWidth = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Button
            SandboxIconButton(
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings,
                contentDescription = "Settings"
            )
        }
    }

    // Device Alias Dialog
    if (uiState.showAliasDialog != null) {
        DeviceAliasDialog(
            deviceName = uiState.showAliasDialog!!.name,
            onSave = { alias -> viewModel.saveDeviceAlias(alias) },
            onSkip = { viewModel.skipAlias() },
            onDismiss = { viewModel.dismissAliasDialog() }
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        profileViewModel.logout(context)
                    }
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Loading overlay during logout
    if (logoutState is LogoutState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Device Card Component
 * Shows device information and connection status
 */
@Composable
private fun DeviceCard(
    device: PairedDeviceEntity,
    onEditAlias: (String, String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    SandboxCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showEditDialog = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Device name or alias
                Text(
                    text = device.alias ?: device.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Show device name if alias is set
                if (device.alias != null) {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Connection Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConnectionStatusBadge(status = device.connectionStatus)
                    
                    // Last sync time
                    if (device.lastSyncTimestamp != null) {
                        Text(
                            text = "• Last sync: ${formatTimestamp(device.lastSyncTimestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Edit icon
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit alias",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    // Edit Alias Dialog
    if (showEditDialog) {
        var aliasText by remember { mutableStateOf(device.alias ?: "") }
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Device Alias") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Device: ${device.deviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SandboxInput(
                        value = aliasText,
                        onValueChange = { aliasText = it },
                        label = "Alias",
                        placeholder = "Enter friendly name"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditAlias(device.deviceId, aliasText.trim())
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Connection Status Badge
 */
@Composable
private fun ConnectionStatusBadge(status: String) {
    val (color, text) = when (status) {
        "VERIFIED" -> MaterialTheme.colorScheme.primary to "Connected"
        "CONNECTED" -> MaterialTheme.colorScheme.tertiary to "Connecting"
        "DISCONNECTED" -> MaterialTheme.colorScheme.error to "Disconnected"
        else -> MaterialTheme.colorScheme.outline to status
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Format timestamp to readable string
 */
private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
