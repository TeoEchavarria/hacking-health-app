package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.RefreshRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SuccessResponse
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthTokenRequest
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthTokenResponse
import com.samsung.android.health.sdk.sample.healthdiary.oauth.OAuthProvidersResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    
    // ==========================================================================
    // Legacy Authentication (username/password)
    // ==========================================================================
    
    @POST(ApiConstants.API_PATH_LOGIN)
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/refresh")
    fun refresh(
        @Body request: RefreshRequest
    ): retrofit2.Call<LoginResponse>

    @POST("/logout")
    suspend fun logout(
        @Body request: RefreshRequest
    ): Response<Map<String, Boolean>>
    
    /**
     * Revoke all tokens for the current user.
     * Requires Authorization header with Bearer token.
     */
    @DELETE("/revoke")
    suspend fun revokeTokens(
        @Header("Authorization") authorization: String
    ): Response<SuccessResponse>
    
    // ==========================================================================
    // OAuth Authentication
    // ==========================================================================
    
    /**
     * Exchange OAuth provider ID token for application tokens.
     * 
     * The backend verifies the ID token with the provider (e.g., Google),
     * creates or links the user account, and returns JWT access/refresh tokens.
     */
    @POST("/oauth/token")
    suspend fun authenticateWithOAuth(
        @Body request: OAuthTokenRequest
    ): Response<OAuthTokenResponse>
    
    /**
     * Get list of available OAuth providers.
     */
    @GET("/oauth/providers")
    suspend fun getOAuthProviders(): Response<OAuthProvidersResponse>
}


