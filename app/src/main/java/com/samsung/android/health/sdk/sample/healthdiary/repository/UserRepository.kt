package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.FullUserProfileResponse
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairingEntity
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for user profile operations.
 */
class UserRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "UserRepository"
    }
    
    private val apiService = RetrofitClient.userApiService
    private val pairingDao = AppDatabase.getDatabase(context).pairingDao()
    
    /**
     * Get the currently active pairing (if any).
     * Returns the first active pairing found (priority: caregiver, then patient).
     */
    suspend fun getActivePairing(): PairingEntity? = withContext(Dispatchers.IO) {
        try {
            val activePairings = pairingDao.getAllActiveSync()
            if (activePairings.isEmpty()) {
                Log.d(TAG, "No active pairings found")
                return@withContext null
            }
            
            // Priority: caregiver role first (since they need to see patient data)
            val caregiverPairing = activePairings.find { it.userRole == "caregiver" }
            if (caregiverPairing != null) {
                Log.d(TAG, "Found active caregiver pairing: ${caregiverPairing.pairingId}")
                return@withContext caregiverPairing
            }
            
            // Fall back to patient pairing
            val patientPairing = activePairings.find { it.userRole == "patient" }
            if (patientPairing != null) {
                Log.d(TAG, "Found active patient pairing: ${patientPairing.pairingId}")
                return@withContext patientPairing
            }
            
            // Return first pairing as fallback
            Log.d(TAG, "Returning first active pairing: ${activePairings[0].pairingId}")
            return@withContext activePairings[0]
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active pairing", e)
            return@withContext null
        }
    }
    
    /**
     * Get full user profile with role and connections.
     * 
     * @return Result containing full profile or error
     */
    suspend fun getFullProfile(): Result<FullUserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getFullProfile(
                authorization = "Bearer $token"
            )
            
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                Log.i(TAG, "Fetched profile for user ${profile.id}: role=${profile.role}, connections=${profile.connections.size}")
                Result.success(profile)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> "No autorizado: Inicia sesión nuevamente"
                    404 -> "Usuario no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error ${response.code()}"
                }
                Log.e(TAG, "Failed to get profile: $errorMessage ($errorBody)")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> "Error: ${e.message}"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
}
