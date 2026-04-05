package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import com.samsung.android.health.sdk.sample.healthdiary.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfile(
    val name: String = "Usuario",
    val email: String = "",
    val age: Int = 0,
    val gender: String = "",
    val height: Int = 0, // cm
    val weight: Int = 0,   // kg
    val role: UserRole = UserRole.PATIENT,
    val avatarUrl: String? = null
)

data class HomeUiState(
    val userProfile: UserProfile = UserProfile(),
    val lastSyncTime: String = "Never",
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val userRepository = UserRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadLastSyncTime()
    }

    fun refreshProfile() {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = userRepository.getFullProfile()
            
            result.fold(
                onSuccess = { fullProfile ->
                    Log.i(TAG, "Profile loaded: ${fullProfile.name}")
                    
                    val role = when (fullProfile.role) {
                        "caregiver" -> UserRole.CAREGIVER
                        "patient" -> UserRole.PATIENT
                        else -> UserRole.PATIENT
                    }
                    
                    val profile = UserProfile(
                        name = fullProfile.name ?: "Usuario",
                        email = fullProfile.email ?: "",
                        age = 0,
                        gender = "",
                        height = 0,
                        weight = 0,
                        role = role,
                        avatarUrl = fullProfile.profilePicture
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        userProfile = profile,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load profile: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    private fun loadLastSyncTime() {
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        _uiState.value = _uiState.value.copy(
            lastSyncTime = dateFormat.format(Date())
        )
    }

    fun syncData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            // Simulate sync delay
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastSyncTime = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
            )
        }
    }
}
