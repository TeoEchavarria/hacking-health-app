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
 * Previous Version Screen
 * 
 * This screen contains the OLD home UI (DeviceHomeScreen) preserved for reference.
 * Accessible via: Settings → Previous Version
 * 
 * Features:
 * - List of paired devices with aliases
 * - Connection status indicators
 * - Device alias management
 * - "Connect Another Device" button (legacy)
 * - "Return to new version" navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousVersionScreen(
    onNavigateToLogs: () -> Unit,
    onNavigateToTxAgent: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onUploadPdf: () -> Unit,
    onLogout: () -> Unit = {},
    onReturnToNewVersion: () -> Unit,
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
        topBar = {
            SandboxTopBar(
                title = "Previous Version",
                onNavigationClick = onReturnToNewVersion
            )
        },
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
                    .padding(vertical = 16.dp),
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

            // Info banner about this being the old version
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "This is the previous version of the home screen. Use the back button to return to the new version.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        PreviousVersionDeviceCard(
                            device = device,
                            onEditAlias = { deviceId, newAlias ->
                                viewModel.updateDeviceAlias(deviceId, newAlias)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Floating action: Connect new device (LEGACY - kept for previous version)
                SandboxButton(
                    text = "Connect Another Device",
                    onClick = onNavigateToSettings,
                    icon = Icons.Default.Add,
                    variant = ButtonVariant.Secondary,
                    fullWidth = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Return to new version button
            SandboxButton(
                text = "Return to New Version",
                onClick = onReturnToNewVersion,
                icon = Icons.Default.ArrowBack,
                variant = ButtonVariant.Primary,
                fullWidth = true
            )

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
 * Device Card Component for Previous Version Screen
 * Shows device information and connection status
 */
@Composable
private fun PreviousVersionDeviceCard(
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
                
                // Show original name if alias is set
                if (device.alias != null) {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Connection status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusColor, statusIcon) = when (device.connectionStatus) {
                        "CONNECTED", "VERIFIED" -> 
                            MaterialTheme.colorScheme.primary to Icons.Default.CheckCircle
                        "CONNECTING" -> 
                            MaterialTheme.colorScheme.tertiary to Icons.Default.Sync
                        else -> 
                            MaterialTheme.colorScheme.outline to Icons.Default.Cancel
                    }
                    
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = device.connectionStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                
                // Last sync time
                device.lastSyncTimestamp?.let { timestamp ->
                    Text(
                        text = "Last sync: ${formatTimestamp(timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Edit button
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit alias"
                )
            }
        }
    }

    // Edit alias dialog
    if (showEditDialog) {
        var newAlias by remember { mutableStateOf(device.alias ?: "") }
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Device Alias") },
            text = {
                OutlinedTextField(
                    value = newAlias,
                    onValueChange = { newAlias = it },
                    label = { Text("Alias (e.g., Dad, Mom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditAlias(device.deviceId, newAlias)
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

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} min ago"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
    }
}
