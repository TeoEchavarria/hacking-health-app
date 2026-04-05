package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.FullUserProfileResponse
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
