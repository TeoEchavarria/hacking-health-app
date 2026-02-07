package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.activity.HealthMainActivity
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.AuthViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory

/**
 * Login Screen using Sandbox components
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(
        factory = HealthViewModelFactory(LocalContext.current)
    )
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    
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
                    val intent = android.content.Intent(context, HealthMainActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                                   android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    if (context is androidx.activity.ComponentActivity) {
                        context.finish()
                    }
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
