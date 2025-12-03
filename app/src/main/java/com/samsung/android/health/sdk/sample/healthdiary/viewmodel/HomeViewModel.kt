package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfile(
    val name: String = "John Doe",
    val age: Int = 30,
    val gender: String = "Male",
    val height: Int = 175, // cm
    val weight: Int = 70   // kg
)

data class HomeUiState(
    val userProfile: UserProfile = UserProfile(),
    val lastSyncTime: String = "Never",
    val isSyncing: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // TODO: Load actual profile from Samsung Health or DataStore
        loadUserProfile()
        loadLastSyncTime()
    }

    private fun loadUserProfile() {
        // Mock data for now
        _uiState.value = _uiState.value.copy(
            userProfile = UserProfile(
                name = "Teo Echavarria",
                age = 28,
                gender = "Male",
                height = 180,
                weight = 75
            )
        )
    }

    private fun loadLastSyncTime() {
        // TODO: Get from DataStore or Room
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
