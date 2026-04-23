package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientAlertsResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientDataResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PatientHealthSummaryResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.HeartRateHistoryResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SyncRequestCreate
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SyncRequestResponse
import com.samsung.android.health.sdk.sample.healthdiary.utils.AuthEventBus
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for accessing patient health data.
 * 
 * Used by caregivers to fetch health data (sensor readings, alerts) from
 * their linked patients. Also used by patients to fetch their own data.
 * 
 * Authorization:
 * - Own data: User can always fetch their own data
 * - Patient data: Requires active pairing between caregiver and patient
 */
class PatientDataRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "PatientDataRepository"
    }
    
    private val apiService = RetrofitClient.patientHealthApiService
    
    /**
     * Get sensor data for a patient.
     * 
     * Fetches accelerometer data collected from the patient's watch.
     * 
     * @param patientId ID of the patient to fetch data for
     * @param startTime Start timestamp in ms (inclusive), optional
     * @param endTime End timestamp in ms (inclusive), optional
     * @param limit Maximum records to return (default 100)
     * @return Result containing sensor data or error
     */
    suspend fun getPatientData(
        patientId: String,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int = 100
    ): Result<PatientDataResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getPatientData(
                authorization = "Bearer $token",
                patientId = patientId,
                startTime = startTime,
                endTime = endTime,
                limit = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                Log.i(TAG, "Fetched ${data.count} sensor records for patient $patientId")
                Result.success(data)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    403 -> "No tienes permiso para ver los datos de este paciente"
                    404 -> "Paciente no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error ${response.code()}"
                }
                Log.e(TAG, "Failed to get patient data: $errorMessage ($errorBody)")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching patient data", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al obtener datos"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
    
    /**
     * Get alerts for a patient.
     * 
     * Fetches health alerts generated for the patient.
     * 
     * @param patientId ID of the patient
     * @param cursor Pagination cursor, optional
     * @param limit Maximum alerts to return (default 50)
     * @param severity Filter by severity level, optional
     * @return Result containing alerts or error
     */
    suspend fun getPatientAlerts(
        patientId: String,
        cursor: String? = null,
        limit: Int = 50,
        severity: String? = null
    ): Result<PatientAlertsResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getPatientAlerts(
                authorization = "Bearer $token",
                patientId = patientId,
                cursor = cursor,
                limit = limit,
                severity = severity
            )
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                Log.i(TAG, "Fetched ${data.count} alerts for patient $patientId")
                Result.success(data)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    403 -> "No tienes permiso para ver las alertas de este paciente"
                    404 -> "Paciente no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error ${response.code()}"
                }
                Log.e(TAG, "Failed to get patient alerts: $errorMessage ($errorBody)")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching patient alerts", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al obtener alertas"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
    
    /**
     * Get recent data for a patient (convenience method).
     * 
     * Fetches data from the last N hours.
     * 
     * @param patientId ID of the patient
     * @param hoursAgo Hours to look back (default 24)
     * @param limit Maximum records to return (default 100)
     */
    suspend fun getRecentPatientData(
        patientId: String,
        hoursAgo: Int = 24,
        limit: Int = 100
    ): Result<PatientDataResponse> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (hoursAgo * 60 * 60 * 1000L)
        return getPatientData(patientId, startTime, endTime, limit)
    }
    
    /**
     * Get urgent alerts for a patient (convenience method).
     * 
     * Fetches only high and urgent severity alerts.
     * 
     * @param patientId ID of the patient
     * @param limit Maximum alerts to return (default 20)
     */
    suspend fun getUrgentPatientAlerts(
        patientId: String,
        limit: Int = 20
    ): Result<PatientAlertsResponse> {
        // Fetch high severity first
        val highResult = getPatientAlerts(patientId, limit = limit, severity = "high")
        val urgentResult = getPatientAlerts(patientId, limit = limit, severity = "urgent")
        
        // Combine results - if both succeed, merge; otherwise return the one that succeeded
        return when {
            highResult.isSuccess && urgentResult.isSuccess -> {
                val high = highResult.getOrThrow()
                val urgent = urgentResult.getOrThrow()
                val combined = (urgent.alerts + high.alerts)
                    .sortedByDescending { it.createdAt }
                    .take(limit)
                Result.success(
                    PatientAlertsResponse(
                        patientId = patientId,
                        alerts = combined,
                        count = combined.size,
                        nextCursor = null,
                        hasMore = high.hasMore || urgent.hasMore
                    )
                )
            }
            urgentResult.isSuccess -> urgentResult
            highResult.isSuccess -> highResult
            else -> highResult // Return the first failure
        }
    }
    
    /**
     * Get health summary for a patient.
     * 
     * Fetches summary of health metrics (last 24 hours):
     * - Heart rate (average, min, max, last reading)
     * - Steps (total count)
     * - Sleep (total minutes)
     * - Unavailable metrics (SpO2, Blood Pressure, Temperature)
     * 
     * @param patientId ID of the patient
     * @return Result containing health summary or error
     */
    suspend fun getPatientHealthSummary(
        patientId: String
    ): Result<PatientHealthSummaryResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getPatientHealthSummary(
                authorization = "Bearer $token",
                patientId = patientId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                Log.i(TAG, "Fetched health summary for patient $patientId: dataAvailable=${data.dataAvailable}")
                Result.success(data)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    403 -> "No tienes permiso para ver el resumen de este paciente"
                    404 -> "Paciente no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error ${response.code()}"
                }
                Log.e(TAG, "Failed to get patient health summary: $errorMessage ($errorBody)")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching patient health summary", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al obtener resumen de salud"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
    
    /**
     * Get heart rate history for a patient.
     * 
     * Fetches daily aggregated heart rate data.
     * 
     * @param patientId ID of the patient
     * @param days Number of days of history to fetch (1-30)
     * @return Result containing heart rate history or error
     */
    suspend fun getPatientHeartRateHistory(
        patientId: String,
        days: Int = 7
    ): Result<HeartRateHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getPatientHeartRateHistory(
                authorization = "Bearer $token",
                patientId = patientId,
                days = days
            )
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                Log.i(TAG, "Fetched heart rate history for patient $patientId: ${data.count} data points")
                Result.success(data)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    403 -> "No tienes permiso para ver el historial de este paciente"
                    404 -> "Paciente no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error ${response.code()}"
                }
                Log.e(TAG, "Failed to get heart rate history: $errorMessage ($errorBody)")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching heart rate history", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al obtener historial de frecuencia cardíaca"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
    
    /**
     * Create a sync request for a patient.
     * 
     * Requests immediate sync from the patient's device.
     * 
     * @param patientId ID of the patient to sync
     * @param priority Request priority (normal|urgent)
     * @return Result containing sync request response or error
     */
    suspend fun requestSync(
        patientId: String,
        priority: String = "normal"
    ): Result<SyncRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.createSyncRequest(
                authorization = "Bearer $token",
                patientId = patientId,
                request = SyncRequestCreate(priority = priority)
            )
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                Log.i(TAG, "Sync request created for patient $patientId: ${data.requestId}")
                Result.success(data)
            } else {
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    403 -> "No tienes permiso para solicitar sync de este paciente"
                    404 -> "Paciente no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error ${response.code()}"
                }
                Log.e(TAG, "Failed to create sync request: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sync request", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al solicitar sincronización"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
}
