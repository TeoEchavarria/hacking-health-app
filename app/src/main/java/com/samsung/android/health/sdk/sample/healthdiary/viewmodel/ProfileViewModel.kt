package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.repository.AuthRepository
import com.samsung.android.health.sdk.sample.healthdiary.repository.LogoutResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * States for the logout process.
 */
sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    object Success : LogoutState()
    data class Error(val message: String) : LogoutState()
}

/**
 * ViewModel for user profile and logout functionality.
 */
class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    /**
     * Initiate logout process.
     * 
     * @param context Application context for OAuth sign-out
     */
    fun logout(context: Context) {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading

            when (val result = authRepository.logout(context)) {
                is LogoutResult.Success -> {
                    _logoutState.value = LogoutState.Success
                }
                is LogoutResult.Error -> {
                    _logoutState.value = LogoutState.Error(result.message)
                }
            }
        }
    }

    /**
     * Reset logout state to idle.
     */
    fun resetState() {
        _logoutState.value = LogoutState.Idle
    }
}
