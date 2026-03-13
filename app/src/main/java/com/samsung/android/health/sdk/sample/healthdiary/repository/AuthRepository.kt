package com.samsung.android.health.sdk.sample.healthdiary.repository

import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.RefreshRequest
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthTokenRequest
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthTokenResponse
import retrofit2.HttpException
import java.io.IOException

import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager

class AuthRepository {
    
    private val apiService = RetrofitClient.authApiService
    
    suspend fun logout() {
        try {
            val refresh = TokenManager.getRefreshToken()
            if (!refresh.isNullOrEmpty()) {
                apiService.logout(RefreshRequest(refresh))
            }
        } catch (e: Exception) {
            // Best effort - ignore network errors
        } finally {
            TokenManager.clearToken()
        }
    }

    /**
     * Legacy username/password login.
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                // Save tokens
                TokenManager.saveAuthInfo(
                    token = loginResponse.token,
                    refreshToken = loginResponse.refresh,
                    expiry = loginResponse.expiry
                )
                Result.success(loginResponse)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    401 -> "Unauthorized: Invalid credentials"
                    403 -> parseLoginError(errorBody)
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
    
    /**
     * OAuth authentication - exchange provider token for app tokens.
     */
    suspend fun authenticateWithOAuth(request: OAuthTokenRequest): Result<OAuthTokenResponse> {
        return try {
            val response = apiService.authenticateWithOAuth(request)
            
            if (response.isSuccessful && response.body() != null) {
                val oauthResponse = response.body()!!
                // Save OAuth tokens
                TokenManager.saveOAuthTokens(
                    accessToken = oauthResponse.accessToken,
                    refreshToken = oauthResponse.refreshToken,
                    expiresIn = oauthResponse.expiresIn
                )
                Result.success(oauthResponse)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = when (response.code()) {
                    400 -> "Invalid OAuth provider or request"
                    401 -> "OAuth token verification failed"
                    409 -> "Email conflict - account already exists with different provider"
                    500 -> "Server error during OAuth authentication"
                    else -> "OAuth error ${response.code()}: $errorBody"
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
    
    /**
     * Get available OAuth providers from server.
     */
    suspend fun getAvailableOAuthProviders(): Result<List<String>> {
        return try {
            val response = apiService.getOAuthProviders()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.providers)
            } else {
                Result.failure(Exception("Failed to fetch OAuth providers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse login error message from backend response.
     */
    private fun parseLoginError(errorBody: String): String {
        return when {
            errorBody.contains("social login", ignoreCase = true) -> 
                "This account uses social login. Please sign in with Google."
            errorBody.contains("invalid password", ignoreCase = true) -> 
                "Invalid password"
            else -> 
                "Authentication failed: $errorBody"
        }
    }
}


