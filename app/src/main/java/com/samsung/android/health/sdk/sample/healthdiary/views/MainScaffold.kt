package com.samsung.android.health.sdk.sample.healthdiary.views

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.activity.LoginActivity
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxBackground
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HomeViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.LogoutState
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ProfileViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.DeviceConnectionStatus

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
    onNavigateToProfile: () -> Unit = {},
    onNavigateToTraining: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToHeartRateHistory: () -> Unit = {},
    onNavigateToStepsHistory: () -> Unit = {},
    onNavigateToSleepHistory: () -> Unit = {},
    onNavigateToAddMedication: () -> Unit = {},
    onNavigateToDailyChallenge: () -> Unit = {},
    onLogout: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(BottomNavTab.DASHBOARD) }
    var showTrackingScreen by remember { mutableStateOf(false) }
    val logoutState by profileViewModel.logoutState.collectAsState()
    
    // User profile data from HomeViewModel
    val homeUiState by homeViewModel.uiState.collectAsState()
    val userName = homeUiState.userProfile.name.split(" ").firstOrNull() ?: ""
    
    // Health Dashboard ViewModel for connection status
    val healthDashboardViewModel = remember { 
        com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthDashboardViewModel(context) 
    }
    val dashboardUiState by healthDashboardViewModel.uiState.collectAsState()
    
    // Handle logout success
    LaunchedEffect(logoutState) {
        if (logoutState is LogoutState.Success) {
            profileViewModel.resetState()
            onLogout()
        }
    }
    
    Scaffold(
        topBar = {
            // Determine connection status from dashboardViewModel
            val isConnected = when (dashboardUiState.connectionStatus) {
                is DeviceConnectionStatus.Connected,
                is DeviceConnectionStatus.Verified -> true
                else -> false
            }
            
            TuSaludTopBar(
                userName = userName,
                isConnected = isConnected,
                onSensorsClick = onNavigateToSettings,
                onAvatarClick = onNavigateToProfile
            )
        },
        bottomBar = {
            TuSaludBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> 
                    selectedTab = tab
                    showTrackingScreen = false // Reset tracking screen when changing tabs
                }
            )
        },
        containerColor = SandboxBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main tab content
            if (!showTrackingScreen) {
                when (selectedTab) {
                    BottomNavTab.DASHBOARD -> {
                        DashboardTabContent(
                            userName = userName,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToTraining = onNavigateToTraining,
                            onNavigateToHabits = onNavigateToHabits,
                            onNavigateToHeartRateHistory = onNavigateToHeartRateHistory,
                            onNavigateToStepsHistory = onNavigateToStepsHistory,
                            onNavigateToSleepHistory = onNavigateToSleepHistory,
                            onNavigateToDailyChallenge = onNavigateToDailyChallenge
                        )
                    }
                    BottomNavTab.VITALS -> {
                        VitalsScreen(
                            onNavigateToTracking = { showTrackingScreen = true }
                        )
                    }
                    BottomNavTab.CALENDAR -> {
                        CalendarScreen(
                            onNavigateToHeartRateHistory = onNavigateToHeartRateHistory,
                            onNavigateToAddMedication = onNavigateToAddMedication
                        )
                    }
                }
            } else {
                // Tracking screen overlay
                TrackingScreen()
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
 * - Health tip nudge card
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
    onNavigateToDailyChallenge: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        val minContentHeight = 450.dp // Minimum height needed for content
        val needsScroll = maxHeight < minContentHeight
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (needsScroll) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                ),
            verticalArrangement = if (needsScroll) Arrangement.spacedBy(24.dp) else Arrangement.SpaceBetween
        ) {
            // Top Section: Daily Challenge Card
            DailyChallengeCard(
                onClick = onNavigateToDailyChallenge
            )
            
            // Middle Section: AI Interaction
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (needsScroll) Modifier.heightIn(min = 200.dp)
                        else Modifier.weight(1f)
                    ),
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
            
            // Bottom Section: Emergency Button (fixed size)
            EmergencyButton(
                onClick = {
                    // Placeholder - no emergency action yet
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Daily Challenge Card
 * 
 * A card that navigates to the daily cognitive exercises.
 */
@Composable
private fun DailyChallengeCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6),
                            Color(0xFFA855F7)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Text
                Column {
                    Text(
                        text = "Reto Diario",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Ejercita tu mente con números, palabras y dibujos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
