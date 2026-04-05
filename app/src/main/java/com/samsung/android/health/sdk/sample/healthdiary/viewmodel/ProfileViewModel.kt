package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.models.FullUserProfileResponse
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import com.samsung.android.health.sdk.sample.healthdiary.repository.AuthRepository
import com.samsung.android.health.sdk.sample.healthdiary.repository.LogoutResult
import com.samsung.android.health.sdk.sample.healthdiary.repository.UserRepository
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
    val profilePicture: String? = null,
    val isActive: Boolean = true
)

/**
 * UI state for profile screen.
 */
data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(),
    val connections: List<ConnectionInfo> = emptyList(),
    val role: String = "none",  // "caregiver", "patient", or "none"
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
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(application.applicationContext)

    private val _profileUiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * Refresh the profile data.
     */
    fun refreshProfile() {
        loadProfile()
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
     * Load user profile from API.
     */
    private fun loadProfile() {
        viewModelScope.launch {
            _profileUiState.value = _profileUiState.value.copy(isLoading = true, error = null)
            
            val result = userRepository.getFullProfile()
            
            result.fold(
                onSuccess = { fullProfile ->
                    Log.i(TAG, "Profile loaded: ${fullProfile.name}, role=${fullProfile.role}")
                    
                    // Convert API response to UI models
                    val userProfile = convertToUserProfile(fullProfile)
                    val connections = convertToConnections(fullProfile)
                    
                    _profileUiState.value = ProfileUiState(
                        userProfile = userProfile,
                        connections = connections,
                        role = fullProfile.role,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load profile: ${error.message}")
                    _profileUiState.value = _profileUiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    /**
     * Convert API response to UserProfile UI model.
     */
    private fun convertToUserProfile(fullProfile: FullUserProfileResponse): UserProfile {
        val role = when (fullProfile.role) {
            "caregiver" -> UserRole.CAREGIVER
            "patient" -> UserRole.PATIENT
            else -> UserRole.PATIENT
        }
        
        return UserProfile(
            name = fullProfile.name ?: "Usuario",
            email = fullProfile.email ?: "",
            age = 0,  // Not available from API yet
            gender = "",  // Not available from API yet
            height = 0,  // Not available from API yet
            weight = 0,  // Not available from API yet
            role = role,
            avatarUrl = fullProfile.profilePicture
        )
    }

    /**
     * Convert API connections to ConnectionInfo list.
     */
    private fun convertToConnections(fullProfile: FullUserProfileResponse): List<ConnectionInfo> {
        return fullProfile.connections.map { conn ->
            val relationship = when (conn.role) {
                "patient" -> "Persona a Cuidar"
                "caregiver" -> "Cuidador"
                else -> "Conexión"
            }
            
            ConnectionInfo(
                userId = conn.userId,
                name = conn.name,
                relationship = relationship,
                profilePicture = conn.profilePicture,
                isActive = true
            )
        }
    }
}
