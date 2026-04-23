package com.samsung.android.health.sdk.sample.healthdiary.api

import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.models.RefreshRequest
import com.samsung.android.health.sdk.sample.healthdiary.utils.AuthEventBus
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator that handles automatic token refresh on 401 responses.
 * 
 * When a request returns 401 Unauthorized:
 * 1. Gets the current refresh token
 * 2. Calls the /refresh endpoint synchronously
 * 3. Saves the new tokens via TokenManager
 * 4. Retries the original request with the new access token
 * 
 * If refresh fails or there's no refresh token, returns null to let the 401 propagate.
 */
class TokenAuthenticator : Authenticator {
    
    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val MAX_RETRY_COUNT = 2
        
        // Track retry count to prevent infinite loops
        @Volatile
        private var isRefreshing = false
        private val refreshLock = Any()
    }
    
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "🔄 [AUTO-REFRESH] Received 401, attempting token refresh")
        
        // Check if this request already had an auth header
        val originalRequest = response.request
        val authHeader = originalRequest.header("Authorization")
        if (authHeader == null) {
            Log.d(TAG, "🔄 [AUTO-REFRESH] No Authorization header, skipping refresh")
            return null
        }
        
        // Prevent infinite refresh loops - if we've already retried, stop
        val retryCount = response.request.header("X-Refresh-Retry-Count")?.toIntOrNull() ?: 0
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.w(TAG, "🔄 [AUTO-REFRESH] Max retry count reached, giving up")
            return null
        }
        
        // Get refresh token
        val refreshToken = TokenManager.getRefreshToken()
        if (refreshToken == null) {
            Log.w(TAG, "🔄 [AUTO-REFRESH] No refresh token available")
            return null
        }
        
        // Synchronize to prevent multiple simultaneous refresh calls
        synchronized(refreshLock) {
            // Double-check if another thread already refreshed
            val currentToken = TokenManager.getToken()
            val originalToken = authHeader.removePrefix("Bearer ").trim()
            
            if (currentToken != null && currentToken != originalToken) {
                // Token was already refreshed by another thread
                Log.d(TAG, "🔄 [AUTO-REFRESH] Token already refreshed by another request")
                return originalRequest.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header("X-Refresh-Retry-Count", (retryCount + 1).toString())
                    .build()
            }
            
            if (isRefreshing) {
                Log.d(TAG, "🔄 [AUTO-REFRESH] Another refresh in progress, waiting...")
                return null
            }
            
            isRefreshing = true
        }
        
        return try {
            doRefresh(refreshToken, originalRequest, retryCount)
        } finally {
            synchronized(refreshLock) {
                isRefreshing = false
            }
        }
    }
    
    private fun doRefresh(refreshToken: String, originalRequest: Request, retryCount: Int): Request? {
        Log.d(TAG, "🔄 [AUTO-REFRESH] Calling /refresh endpoint...")
        
        return try {
            // Make synchronous refresh call
            val refreshCall = RetrofitClient.authApiService.refresh(
                RefreshRequest(refresh = refreshToken)
            )
            
            val refreshResponse = refreshCall.execute()
            
            if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body()
                if (body != null) {
                    Log.d(TAG, "🔄 [AUTO-REFRESH] ✅ Token refresh successful!")
                    
                    // Save new tokens
                    TokenManager.saveAuthInfo(
                        token = body.token,
                        refreshToken = body.refresh,
                        expiry = body.expiry
                    )
                    
                    // Retry original request with new token
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer ${body.token}")
                        .header("X-Refresh-Retry-Count", (retryCount + 1).toString())
                        .build()
                } else {
                    Log.e(TAG, "🔄 [AUTO-REFRESH] ❌ Refresh succeeded but body is null")
                    null
                }
            } else {
                val errorBody = refreshResponse.errorBody()?.string()
                Log.e(TAG, "🔄 [AUTO-REFRESH] ❌ Refresh failed: ${refreshResponse.code()} - $errorBody")
                
                // If refresh token is also invalid, emit session expired event
                if (refreshResponse.code() == 403 || refreshResponse.code() == 401) {
                    Log.w(TAG, "🔄 [AUTO-REFRESH] Refresh token invalid, emitting session expired")
                    AuthEventBus.emitSessionExpired(AuthEventBus.SessionExpiredReason.REFRESH_FAILED)
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔄 [AUTO-REFRESH] ❌ Exception during refresh: ${e.message}", e)
            // Network error during refresh - emit session expired so user can re-login
            AuthEventBus.emitSessionExpired(AuthEventBus.SessionExpiredReason.REFRESH_FAILED)
            null
        }
    }
}
