package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LegacyHomeViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogoutState
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ProfileViewModel

/**
 * Legacy Home Screen - Previous version preserved for reference
 * This screen contains the original feature-rich home UI.
 * Access: Settings → Previous Version
 */
@Composable
fun LegacyHomeScreen(
    onNavigateToLogs: () -> Unit,
    onNavigateToTxAgent: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onUploadPdf: () -> Unit,
    onLogout: () -> Unit = {},
    onReturnToNewVersion: () -> Unit,
    onNavigateToSandboxGallery: (() -> Unit)? = null,
    viewModel: LegacyHomeViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val logoutState by profileViewModel.logoutState.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Handle logout success
    LaunchedEffect(logoutState) {
        if (logoutState is LogoutState.Success) {
            profileViewModel.resetState()
            onLogout()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar with back navigation
        SandboxTopBar(
            title = "Previous Version",
            onNavigationClick = onReturnToNewVersion
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                Spacer(modifier = Modifier.width(48.dp)) // Balance for logout button
                
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

            // Profile Card
            ProfileCard(uiState.userProfile)

            Spacer(modifier = Modifier.height(24.dp))

            // Sync Status
            SyncStatusCard(
                lastSyncTime = uiState.lastSyncTime,
                isSyncing = uiState.isSyncing,
                onSyncClick = { viewModel.syncData() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            SandboxButton(
                text = "Daily Training Session",
                onClick = onNavigateToTraining,
                icon = Icons.Default.PlayArrow,
                fullWidth = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            SandboxButton(
                text = "Habit Reminders",
                onClick = onNavigateToHabits,
                icon = Icons.Default.Notifications,
                fullWidth = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            SandboxButton(
                text = "Logs / Docs",
                onClick = onNavigateToLogs,
                icon = Icons.Default.List,
                fullWidth = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            SandboxButton(
                text = "TxAgent Chat",
                onClick = onNavigateToTxAgent,
                icon = Icons.Default.Person,
                fullWidth = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            SandboxButton(
                text = "Upload Medical History",
                onClick = onUploadPdf,
                icon = Icons.Default.Add,
                fullWidth = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // Settings Button
            SandboxIconButton(
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings,
                contentDescription = "Settings"
            )
            
            // Sandbox Gallery (Debug only)
            if (onNavigateToSandboxGallery != null && com.samsung.android.health.sdk.sample.healthdiary.BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(8.dp))
                SandboxButton(
                    text = "Sandbox Gallery",
                    onClick = onNavigateToSandboxGallery,
                    variant = ButtonVariant.Text,
                    fullWidth = true
                )
            }
        }
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
    
    // Error message
    if (logoutState is LogoutState.Error) {
        LaunchedEffect(logoutState) {
            // Could show a Snackbar here if needed
            profileViewModel.resetState()
        }
    }
}
