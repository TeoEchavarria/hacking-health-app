package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToLogs: () -> Unit,
    onNavigateToTxAgent: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onUploadPdf: () -> Unit,
    onNavigateToSandboxGallery: (() -> Unit)? = null,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        SandboxHeader(
            title = "Health Diary v1.2.8 ✓",
            variant = HeaderVariant.Medium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

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

@Composable
fun ProfileCard(profile: com.samsung.android.health.sdk.sample.healthdiary.viewmodel.UserProfile) {
    SandboxCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProfileStat("Age", "${profile.age}")
            ProfileStat("Gender", profile.gender)
            ProfileStat("Height", "${profile.height} cm")
            ProfileStat("Weight", "${profile.weight} kg")
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SyncStatusCard(lastSyncTime: String, isSyncing: Boolean, onSyncClick: () -> Unit) {
    SandboxCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Last Sync with Watch",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lastSyncTime,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isSyncing) {
                SandboxLoader(variant = LoaderVariant.Small)
            } else {
                SandboxIconButton(
                    icon = Icons.Default.Refresh,
                    onClick = onSyncClick,
                    contentDescription = "Sync Now"
                )
            }
        }
    }
}

