package com.samsung.android.health.sdk.sample.healthdiary.update.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.VersionInfo

@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Launch check on start if idle? Or assume caller did it?
    // Let's assume caller calls checkForUpdates() or we do it here.
    LaunchedEffect(Unit) {
        if (uiState is UpdateUiState.Idle) {
            viewModel.checkForUpdates()
        }
    }

    when (val state = uiState) {
        is UpdateUiState.Idle -> {
            // Do nothing or show loading if triggered manually?
            // If idle and we just launched check, it will switch to Checking soon.
        }
        is UpdateUiState.Checking -> {
            // Optional: Show loading indicator
        }
        is UpdateUiState.UpdateAvailable -> {
            UpdateAvailableDialog(
                versionInfo = state.versionInfo,
                isMobile = state.isMobile,
                onUpdate = {
                    viewModel.startDownload(state.versionInfo, state.isMobile)
                },
                onDismiss = if (state.versionInfo.force) null else {
                    {
                        viewModel.dismissError()
                        onDismiss()
                    }
                }
            )
        }
        is UpdateUiState.Downloading -> {
            DownloadingDialog(progress = state.progress)
        }
        is UpdateUiState.ReadyToInstall -> {
            ReadyToInstallDialog(
                onInstall = {
                    viewModel.installUpdate(state.file)
                },
                onDismiss = {
                    viewModel.dismissError()  // Reset state to Idle
                    onDismiss()
                }
            )
        }
        is UpdateUiState.Error -> {
            AlertDialog(
                onDismissRequest = {
                    viewModel.dismissError()
                    onDismiss()
                },
                title = { Text("Update Error") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.dismissError()
                        onDismiss()
                    }) {
                        Text("OK")
                    }
                }
            )
        }
        is UpdateUiState.Completed -> {
             AlertDialog(
                onDismissRequest = {
                    viewModel.dismissError()
                    onDismiss()
                },
                title = { Text("Update Sent") },
                text = { Text("The update has been sent to your watch. Please check your watch to confirm installation.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.dismissError()
                        onDismiss()
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun UpdateAvailableDialog(
    versionInfo: VersionInfo,
    isMobile: Boolean,
    onUpdate: () -> Unit,
    onDismiss: (() -> Unit)?
) {
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text("Update Available") },
        text = {
            Column {
                Text("A new version ${versionInfo.version} is available for ${if (isMobile) "Mobile" else "Watch"}.")
                if (versionInfo.force) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This update is required.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Text("Update Now")
            }
        },
        dismissButton = {
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        },
        properties = DialogProperties(dismissOnBackPress = onDismiss != null, dismissOnClickOutside = onDismiss != null)
    )
}

@Composable
fun DownloadingDialog(progress: Int) {
    Dialog(
        onDismissRequest = { /* Prevent dismiss */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Downloading Update...", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("$progress%")
            }
        }
    }
}

@Composable
fun ReadyToInstallDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Complete") },
        text = { Text("The update is ready to be installed.") },
        confirmButton = {
            Button(onClick = onInstall) {
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
