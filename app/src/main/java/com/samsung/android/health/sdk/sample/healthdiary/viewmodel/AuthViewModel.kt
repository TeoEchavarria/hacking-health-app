package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.UserProfileData
import com.samsung.android.health.sdk.sample.healthdiary.repository.AuthRepository
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _authState.emit(AuthState.Error(exception.message ?: "Unknown error"))
            _errorMessage.emit(exception.message ?: "Unknown error occurred")
        }
    }
    
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
                        else -> exception.message ?: "Login failed"
                    }
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
        data class Error(val message: String) : AuthState()
    }
}

