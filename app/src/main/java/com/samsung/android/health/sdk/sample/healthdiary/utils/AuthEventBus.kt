package com.samsung.android.health.sdk.sample.healthdiary.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Centralized event bus for authentication events.
 * 
 * When a session expires (401 that can't be recovered via refresh),
 * repositories and authenticators emit events here. The UI layer
 * observes these events and redirects to the login screen.
 * 
 * This prevents scattered "No autorizado" error handling across
 * repositories and ensures consistent logout behavior.
 */
object AuthEventBus {
    private const val TAG = "AuthEventBus"
    
    /**
     * Reasons for session expiration.
     */
    enum class SessionExpiredReason {
        TOKEN_EXPIRED,          // Token expired and refresh failed
        REFRESH_FAILED,         // Refresh token is invalid/expired
        UNAUTHORIZED_API,       // API returned 401 after refresh attempt
        MANUAL_LOGOUT           // User initiated logout
    }
    
    private val _sessionExpired = MutableSharedFlow<SessionExpiredReason>(
        replay = 0,           // Don't replay old events to new collectors
        extraBufferCapacity = 1 // Buffer one event to prevent suspension
    )
    
    /**
     * Flow that emits when the session has expired and user must re-login.
     * 
     * Collectors should:
     * 1. Clear any cached user data
     * 2. Navigate to LoginActivity with CLEAR_TASK flag
     */
    val sessionExpired: SharedFlow<SessionExpiredReason> = _sessionExpired.asSharedFlow()
    
    /**
     * Emit a session expired event.
     * 
     * Call this when:
     * - TokenAuthenticator fails to refresh the token
     * - A repository receives 401 after the authenticator already tried refresh
     * - Token is detected as expired on app startup
     * 
     * This will also clear tokens to ensure a clean state.
     */
    fun emitSessionExpired(reason: SessionExpiredReason = SessionExpiredReason.UNAUTHORIZED_API) {
        Log.w(TAG, "🔐 Session expired: $reason")
        
        // Clear tokens to ensure clean state
        try {
            TokenManager.clearToken()
            Log.d(TAG, "Tokens cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear tokens", e)
        }
        
        // Emit event (non-blocking)
        CoroutineScope(Dispatchers.Main).launch {
            _sessionExpired.emit(reason)
        }
    }
    
    /**
     * Helper function to handle unauthorized (401) responses in repositories.
     * 
     * Call this instead of manually returning "No autorizado" errors.
     * This ensures consistent behavior: emit event + return failure.
     * 
     * Usage in repository:
     * ```kotlin
     * when (response.code()) {
     *     401 -> return AuthEventBus.handleUnauthorized()
     *     // other codes...
     * }
     * ```
     * 
     * @return Result.failure with a user-friendly message
     */
    fun <T> handleUnauthorized(): Result<T> {
        emitSessionExpired(SessionExpiredReason.UNAUTHORIZED_API)
        return Result.failure(SessionExpiredException("Sesión expirada"))
    }
}

/**
 * Exception thrown when the session has expired.
 * 
 * This is a marker exception that can be caught by UI layers
 * to distinguish session expiration from other errors.
 */
class SessionExpiredException(message: String = "Sesión expirada") : Exception(message)
