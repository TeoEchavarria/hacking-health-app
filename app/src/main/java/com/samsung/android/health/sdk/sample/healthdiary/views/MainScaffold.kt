package com.samsung.android.health.sdk.sample.healthdiary.views

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.activity.LoginActivity
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxBackground
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HomeViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogoutState
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ProfileViewModel

/**
 * Main Scaffold - Tu Salud
 * 
 * Wrapper that provides:
 * - Fixed top app bar (TuSaludTopBar)
 * - Fixed bottom navigation (TuSaludBottomBar)
 * - Content area that changes based on selected tab
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTraining: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToHeartRateHistory: () -> Unit = {},
    onNavigateToStepsHistory: () -> Unit = {},
    onNavigateToSleepHistory: () -> Unit = {},
    onLogout: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(BottomNavTab.DASHBOARD) }
    val logoutState by profileViewModel.logoutState.collectAsState()
    
    // User profile data from HomeViewModel
    val homeUiState by homeViewModel.uiState.collectAsState()
    val userName = homeUiState.userProfile.name.split(" ").firstOrNull() ?: ""
    
    // Handle logout success
    LaunchedEffect(logoutState) {
        if (logoutState is LogoutState.Success) {
            profileViewModel.resetState()
            onLogout()
        }
    }
    
    Scaffold(
        topBar = {
            TuSaludTopBar(
                userName = userName,
                onSensorsClick = onNavigateToSettings
            )
        },
        bottomBar = {
            TuSaludBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab }
            )
        },
        containerColor = SandboxBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                BottomNavTab.DASHBOARD -> {
                    DashboardTabContent(
                        userName = userName,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToTraining = onNavigateToTraining,
                        onNavigateToHabits = onNavigateToHabits,
                        onNavigateToHeartRateHistory = onNavigateToHeartRateHistory,
                        onNavigateToStepsHistory = onNavigateToStepsHistory,
                        onNavigateToSleepHistory = onNavigateToSleepHistory
                    )
                }
                BottomNavTab.TRACKING -> {
                    TrackingScreen()
                }
                BottomNavTab.VITALS -> {
                    VitalsScreen()
                }
                BottomNavTab.CALENDAR -> {
                    CalendarScreen()
                }
            }
        }
    }
    
    // Loading overlay during logout
    if (logoutState is LogoutState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

/**
 * Dashboard Tab Content
 * 
 * Main dashboard view with:
 * - Wearable status card
 * - AI interaction button
 * - Emergency button
 */
@Composable
fun DashboardTabContent(
    userName: String = "",
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTraining: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToHeartRateHistory: () -> Unit = {},
    onNavigateToStepsHistory: () -> Unit = {},
    onNavigateToSleepHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { 
        com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthDashboardViewModel(context) 
    }
    val uiState by viewModel.uiState.collectAsState()
    
    // Determine connection status
    val isConnected = when (uiState.connectionStatus) {
        is com.samsung.android.health.sdk.sample.healthdiary.viewmodel.DeviceConnectionStatus.Connected,
        is com.samsung.android.health.sdk.sample.healthdiary.viewmodel.DeviceConnectionStatus.Verified -> true
        else -> false
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Wearable Status
        WearableStatusCard(
            deviceName = uiState.connectedDevice?.alias ?: "Dispositivo Wearable",
            deviceModel = uiState.connectedDevice?.deviceName ?: "Sin dispositivo conectado",
            isConnected = isConnected,
            isLoading = uiState.isLoadingDevice,
            onClick = onNavigateToSettings
        )
        
        // Middle Section: AI Interaction
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            AiInteractionButton(
                userName = userName,
                isListening = false, // Placeholder - no functionality yet
                onTap = {
                    // Placeholder - no action yet
                }
            )
        }
        
        // Bottom Section: Emergency Button
        EmergencyButton(
            onClick = {
                // Placeholder - no emergency action yet
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
