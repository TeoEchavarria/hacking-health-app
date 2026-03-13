package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.ViewModelProvider
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthRedirectActivity
import com.samsung.android.health.sdk.sample.healthdiary.repository.AuthRepository
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import com.samsung.android.health.sdk.sample.healthdiary.update.ui.UpdateDialog
import com.samsung.android.health.sdk.sample.healthdiary.update.ui.UpdateUiState
import com.samsung.android.health.sdk.sample.healthdiary.update.ui.UpdateViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.AuthViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
import com.samsung.android.health.sdk.sample.healthdiary.views.LoginScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "LoginActivity"
    }
    
    private lateinit var authViewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AuthViewModel
        authViewModel = ViewModelProvider(
            this, 
            HealthViewModelFactory(this)
        )[AuthViewModel::class.java]
        
        // Initialize OAuth
        authViewModel.initOAuth(this)
        
        // Handle OAuth callback if this is a redirect
        handleOAuthCallback(intent)
        
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
                        // Show login screen with OAuth support
                        LoginScreen(
                            viewModel = authViewModel,
                            onGoogleSignInClick = {
                                authViewModel.launchGoogleSignIn(this@LoginActivity)
                            }
                        )
                        
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
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthCallback(intent)
    }
    
    /**
     * Handle OAuth redirect callback from OAuthRedirectActivity.
     */
    private fun handleOAuthCallback(intent: Intent?) {
        intent ?: return
        
        val oauthResponse = intent.getStringExtra(OAuthRedirectActivity.EXTRA_OAUTH_RESPONSE)
        val oauthException = intent.getStringExtra(OAuthRedirectActivity.EXTRA_OAUTH_EXCEPTION)
        
        if (oauthResponse != null || oauthException != null) {
            Log.d(TAG, "Received OAuth callback: response=${oauthResponse != null}, exception=${oauthException != null}")
            authViewModel.handleOAuthCallback(oauthResponse, oauthException)
            
            // Clear the extras to prevent re-processing
            intent.removeExtra(OAuthRedirectActivity.EXTRA_OAUTH_RESPONSE)
            intent.removeExtra(OAuthRedirectActivity.EXTRA_OAUTH_EXCEPTION)
        }
    }
}

