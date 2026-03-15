package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.*

/**
 * Health Dashboard - New Main Screen
 * 
 * Focused view showing:
 * 1. Connected device status (real-time)
 * 2. Current heart rate
 * 3. Today's sleep hours
 * 4. Today's steps (cumulative daily total)
 * 
 * The "No watch connected" state is tappable and navigates to Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToHeartRateHistory: () -> Unit = {},
    onNavigateToStepsHistory: () -> Unit = {},
    onNavigateToSleepHistory: () -> Unit = {},
    onLogout: () -> Unit = {},
    profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val viewModel = remember { HealthDashboardViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val logoutState by profileViewModel.logoutState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle logout success
    LaunchedEffect(logoutState) {
        if (logoutState is LogoutState.Success) {
            profileViewModel.resetState()
            onLogout()
        }
    }

    // Show error snackbar
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp))

                SandboxHeader(
                    title = "Health Diary",
                    variant = HeaderVariant.Medium,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Connected Device Card - tappable if no device connected
            ConnectedDeviceCard(
                device = uiState.connectedDevice,
                connectionStatus = uiState.connectionStatus,
                lastSyncTime = uiState.lastSyncTime,
                onRefresh = { viewModel.refreshConnectionStatus() },
                isLoading = uiState.isLoadingDevice,
                onConnectClick = onNavigateToSettings,
                onSendTestMessage = { viewModel.sendTestMessageToWatch() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Health Metrics Section
            Text(
                text = "Today's Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Health Metrics Grid (3 cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Heart Rate
                HealthMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFE53935),
                    title = "Heart Rate",
                    value = uiState.currentHeartRate?.toString(),
                    unit = "bpm",
                    subtitle = uiState.heartRateTimestamp,
                    isLoading = uiState.isLoadingMetrics,
                    onClick = onNavigateToHeartRateHistory
                )

                // Sleep
                HealthMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bedtime,
                    iconColor = Color(0xFF5E35B1),
                    title = "Sleep",
                    value = uiState.todaySleepHours?.let { 
                        String.format("%.1f", it) 
                    },
                    unit = "hours",
                    subtitle = "Today",
                    isLoading = uiState.isLoadingMetrics,
                    onClick = onNavigateToSleepHistory
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Steps (full width)
            HealthMetricCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.DirectionsWalk,
                iconColor = Color(0xFF43A047),
                title = "Today's Steps",
                value = uiState.todayMaxSteps?.let { 
                    String.format("%,d", it) 
                },
                unit = "steps",
                subtitle = "Today's total steps",
                isLoading = uiState.isLoadingMetrics,
                isLarge = true,
                onClick = onNavigateToStepsHistory
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Quick Actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            SandboxButton(
                text = "Daily Training",
                onClick = onNavigateToTraining,
                icon = Icons.Default.PlayArrow,
                fullWidth = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            SandboxButton(
                text = "Habit Reminders",
                onClick = onNavigateToHabits,
                icon = Icons.Default.Notifications,
                variant = ButtonVariant.Secondary,
                fullWidth = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // Settings button
            SandboxIconButton(
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings,
                contentDescription = "Settings"
            )
        }
    }

    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        profileViewModel.logout(context)
                    }
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Loading overlay
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
 * Connected Device Card - Shows real-time connection status.
 * When no device is connected, the card is tappable to navigate to Settings.
 */
@Composable
private fun ConnectedDeviceCard(
    device: com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairedDeviceEntity?,
    connectionStatus: DeviceConnectionStatus,
    lastSyncTime: String?,
    onRefresh: () -> Unit,
    isLoading: Boolean,
    onConnectClick: () -> Unit = {},
    onSendTestMessage: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionStatus) {
                is DeviceConnectionStatus.Connected,
                is DeviceConnectionStatus.Verified -> MaterialTheme.colorScheme.primaryContainer
                is DeviceConnectionStatus.BluetoothOff -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = if (device == null && !isLoading) onConnectClick else ({})
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (device == null) {
            // No device paired - TAPPABLE to navigate to Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No watch connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap here to connect your watch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        } else {
            // Device present
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionStatus) {
                                is DeviceConnectionStatus.Connected,
                                is DeviceConnectionStatus.Verified -> Color(0xFF4CAF50)
                                is DeviceConnectionStatus.Searching -> Color(0xFFFFC107)
                                is DeviceConnectionStatus.BluetoothOff -> Color(0xFFF44336)
                                else -> Color(0xFF9E9E9E)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (connectionStatus) {
                            is DeviceConnectionStatus.Connected,
                            is DeviceConnectionStatus.Verified -> Icons.Default.Check
                            is DeviceConnectionStatus.Searching -> Icons.Default.Search
                            is DeviceConnectionStatus.BluetoothOff -> Icons.Default.BluetoothDisabled
                            else -> Icons.Default.LinkOff
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.alias ?: device.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (device.alias != null) {
                        Text(
                            text = device.deviceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = connectionStatus.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (connectionStatus) {
                            is DeviceConnectionStatus.Connected,
                            is DeviceConnectionStatus.Verified -> Color(0xFF2E7D32)
                            is DeviceConnectionStatus.BluetoothOff -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Medium
                    )
                    lastSyncTime?.let {
                        Text(
                            text = "Last sync: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Refresh button
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
                
                // Test connectivity button (send "You're connected" to watch)
                if (connectionStatus.isConnected) {
                    IconButton(onClick = onSendTestMessage) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Test Connection"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Health Metric Card - Displays a single health metric.
 */
@Composable
private fun HealthMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String?,
    unit: String,
    subtitle: String?,
    isLoading: Boolean,
    isLarge: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLarge) 20.dp else 16.dp),
            horizontalAlignment = if (isLarge) Alignment.Start else Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isLarge) Arrangement.Start else Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(if (isLarge) 28.dp else 24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(if (isLarge) 12.dp else 8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else if (value == null) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = if (isLarge) Arrangement.Start else Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = value,
                        style = if (isLarge) {
                            MaterialTheme.typography.headlineLarge
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                subtitle?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = if (isLarge) TextAlign.Start else TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
