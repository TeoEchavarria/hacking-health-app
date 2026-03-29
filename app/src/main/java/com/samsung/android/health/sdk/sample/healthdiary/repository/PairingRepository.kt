package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CreatePairingCodeRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CreatePairingCodeResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PairingStatusResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ValidatePairingCodeRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ValidatePairingCodeResponse
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for family pairing/linking operations.
 * 
 * Handles the flow of linking caregivers with patients via 6-digit codes:
 * - Patients generate codes
 * - Caregivers validate codes
 * - Both parties monitor pairing status
 */
class PairingRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "PairingRepository"
    }
    
    private val apiService = RetrofitClient.pairingApiService
    
    /**
     * Create a new pairing code for a patient.
     * Patient calls this to generate a code to share with their caregiver.
     * 
     * @return Result containing the pairing code and metadata
     */
    suspend fun createPairingCode(): Result<CreatePairingCodeResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available for pairing code creation")
                return@withContext Result.failure(Exception("Not authenticated"))
            }
            
            val response = apiService.createPairingCode(
                authorization = "Bearer $token",
                request = CreatePairingCodeRequest()
            )
            
            if (response.isSuccessful && response.body() != null) {
                val pairingCode = response.body()!!
                Log.i(TAG, "Pairing code created: ${pairingCode.pairingId}")
                Result.success(pairingCode)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> "No autorizado: Inicia sesión nuevamente"
                    403 -> "No tienes permisos para generar un código"
                    429 -> "Demasiadas solicitudes, intenta más tarde"
                    500 -> "Error del servidor: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Log.e(TAG, "Failed to create pairing code: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating pairing code", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado, intenta de nuevo"
                else -> e.message ?: "Error al generar código"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
    
    /**
     * Validate a pairing code entered by a caregiver.
     * Links the caregiver's account with the patient who generated the code.
     * 
     * @param code 6-digit numeric code
     * @return Result containing patient info if successful
     */
    suspend fun validatePairingCode(code: String): Result<ValidatePairingCodeResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available for pairing code validation")
                return@withContext Result.failure(Exception("Not authenticated"))
            }
            
            // Validate code format
            if (code.length != 6 || !code.all { it.isDigit() }) {
                return@withContext Result.failure(Exception("El código debe tener 6 dígitos"))
            }
            
            val response = apiService.validatePairingCode(
                authorization = "Bearer $token",
                request = ValidatePairingCodeRequest(code = code)
            )
            
            if (response.isSuccessful && response.body() != null) {
                val validationResult = response.body()!!
                
                if (validationResult.success) {
                    Log.i(TAG, "Pairing code validated successfully: ${validationResult.pairingId}")
                    Result.success(validationResult)
                } else {
                    val errorMessage = validationResult.error ?: "Código inválido o expirado"
                    Log.w(TAG, "Pairing validation failed: $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> "No autorizado: Inicia sesión nuevamente"
                    404 -> "Código inválido o expirado"
                    410 -> "Este código ya fue utilizado"
                    429 -> "Demasiados intentos, espera un momento"
                    500 -> "Error del servidor: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Log.e(TAG, "Failed to validate pairing code: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating pairing code", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado, intenta de nuevo"
                else -> e.message ?: "Error al validar código"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
    
    /**
     * Check the status of a pairing code.
     * Patient calls this periodically to see if a caregiver has used their code.
     * 
     * @param pairingId The ID returned when code was created
     * @return Result containing current pairing status
     */
    suspend fun checkPairingStatus(pairingId: String): Result<PairingStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available for status check")
                return@withContext Result.failure(Exception("Not authenticated"))
            }
            
            val response = apiService.checkPairingStatus(
                authorization = "Bearer $token",
                pairingId = pairingId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val status = response.body()!!
                Log.d(TAG, "Pairing status: ${status.status}, linked: ${status.linked}")
                Result.success(status)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> "No autorizado: Inicia sesión nuevamente"
                    404 -> "Código no encontrado o expirado"
                    500 -> "Error del servidor: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Log.e(TAG, "Failed to check pairing status: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking pairing status", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al verificar estado"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
}
