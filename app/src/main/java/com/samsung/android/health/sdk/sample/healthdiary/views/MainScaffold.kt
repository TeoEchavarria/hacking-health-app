package com.samsung.android.health.sdk.sample.healthdiary.views

import android.Manifest
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.samsung.android.health.sdk.sample.healthdiary.activity.LoginActivity
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.components.BPVoiceFlow.BPVoiceFlowDialog
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxBackground
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BloodPressureVoiceViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
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
    onNavigateToSharedMap: () -> Unit = {},
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
                            onNavigateToTracking = onNavigateToSharedMap
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
 * - AI interaction button (Blood Pressure Voice Flow)
 * - Emergency button
 */
@OptIn(ExperimentalPermissionsApi::class)
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
    modifier: Modifier = Modifier,
    bpVoiceViewModel: BloodPressureVoiceViewModel = viewModel(
        factory = HealthViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    
    // BP Voice Flow state
    val bpVoiceState by bpVoiceViewModel.uiState.collectAsState()
    
    // Snackbar host state for notifications
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Audio permission state for voice recording
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // Track if we should start listening after permission is granted
    var pendingStartListening by remember { mutableStateOf(false) }
    
    // Start listening when permission is granted after request
    LaunchedEffect(audioPermissionState.status.isGranted, pendingStartListening) {
        if (audioPermissionState.status.isGranted && pendingStartListening) {
            pendingStartListening = false
            bpVoiceViewModel.startListening()
        }
    }
    
    // Track processing state and show notifications
    var lastIsUploading by remember { mutableStateOf(false) }
    
    LaunchedEffect(bpVoiceState.isUploading) {
        if (bpVoiceState.isUploading && !lastIsUploading) {
            // Started uploading - show processing notification
            snackbarHostState.showSnackbar(
                message = "📤 Procesando registro de presión...",
                duration = SnackbarDuration.Indefinite
            )
        } else if (!bpVoiceState.isUploading && lastIsUploading) {
            // Finished uploading - dismiss and show result
            snackbarHostState.currentSnackbarData?.dismiss()
            if (bpVoiceState.parseResult != null) {
                snackbarHostState.showSnackbar(
                    message = "✅ Registro procesado: ${bpVoiceState.parseResult?.systolic ?: "--"}/${bpVoiceState.parseResult?.diastolic ?: "--"} mmHg",
                    duration = SnackbarDuration.Short
                )
            } else if (bpVoiceState.error != null) {
                snackbarHostState.showSnackbar(
                    message = "❌ Error: ${bpVoiceState.error}",
                    duration = SnackbarDuration.Short
                )
            }
        }
        lastIsUploading = bpVoiceState.isUploading
    }
    
    // Show success notification when submission completes
    LaunchedEffect(bpVoiceState.submissionSuccess) {
        if (bpVoiceState.submissionSuccess) {
            snackbarHostState.showSnackbar(
                message = "✅ Se ha guardado un nuevo registro de presión arterial",
                duration = SnackbarDuration.Short
            )
        }
    }
    
    // Show dialog when any flow is active
    val showBPDialog = bpVoiceState.isListening || 
                       bpVoiceState.isUploading ||  // Include uploading state
                       bpVoiceState.isParsing || 
                       bpVoiceState.showLowConfidenceDialog || 
                       bpVoiceState.showCrisisDialog || 
                       bpVoiceState.showSuccessDialog ||
                       bpVoiceState.error != null
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier
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
                    isListening = bpVoiceState.isListening,
                    onTap = {
                        // Check for RECORD_AUDIO permission before starting
                        if (audioPermissionState.status.isGranted) {
                            bpVoiceViewModel.startListening()
                        } else {
                            // Request permission and set flag to start listening when granted
                            pendingStartListening = true
                            audioPermissionState.launchPermissionRequest()
                        }
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
        }  // Close BoxWithConstraints
        
        // Snackbar for notifications - positioned at bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }  // Close outer Box
    
    // BP Voice Flow Dialog
    if (showBPDialog) {
        BPVoiceFlowDialog(
            state = bpVoiceState,
            onDismiss = { bpVoiceViewModel.resetState() },
            onRetry = { bpVoiceViewModel.retryMeasurement() },
            onConfirm = { bpVoiceViewModel.confirmAndSubmit() },
            onEditValues = { s, d, p -> bpVoiceViewModel.editValues(s, d, p) },
            onOpenLanguageSettings = { bpVoiceViewModel.openLanguageSettings() },
            onStopRecording = { 
                android.util.Log.i("BPVoiceFlow", "🛑 User tapped Stop Recording")
                bpVoiceViewModel.stopRecording() 
            }
        )
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
