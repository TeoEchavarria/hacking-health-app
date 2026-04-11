package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for Add Medication form.
 */
data class AddMedicationUiState(
    val name: String = "",
    val dosage: String = "",
    val time: String = "08:00",
    val instructions: String = "",
    val medicationType: String = "pill", // "pill" or "injection"
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val nameError: String? = null,
    val timeError: String? = null
)

/**
 * ViewModel for Add Medication screen.
 * 
 * Handles form validation and medication creation.
 */
class AddMedicationViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AddMedicationViewModel"
    }

    private val medicationRepository = MedicationRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AddMedicationUiState())
    val uiState: StateFlow<AddMedicationUiState> = _uiState.asStateFlow()

    // Optional: patient ID when caregiver is creating medication for patient
    private var targetPatientId: String? = null

    /**
     * Set if creating medication for a patient (caregiver mode).
     */
    fun setPatientId(patientId: String?) {
        targetPatientId = patientId
    }

    // Form field updates
    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(
            name = value,
            nameError = null
        )
    }

    fun updateDosage(value: String) {
        _uiState.value = _uiState.value.copy(dosage = value)
    }

    fun updateTime(value: String) {
        _uiState.value = _uiState.value.copy(
            time = value,
            timeError = null
        )
    }

    fun updateInstructions(value: String) {
        _uiState.value = _uiState.value.copy(instructions = value)
    }

    fun updateMedicationType(value: String) {
        _uiState.value = _uiState.value.copy(medicationType = value)
    }

    /**
     * Validate form fields.
     * @return true if valid, false otherwise
     */
    private fun validateForm(): Boolean {
        var isValid = true
        val current = _uiState.value

        // Validate name
        if (current.name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "El nombre es obligatorio")
            isValid = false
        }

        // Validate time format (HH:MM)
        val timePattern = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)\$")
        if (!timePattern.matches(current.time)) {
            _uiState.value = _uiState.value.copy(timeError = "Formato inválido. Usa HH:MM")
            isValid = false
        }

        return isValid
    }

    /**
     * Submit the form to create a new medication.
     */
    fun createMedication() {
        if (!validateForm()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val current = _uiState.value
            
            val result = medicationRepository.createMedication(
                name = current.name.trim(),
                dosage = current.dosage.trim(),
                time = current.time,
                instructions = current.instructions.trim(),
                medicationType = current.medicationType,
                patientId = targetPatientId
            )

            result.fold(
                onSuccess = { medication ->
                    Log.i(TAG, "Medication created: ${medication.name}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Error creating medication: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al crear el medicamento"
                    )
                }
            )
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset form to initial state.
     */
    fun resetForm() {
        _uiState.value = AddMedicationUiState()
        targetPatientId = null
    }
}
