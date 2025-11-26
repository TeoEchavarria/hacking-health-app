package com.samsung.android.health.sdk.sample.healthdiary.repository

import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.DateRange
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SyncRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SyncResponse
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import retrofit2.HttpException
import java.io.IOException

class SyncRepository {
    
    private val apiService = RetrofitClient.syncApiService
    
    suspend fun syncData(
        syncType: String,
        dateRange: DateRange? = null,
        forceRefresh: Boolean = false
    ): Result<SyncResponse> {
        return try {
            if (!TokenManager.hasToken()) {
                return Result.failure(Exception("No authentication token available. Please set your access token."))
            }
            
            val request = SyncRequest(
                syncType = syncType,
                dateRange = dateRange,
                forceRefresh = forceRefresh
            )
            
            val response = apiService.syncData(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> "Unauthorized: Invalid or missing authentication token"
                    400 -> "Bad Request: $errorBody"
                    500 -> "Internal Server Error: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Result.failure(HttpException(response))
            }
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun validateToken(): Boolean {
        return TokenManager.hasToken()
    }
}



