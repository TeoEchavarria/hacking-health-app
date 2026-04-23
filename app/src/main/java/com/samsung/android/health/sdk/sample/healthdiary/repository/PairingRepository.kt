package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CreatePairingCodeRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CreatePairingCodeResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ListUserPairingsResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PairingStatusResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ValidatePairingCodeRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ValidatePairingCodeResponse
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairingEntity
import com.samsung.android.health.sdk.sample.healthdiary.utils.AuthEventBus
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
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
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
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
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
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
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
    
    /**
     * Sync pairings from the backend to the local Room database.
     * Fetches all active pairings where the user is either patient or caregiver.
     * 
     * This should be called on app startup to ensure local DB is in sync
     * with the backend, especially after app reinstall or data clear.
     * 
     * @return Result containing the number of pairings synced
     */
    suspend fun syncPairingsFromBackend(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.w(TAG, "No auth token available for pairing sync")
                return@withContext Result.failure(Exception("Not authenticated"))
            }
            
            val pairingDao = AppDatabase.getDatabase(context).pairingDao()
            var totalSynced = 0
            
            // Fetch pairings where user is a patient
            val patientResponse = apiService.listUserPairings(
                authorization = "Bearer $token",
                role = "patient"
            )
            
            if (patientResponse.isSuccessful && patientResponse.body() != null) {
                val patientPairings = patientResponse.body()!!.pairings
                Log.d(TAG, "Fetched ${patientPairings.size} pairings as patient")
                
                for (pairing in patientPairings) {
                    val entity = PairingEntity(
                        pairingId = pairing.id,
                        patientId = pairing.patientId,
                        patientName = pairing.patientName,
                        caregiverId = pairing.caregiverId,
                        caregiverName = pairing.caregiverName,
                        status = pairing.status,
                        userRole = "patient",
                        createdAt = pairing.createdAt ?: System.currentTimeMillis(),
                        activatedAt = pairing.activatedAt
                    )
                    pairingDao.insert(entity)
                    totalSynced++
                }
            }
            
            // Fetch pairings where user is a caregiver
            val caregiverResponse = apiService.listUserPairings(
                authorization = "Bearer $token",
                role = "caregiver"
            )
            
            if (caregiverResponse.isSuccessful && caregiverResponse.body() != null) {
                val caregiverPairings = caregiverResponse.body()!!.pairings
                Log.d(TAG, "Fetched ${caregiverPairings.size} pairings as caregiver")
                
                for (pairing in caregiverPairings) {
                    val entity = PairingEntity(
                        pairingId = pairing.id,
                        patientId = pairing.patientId,
                        patientName = pairing.patientName,
                        caregiverId = pairing.caregiverId,
                        caregiverName = pairing.caregiverName,
                        status = pairing.status,
                        userRole = "caregiver",
                        createdAt = pairing.createdAt ?: System.currentTimeMillis(),
                        activatedAt = pairing.activatedAt
                    )
                    pairingDao.insert(entity)
                    totalSynced++
                }
            }
            
            Log.i(TAG, "Successfully synced $totalSynced pairings from backend")
            Result.success(totalSynced)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pairings from backend", e)
            Result.failure(e)
        }
    }
    
    /**
     * Revoke an active pairing.
     * This marks the pairing as "revoked" on the backend.
     * 
     * @param pairingId ID of the pairing to revoke
     * @return Result containing success status
     */
    suspend fun revokePairing(pairingId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available for revoking pairing")
                return@withContext Result.failure(Exception("Not authenticated"))
            }
            
            val response = apiService.revokePairing(
                authorization = "Bearer $token",
                pairingId = pairingId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                if (result.success) {
                    Log.i(TAG, "Pairing $pairingId revoked successfully")
                    
                    // Also update local database
                    val pairingDao = AppDatabase.getDatabase(context).pairingDao()
                    pairingDao.updateStatus(pairingId, "revoked")
                    
                    Result.success(true)
                } else {
                    Log.w(TAG, "Failed to revoke pairing: ${result.error}")
                    Result.failure(Exception(result.error ?: "Error al revocar vinculación"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    403 -> "No tienes permiso para revocar esta vinculación"
                    404 -> "Vinculación no encontrada"
                    500 -> "Error del servidor: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Log.e(TAG, "Failed to revoke pairing: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking pairing", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al revocar vinculación"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
}
