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
 * Connection information for linked family members.
 */
data class ConnectionInfo(
    val userId: String,
    val name: String,
    val relationship: String,
    val isActive: Boolean = true
)

/**
 * UI state for profile screen.
 */
data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(),
    val connections: List<ConnectionInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

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

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    init {
        loadProfile()
        loadConnections()
    }

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

    /**
     * Load user profile data.
     * TODO: Integrate with Samsung Health SDK or backend API
     */
    private fun loadProfile() {
        viewModelScope.launch {
            _profileUiState.value = _profileUiState.value.copy(isLoading = true)
            
            // Mock data for now - matches HomeViewModel
            val profile = UserProfile(
                name = "Lucía Méndez",
                email = "lucia.mendez@serenecare.com",
                age = 42,
                gender = "Female",
                height = 165,
                weight = 62,
                role = com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole.CAREGIVER,
                avatarUrl = null
            )
            
            _profileUiState.value = _profileUiState.value.copy(
                userProfile = profile,
                isLoading = false
            )
        }
    }

    /**
     * Load family connections.
     * TODO: Query Room database for active PairingEntity records
     */
    private fun loadConnections() {
        viewModelScope.launch {
            // Mock data for demonstration
            val connections = listOf(
                ConnectionInfo(
                    userId = "marta_id",
                    name = "Marta García",
                    relationship = "Madre • Monitoreo Activo",
                    isActive = true
                ),
                ConnectionInfo(
                    userId = "roberto_id",
                    name = "Roberto Méndez",
                    relationship = "Padre • Monitoreo de Signos",
                    isActive = true
                )
            )
            
            _profileUiState.value = _profileUiState.value.copy(
                connections = connections
            )
        }
    }
}
