package com.samsung.android.health.sdk.sample.healthdiary.views

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.activity.LoginActivity
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogoutState
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ProfileViewModel

/**
 * Profile Screen - User Profile & Settings
 * 
 * Displays:
 * - User profile header with avatar
 * - Current role (Caregiver/Patient)
 * - Active family connections
 * - Settings menu
 * - Logout option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileUiState by profileViewModel.profileUiState.collectAsState()
    val logoutState by profileViewModel.logoutState.collectAsState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle logout states
    LaunchedEffect(logoutState) {
        when (logoutState) {
            is LogoutState.Success -> {
                // Navigate to login and clear back stack
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                profileViewModel.resetState()
            }
            is LogoutState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (logoutState as LogoutState.Error).message,
                    duration = SnackbarDuration.Short
                )
                profileViewModel.resetState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mi Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SandboxPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = SandboxPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Configuración",
                            tint = SandboxPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SandboxBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (profileUiState.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SandboxLoader(variant = LoaderVariant.Large)
                }
            } else if (profileUiState.error != null) {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = profileUiState.error ?: "Error desconocido",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SandboxError
                        )
                        Button(
                            onClick = { profileViewModel.refreshProfile() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SandboxPrimary
                            )
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            } else {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Profile Header
                    ProfileHeader(
                        name = profileUiState.userProfile.name,
                        email = profileUiState.userProfile.email,
                        avatarUrl = profileUiState.userProfile.avatarUrl,
                        onEditClick = {
                            // TODO: Navigate to profile edit screen
                        }
                    )
                    
                    // Current Role Card
                    CurrentRoleCard(
                        role = profileUiState.userProfile.role,
                        protectedPersonName = if (profileUiState.userProfile.role == UserRole.CAREGIVER) {
                            profileUiState.connections.firstOrNull()?.name
                        } else null,
                        isActive = true
                    )
                    
                    // Connections List
                    ConnectionsList(
                        connections = profileUiState.connections,
                        onConnectionClick = { connection ->
                            // TODO: Navigate to connection details
                        },
                        onAddConnectionClick = {
                            // TODO: Navigate to pairing activity
                        },
                        onManageClick = {
                            // TODO: Navigate to manage connections screen
                        }
                    )
                    
                    // Settings Menu
                    SettingsMenuList(
                        onAccountClick = onNavigateToSettings,
                        onNotificationsClick = onNavigateToNotifications,
                        onSecurityClick = onNavigateToSecurity
                    )
                    
                    // Logout Button
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SandboxErrorContainer,
                            contentColor = SandboxOnErrorContainer
                        ),
                        shape = RoundedCornerShape(24.dp),
                        enabled = logoutState !is LogoutState.Loading
                    ) {
                        if (logoutState is LogoutState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = SandboxOnErrorContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Cerrar Sesión",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Cerrar Sesión",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas cerrar sesión?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        profileViewModel.logout(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SandboxError
                    )
                ) {
                    Text("Cerrar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    SandboxTheme {
        ProfileScreen()
    }
}
