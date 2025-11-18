package com.samsung.android.health.sdk.sample.healthdiary.repository

import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginResponse
import retrofit2.HttpException
import java.io.IOException

class AuthRepository {
    
    private val apiService = RetrofitClient.authApiService
    
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> "Unauthorized: Invalid credentials"
                    404 -> "User not found"
                    400 -> "Bad Request: $errorBody"
                    500 -> "Internal Server Error: $errorBody"
                    else -> "Error ${response.code()}: $errorBody"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

