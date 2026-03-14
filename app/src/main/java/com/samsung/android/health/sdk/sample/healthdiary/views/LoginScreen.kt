package com.samsung.android.health.sdk.sample.healthdiary.views

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.activity.HealthMainActivity
import com.samsung.android.health.sdk.sample.healthdiary.wearable.ui.WatchOnboardingActivity
import com.samsung.android.health.sdk.sample.healthdiary.wearable.ui.isWatchOnboardingComplete
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.AuthViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory

/**
 * Login Screen using Sandbox components
 * Supports both username/password and OAuth (Google) authentication
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(
        factory = HealthViewModelFactory(LocalContext.current)
    ),
    onGoogleSignInClick: (() -> Unit)? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    
    // Initialize OAuth on first composition
    LaunchedEffect(Unit) {
        viewModel.initOAuth(context)
    }
    
    // Observe auth state
    LaunchedEffect(Unit) {
        viewModel.authState.collect { state ->
            when (state) {
                is AuthViewModel.AuthState.Idle -> {
                    isLoading = false
                }
                is AuthViewModel.AuthState.Loading -> {
                    isLoading = true
                    statusMessage = context.getString(R.string.auth_loading)
                    errorMessage = null
                }
                is AuthViewModel.AuthState.Success -> {
                    isLoading = false
                    statusMessage = null
                    errorMessage = null
                    // Navigate to main activity
                    navigateToMainActivity(context)
                }
                is AuthViewModel.AuthState.OAuthSuccess -> {
                    isLoading = false
                    statusMessage = null
                    errorMessage = null
                    // Navigate to main activity
                    navigateToMainActivity(context)
                }
                is AuthViewModel.AuthState.Error -> {
                    isLoading = false
                    errorMessage = state.message
                    statusMessage = null
                }
            }
        }
    }
    
    // Observe error messages
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { errorMsg ->
            errorMsg?.let {
                isLoading = false
                errorMessage = it
                statusMessage = null
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        SandboxHeader(
            title = context.getString(R.string.login_title),
            variant = HeaderVariant.Large,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        
        // Subtitle
        Text(
            text = context.getString(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 18.dp)
        )
        
        // ==========================================================================
        // Google Sign-In Button (OAuth)
        // ==========================================================================
        if (viewModel.isGoogleSignInAvailable()) {
            GoogleSignInButton(
                onClick = {
                    if (onGoogleSignInClick != null) {
                        onGoogleSignInClick()
                    } else {
                        // Try to get activity from context
                        (context as? Activity)?.let { activity ->
                            viewModel.launchGoogleSignIn(activity)
                        }
                    }
                },
                isLoading = isLoading,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // Divider with "or"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        
        // ==========================================================================
        // Username/Password Login (Legacy)
        // ==========================================================================
        
        // Username Input
        SandboxInput(
            value = username,
            onValueChange = { 
                username = it
                errorMessage = null
            },
            label = context.getString(R.string.login_username_label),
            placeholder = context.getString(R.string.login_username_hint),
            keyboardType = KeyboardType.Email,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            isError = errorMessage != null && username.isEmpty(),
            errorMessage = if (username.isEmpty() && errorMessage != null) {
                context.getString(R.string.login_username_required)
            } else null
        )
        
        // Password Input
        SandboxPasswordField(
            value = password,
            onValueChange = { 
                password = it
                errorMessage = null
            },
            label = context.getString(R.string.login_password_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        if (errorMessage != null && password.isEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = context.getString(R.string.login_password_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        // Error Message
        if (errorMessage != null && username.isNotEmpty() && password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SandboxBadge(
                text = errorMessage ?: "",
                variant = BadgeVariant.Error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Login Button
        SandboxButton(
            text = context.getString(R.string.login_button),
            onClick = {
                when {
                    username.isEmpty() -> {
                        errorMessage = context.getString(R.string.login_username_required)
                    }
                    password.isEmpty() -> {
                        errorMessage = context.getString(R.string.login_password_required)
                    }
                    else -> {
                        errorMessage = null
                        statusMessage = null
                        val request = LoginRequest(
                            username = username.trim(),
                            password = password.trim(),
                            fcmToken = ""
                        )
                        viewModel.login(request)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = isLoading,
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Google Sign-In button following Google's branding guidelines.
 */
@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDADCE0))
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            // Google "G" icon (using material icon as placeholder)
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF4285F4) // Google blue
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = "Continue with Google",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Navigate to the main activity (optionally through onboarding) and clear the back stack.
 */
private fun navigateToMainActivity(context: android.content.Context) {
    // Check if watch onboarding has been completed
    val shouldShowOnboarding = !isWatchOnboardingComplete(context)
    
    val targetActivity = if (shouldShowOnboarding) {
        WatchOnboardingActivity::class.java
    } else {
        HealthMainActivity::class.java
    }
    
    val intent = android.content.Intent(context, targetActivity)
    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                   android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
    if (context is androidx.activity.ComponentActivity) {
        context.finish()
    }
}
