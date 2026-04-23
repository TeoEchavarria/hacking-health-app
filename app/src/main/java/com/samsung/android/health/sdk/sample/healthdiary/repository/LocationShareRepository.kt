package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LocationUpdateRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LocationUpdateResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PairedLocationResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SharingToggleRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SharingStatusResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LocationData
import com.samsung.android.health.sdk.sample.healthdiary.utils.AuthEventBus
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for location sharing operations between paired users.
 *
 * Handles:
 * - Updating current user's location
 * - Fetching paired user's location
 * - Managing sharing preferences
 */
class LocationShareRepository(private val context: Context) {

    companion object {
        private const val TAG = "LocationShareRepository"
    }

    private val apiService = RetrofitClient.locationApiService

    /**
     * Update the current user's location.
     *
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @param accuracy GPS accuracy in meters
     * @return Result containing update confirmation
     */
    suspend fun updateLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float? = null
    ): Result<LocationUpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available for location update")
                return@withContext Result.failure(Exception("Not authenticated"))
            }

            val request = LocationUpdateRequest(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                timestamp = System.currentTimeMillis()
            )

            val response = apiService.updateLocation(
                authorization = "Bearer $token",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "Location updated successfully at ${result.updatedAt}")
                Result.success(result)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    403 -> "No tienes permisos para actualizar ubicación"
                    429 -> "Demasiadas solicitudes, intenta más tarde"
                    500 -> "Error del servidor: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Log.e(TAG, "Failed to update location: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating location", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al actualizar ubicación"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    /**
     * Get the paired user's current location.
     *
     * @return Result containing the paired user's location or explanation message
     */
    suspend fun getPairedLocation(): Result<PairedLocationResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available for fetching paired location")
                return@withContext Result.failure(Exception("Not authenticated"))
            }

            val response = apiService.getPairedLocation(
                authorization = "Bearer $token"
            )

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                if (result.found) {
                    Log.d(TAG, "Paired location found: ${result.location?.userName}")
                } else {
                    Log.d(TAG, "Paired location not available: ${result.message}")
                }
                Result.success(result)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    404 -> "No hay usuario vinculado"
                    500 -> "Error del servidor: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Log.e(TAG, "Failed to get paired location: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paired location", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al obtener ubicación"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    /**
     * Toggle location sharing preference.
     *
     * @param enabled Whether to share location with paired user
     * @return Result containing updated sharing status
     */
    suspend fun toggleSharing(enabled: Boolean): Result<SharingStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available for toggling sharing")
                return@withContext Result.failure(Exception("Not authenticated"))
            }

            val request = SharingToggleRequest(sharingEnabled = enabled)

            val response = apiService.toggleSharing(
                authorization = "Bearer $token",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "Sharing toggled to: ${result.sharingEnabled}")
                Result.success(result)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    404 -> "Usuario no encontrado"
                    500 -> "Error del servidor: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Log.e(TAG, "Failed to toggle sharing: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling sharing", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al cambiar preferencia"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }

    /**
     * Get current sharing status.
     *
     * @return Result containing current sharing status
     */
    suspend fun getSharingStatus(): Result<SharingStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available for getting sharing status")
                return@withContext Result.failure(Exception("Not authenticated"))
            }

            val response = apiService.getSharingStatus(
                authorization = "Bearer $token"
            )

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "Current sharing status: ${result.sharingEnabled}")
                Result.success(result)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> return@withContext AuthEventBus.handleUnauthorized()
                    404 -> "Usuario no encontrado"
                    500 -> "Error del servidor: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Log.e(TAG, "Failed to get sharing status: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sharing status", e)
            val friendlyMessage = when (e) {
                is java.net.UnknownHostException -> "Sin conexión a internet"
                is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                else -> e.message ?: "Error al obtener estado"
            }
            Result.failure(Exception(friendlyMessage))
        }
    }
}
