package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.*
import com.samsung.android.health.sdk.sample.healthdiary.utils.AuthEventBus
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing medications and medication takes.
 * 
 * Provides CRUD operations for medications and tracks daily medication adherence.
 * All operations are scoped to the authenticated user.
 */
class MedicationRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "MedicationRepository"
    }
    
    private val apiService = RetrofitClient.medicationApiService
    
    /**
     * Get all medications for the current user.
     * 
     * @param includeInactive Include inactive medications
     * @param patientId Optional patient ID for caregivers viewing patient's medications
     * @return Result containing list of medications or error
     */
    suspend fun getMedications(
        includeInactive: Boolean = false,
        patientId: String? = null
    ): Result<List<Medication>> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getMedications(
                authorization = "Bearer $token",
                includeInactive = includeInactive,
                patientId = patientId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val medications = response.body()!!
                Log.i(TAG, "Fetched ${medications.size} medications")
                Result.success(medications)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "obtener medicamentos")
        }
    }
    
    /**
     * Create a new medication reminder.
     * 
     * @param name Name of the medication
     * @param dosage Dosage information
     * @param time Time of day (HH:MM)
     * @param instructions Additional instructions
     * @param medicationType Type of medication ("pill" or "injection")
     * @param patientId Optional patient ID when caregiver is creating for patient
     * @return Result containing created medication or error
     */
    suspend fun createMedication(
        name: String,
        dosage: String = "",
        time: String,
        instructions: String = "",
        medicationType: String = "pill",
        patientId: String? = null
    ): Result<Medication> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val request = MedicationCreateRequest(
                name = name,
                dosage = dosage,
                time = time,
                instructions = instructions,
                medicationType = medicationType
            )
            
            // Choose endpoint based on whether it's for a patient
            val response = if (patientId != null) {
                apiService.createMedicationForPatient(
                    authorization = "Bearer $token",
                    patientId = patientId,
                    request = request
                )
            } else {
                apiService.createMedication(
                    authorization = "Bearer $token",
                    request = request
                )
            }
            
            if (response.isSuccessful && response.body() != null) {
                val medication = response.body()!!
                Log.i(TAG, "Created medication: ${medication.name}")
                Result.success(medication)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "crear medicamento")
        }
    }
    
    /**
     * Update an existing medication.
     * 
     * @param medicationId ID of the medication to update
     * @param name New name (optional)
     * @param dosage New dosage (optional)
     * @param time New time (optional)
     * @param instructions New instructions (optional)
     * @param medicationType New type (optional)
     * @param isActive Whether medication is active (optional)
     * @return Result containing updated medication or error
     */
    suspend fun updateMedication(
        medicationId: String,
        name: String? = null,
        dosage: String? = null,
        time: String? = null,
        instructions: String? = null,
        medicationType: String? = null,
        isActive: Boolean? = null
    ): Result<Medication> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val request = MedicationUpdateRequest(
                name = name,
                dosage = dosage,
                time = time,
                instructions = instructions,
                medicationType = medicationType,
                isActive = isActive
            )
            
            val response = apiService.updateMedication(
                authorization = "Bearer $token",
                medicationId = medicationId,
                request = request
            )
            
            if (response.isSuccessful && response.body() != null) {
                val medication = response.body()!!
                Log.i(TAG, "Updated medication: ${medication.name}")
                Result.success(medication)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "actualizar medicamento")
        }
    }
    
    /**
     * Delete a medication (soft delete).
     * 
     * @param medicationId ID of the medication to delete
     * @return Result indicating success or error
     */
    suspend fun deleteMedication(
        medicationId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.deleteMedication(
                authorization = "Bearer $token",
                medicationId = medicationId
            )
            
            if (response.isSuccessful) {
                Log.i(TAG, "Deleted medication: $medicationId")
                Result.success(Unit)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "eliminar medicamento")
        }
    }
    
    /**
     * Record that a medication was taken.
     * 
     * @param medicationId ID of the medication
     * @param notes Optional notes
     * @param takenAt Optional specific datetime (ISO format)
     * @return Result containing take record or error
     */
    suspend fun takeMedication(
        medicationId: String,
        notes: String? = null,
        takenAt: String? = null
    ): Result<MedicationTake> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val request = TakeMedicationRequest(
                medicationId = medicationId,
                notes = notes,
                takenAt = takenAt
            )
            
            val response = apiService.takeMedication(
                authorization = "Bearer $token",
                request = request
            )
            
            if (response.isSuccessful && response.body() != null) {
                val take = response.body()!!
                Log.i(TAG, "Recorded take for medication: $medicationId")
                Result.success(take)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "registrar toma de medicamento")
        }
    }
    
    /**
     * Remove a medication take record.
     * 
     * @param medicationId ID of the medication
     * @param date Date of the take to remove (YYYY-MM-DD)
     * @return Result indicating success or error
     */
    suspend fun untakeMedication(
        medicationId: String,
        date: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.untakeMedication(
                authorization = "Bearer $token",
                medicationId = medicationId,
                date = date
            )
            
            if (response.isSuccessful) {
                Log.i(TAG, "Removed take for medication: $medicationId on $date")
                Result.success(Unit)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "desmarcar toma de medicamento")
        }
    }
    
    /**
     * Get medications with their take status for a specific date.
     * 
     * @param date Date to check (YYYY-MM-DD). Defaults to today.
     * @param patientId Optional patient ID for caregivers viewing patient's status
     * @return Result containing list of medications with status or error
     */
    suspend fun getMedicationsWithTodayStatus(
        date: String? = null,
        patientId: String? = null
    ): Result<List<MedicationWithTakes>> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getTodayStatus(
                authorization = "Bearer $token",
                date = date,
                patientId = patientId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val medications = response.body()!!
                Log.i(TAG, "Fetched ${medications.size} medications with status for ${date ?: "today"}")
                Result.success(medications)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "obtener estado de medicamentos")
        }
    }
    
    /**
     * Get monthly adherence report.
     * 
     * @param year Year of the report
     * @param month Month of the report (1-12)
     * @param patientId Optional patient ID for caregivers viewing patient's report
     * @return Result containing monthly report or error
     */
    suspend fun getMonthlyReport(
        year: Int,
        month: Int,
        patientId: String? = null
    ): Result<MonthlyReportResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getMonthlyReport(
                authorization = "Bearer $token",
                year = year,
                month = month,
                patientId = patientId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val report = response.body()!!
                Log.i(TAG, "Fetched monthly report for $year-$month: ${report.overallAdherence}% adherence")
                Result.success(report)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "obtener reporte mensual")
        }
    }
    
    /**
     * Get calendar events for a month.
     * 
     * @param year Year
     * @param month Month (1-12)
     * @param patientId Optional patient ID for caregivers viewing patient's calendar
     * @return Result containing calendar events or error
     */
    suspend fun getCalendarEvents(
        year: Int,
        month: Int,
        patientId: String? = null
    ): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getCalendarEvents(
                authorization = "Bearer $token",
                year = year,
                month = month,
                patientId = patientId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val events = response.body()!!
                Log.i(TAG, "Fetched ${events.size} calendar events for $year-$month")
                Result.success(events)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "obtener eventos del calendario")
        }
    }
    
    // ===================
    // Helper methods
    // ===================
    
    private fun <T> handleError(code: Int, errorBody: String?): Result<T> {
        val errorMessage = when (code) {
            401 -> {
                // Emit session expired event for centralized handling
                return AuthEventBus.handleUnauthorized()
            }
            403 -> "No tienes permiso para realizar esta acción"
            404 -> "No encontrado"
            500 -> "Error del servidor"
            else -> "Error $code"
        }
        Log.e(TAG, "API error: $errorMessage ($errorBody)")
        return Result.failure(Exception(errorMessage))
    }
    
    private fun <T> handleException(e: Exception, action: String): Result<T> {
        Log.e(TAG, "Error al $action", e)
        val friendlyMessage = when (e) {
            is java.net.UnknownHostException -> "Sin conexión a internet"
            is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
            else -> e.message ?: "Error al $action"
        }
        return Result.failure(Exception(friendlyMessage))
    }
}
