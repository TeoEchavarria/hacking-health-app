package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.RefreshRequest
import com.samsung.android.health.sdk.sample.healthdiary.utils.AuthEventBus
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that proactively refreshes the auth token before it expires.
 * 
 * This ensures the user stays logged in for extended periods (30+ days)
 * without requiring manual re-authentication, as long as they open the app
 * occasionally.
 * 
 * Strategy:
 * - Runs every 24 hours when network is available
 * - Checks if token will expire within 7 days
 * - If so, attempts to refresh it proactively
 * - If refresh fails, emits session expired event
 */
class TokenRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TokenRefreshWorker"
        private const val WORK_NAME = "token_refresh_periodic"
        
        // Refresh token if it expires within this many days
        private const val REFRESH_BUFFER_DAYS = 7L
        private const val REFRESH_BUFFER_MINUTES = REFRESH_BUFFER_DAYS * 24 * 60
        
        /**
         * Schedule the periodic token refresh worker.
         * Runs daily when network is available.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = PeriodicWorkRequestBuilder<TokenRefreshWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            
            Log.i(TAG, "🔐 Token refresh worker scheduled (every 24 hours)")
        }
        
        /**
         * Trigger an immediate token refresh check (one-time).
         */
        fun triggerNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<TokenRefreshWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "🔐 Immediate token refresh check triggered")
        }
        
        /**
         * Cancel the periodic token refresh worker.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "🔐 Token refresh worker cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔐 Token refresh worker starting...")
        
        // Check if we have a token at all
        if (!TokenManager.hasToken()) {
            Log.d(TAG, "🔐 No token present, skipping refresh")
            return Result.success()
        }
        
        // Check if token needs refresh (expires within buffer period)
        val needsRefresh = TokenManager.isTokenExpired(bufferMinutes = REFRESH_BUFFER_MINUTES)
        
        if (!needsRefresh) {
            Log.d(TAG, "🔐 Token still valid for > $REFRESH_BUFFER_DAYS days, no refresh needed")
            return Result.success()
        }
        
        Log.i(TAG, "🔐 Token expires within $REFRESH_BUFFER_DAYS days, attempting proactive refresh...")
        
        return try {
            val refreshToken = TokenManager.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                Log.w(TAG, "🔐 No refresh token available")
                AuthEventBus.emitSessionExpired(AuthEventBus.SessionExpiredReason.REFRESH_FAILED)
                return Result.failure()
            }
            
            // Call refresh endpoint synchronously
            val response = RetrofitClient.authApiService.refresh(
                RefreshRequest(refresh = refreshToken)
            ).execute()
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                // Save new tokens
                TokenManager.saveAuthInfo(
                    token = body.token,
                    refreshToken = body.refresh,
                    expiry = body.expiry
                )
                
                Log.i(TAG, "🔐 ✅ Token refresh successful! Session extended.")
                Result.success()
            } else {
                Log.e(TAG, "🔐 ❌ Token refresh failed: ${response.code()}")
                
                // If refresh token is invalid (401/403), session is truly expired
                if (response.code() == 401 || response.code() == 403) {
                    AuthEventBus.emitSessionExpired(AuthEventBus.SessionExpiredReason.REFRESH_FAILED)
                    Result.failure()
                } else {
                    // Other errors (500, network) - retry later
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔐 ❌ Exception during token refresh", e)
            // Network error - retry later
            Result.retry()
        }
    }
}
