package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import com.samsung.android.health.sdk.sample.healthdiary.update.ui.UpdateDialog
import com.samsung.android.health.sdk.sample.healthdiary.update.ui.UpdateUiState
import com.samsung.android.health.sdk.sample.healthdiary.update.ui.UpdateViewModel
import com.samsung.android.health.sdk.sample.healthdiary.views.LoginScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SandboxTheme {
                val updateViewModel: UpdateViewModel = hiltViewModel()
                val updateState by updateViewModel.uiState.collectAsState()
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateCheckCompleted by remember { mutableStateOf(false) }
                
                // Check for updates on startup
                LaunchedEffect(Unit) {
                    updateViewModel.checkForUpdates()
                }
                
                // Show update dialog if update is available
                LaunchedEffect(updateState) {
                    when (updateState) {
                        is UpdateUiState.UpdateAvailable -> {
                            showUpdateDialog = true
                            updateCheckCompleted = true
                        }
                        is UpdateUiState.Idle -> {
                            // Only mark as completed if we're not in initial state
                            if (!updateCheckCompleted) {
                                updateCheckCompleted = true
                            }
                        }
                        is UpdateUiState.Error -> {
                            // Allow login even if update check fails
                            updateCheckCompleted = true
                        }
                        else -> {}
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Show login screen
                        LoginScreen()
                        
                        // Show update dialog on top if needed
                        if (showUpdateDialog) {
                            UpdateDialog(
                                viewModel = updateViewModel,
                                onDismiss = {
                                    // Check if this is a forced update
                                    val state = updateState
                                    if (state is UpdateUiState.UpdateAvailable && state.versionInfo.force) {
                                        // Don't dismiss for forced updates
                                    } else {
                                        showUpdateDialog = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

