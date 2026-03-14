package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.UserProfileData
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthConfig
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthProvider
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthRepository
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthTokenResponse
import com.samsung.android.health.sdk.sample.healthdiary.repository.AuthRepository
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import com.samsung.android.health.sdk.sample.healthdiary.worker.UploadScheduler
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private var oauthRepository: OAuthRepository? = null
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _authState.emit(AuthState.Error(exception.message ?: "Unknown error"))
            _errorMessage.emit(exception.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Initialize OAuth repository with context.
     * Must be called before using OAuth features.
     */
    fun initOAuth(context: Context) {
        if (oauthRepository == null) {
            oauthRepository = OAuthRepository(context.applicationContext)
        }
    }
    
    fun logout(context: Context) {
        viewModelScope.launch(AppConstants.SCOPE_IO_DISPATCHERS + exceptionHandler) {
            // Cancel background sync
            UploadScheduler.cancelAllWork(context)
            
            // Logout from API and clear local tokens
            authRepository.logout(context)
            
            // Reset state
            resetState()
        }
    }
    
    // ==========================================================================
    // Legacy Username/Password Authentication
    // ==========================================================================
    
    fun loginWithProfile(profileData: UserProfileData, password: String? = null) {
        val username = profileData.email?.takeIf { it.isNotBlank() }
            ?: profileData.name?.takeIf { it.isNotBlank() }
        if (username == null) {
            val errorMsg = "Username is required for login"
            _authState.value = AuthState.Error(errorMsg)
            _errorMessage.value = errorMsg
            return
        }
        
        val finalPassword = password?.takeIf { it.isNotBlank() }
            ?: AppConstants.AUTO_AUTH_FALLBACK_PASSWORD
        
        val request = LoginRequest(
            username = username,
            password = finalPassword,
            fcmToken = ""
        )
        
        login(request)
    }
    
    fun login(request: LoginRequest) {
        viewModelScope.launch(AppConstants.SCOPE_IO_DISPATCHERS + exceptionHandler) {
            _authState.emit(AuthState.Loading)
            _errorMessage.emit(null)
            
            val result = authRepository.login(request)
            
            result.fold(
                onSuccess = { response ->
                    TokenManager.saveToken(response.token)
                    _authState.emit(AuthState.Success(loginResponse = response))
                    _errorMessage.emit(null)
                },
                onFailure = { exception ->
                    val errorMsg = when {
                        exception.message?.contains("401") == true -> "Invalid credentials"
                        exception.message?.contains("404") == true -> "User not found"
                        exception.message?.contains("400") == true -> "Invalid login data"
                        exception.message?.contains("500") == true -> "Server error. Please try again later"
                        exception.message?.contains("Network") == true -> "Network error: Check your connection"
                        exception.message?.contains("social login") == true -> exception.message!!
                        else -> exception.message ?: "Login failed"
                    }
                    _authState.emit(AuthState.Error(errorMsg))
                    _errorMessage.emit(errorMsg)
                }
            )
        }
    }
    
    // ==========================================================================
    // OAuth Authentication (Google Sign-In SDK)
    // ==========================================================================
    
    /**
     * Check if Google OAuth is available.
     */
    fun isGoogleSignInAvailable(): Boolean {
        return OAuthConfig.isProviderConfigured(OAuthProvider.GOOGLE)
    }
    
    /**
     * Launch Google Sign-In flow using native Google Sign-In SDK.
     * This opens the native Google account picker.
     */
    fun launchGoogleSignIn(activity: Activity) {
        val repo = oauthRepository ?: run {
            _errorMessage.value = "OAuth not initialized. Call initOAuth first."
            return
        }
        
        if (!isGoogleSignInAvailable()) {
            _errorMessage.value = "Google Sign-In is not configured"
            return
        }
        
        _authState.value = AuthState.Loading
        _errorMessage.value = null
        
        try {
            repo.launchGoogleSignIn(activity)
            Log.d(TAG, "Launched Google Sign-In flow")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Google Sign-In", e)
            _authState.value = AuthState.Error("Failed to start Google Sign-In: ${e.message}")
            _errorMessage.value = e.message
        }
    }
    
    /**
     * Handle Google Sign-In result from onActivityResult.
     * 
     * @param data Intent data from onActivityResult
     */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch(AppConstants.SCOPE_IO_DISPATCHERS + exceptionHandler) {
            _authState.emit(AuthState.Loading)
            
            val repo = oauthRepository ?: run {
                Log.e(TAG, "GoogleAuth: OAuth repository not initialized")
                _authState.emit(AuthState.Error("OAuth not initialized"))
                return@launch
            }
            
            Log.d(TAG, "GoogleAuth: Handling Google Sign-In result")
            
            val result = repo.handleGoogleSignInResult(data)
            
            result.fold(
                onSuccess = { oauthResponse ->
                    Log.d(TAG, "GoogleAuth: Backend authentication successful")
                    Log.d(TAG, "GoogleAuth: Saving access token to TokenManager")
                    
                    // Save the JWT token from backend response
                    TokenManager.saveToken(oauthResponse.accessToken)
                    
                    Log.d(TAG, "GoogleAuth: Token saved, hasToken=${TokenManager.hasToken()}")
                    
                    _authState.emit(AuthState.OAuthSuccess(oauthResponse = oauthResponse))
                    _errorMessage.emit(null)
                },
                onFailure = { error ->
                    Log.e(TAG, "GoogleAuth: Authentication failed - ${error.message}", error)
                    val errorMsg = error.message ?: "Google Sign-In failed"
                    _authState.emit(AuthState.Error(errorMsg))
                    _errorMessage.emit(errorMsg)
                }
            )
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
        _errorMessage.value = null
    }
    
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val loginResponse: LoginResponse) : AuthState()
        data class OAuthSuccess(val oauthResponse: OAuthTokenResponse) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}