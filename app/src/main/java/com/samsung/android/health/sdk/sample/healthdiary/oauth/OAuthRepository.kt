package com.samsung.android.health.sdk.sample.healthdiary.oauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository handling OAuth authentication flows.
 * 
 * Uses Google Sign-In SDK for Google authentication, which is the
 * officially recommended approach for Android apps. This avoids
 * the "Custom scheme URIs not allowed" error that occurs with
 * AppAuth + Web client IDs.
 * 
 * Key benefits:
 * - No custom redirect URI handling needed
 * - ID token returned directly (no token exchange required)
 * - Proper handling of Android client credentials
 * - Native UI with account picker
 */
class OAuthRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "OAuthRepository"
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 1001
    }
    
    /**
     * Google Sign-In client configured for ID token authentication.
     * 
     * IMPORTANT: requestIdToken() requires the **Web Client ID** from Google Cloud Console,
     * not the Android Client ID. The Android Client ID is used internally by the SDK
     * based on package name and SHA-1 fingerprint.
     */
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(OAuthConfig.Google.WEB_CLIENT_ID) // Must be Web Client ID for backend verification
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Launch Google Sign-In flow.
     * 
     * This opens the native Google account picker and handles
     * all OAuth complexity internally. No custom URI scheme needed.
     */
    fun launchGoogleSignIn(activity: Activity) {
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE_SIGN_IN)
    }
    
    /**
     * Handle Google Sign-In result from onActivityResult.
     * 
     * @param data Intent data from onActivityResult
     * @return Result containing OAuth tokens or error
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            // Get the ID token from the signed-in account
            val idToken = account?.idToken
            
            if (idToken == null) {
                Log.e(TAG, "Google Sign-In succeeded but no ID token received")
                return@withContext Result.failure(
                    OAuthException("No ID token received from Google")
                )
            }
            
            Log.d(TAG, "Google Sign-In successful, user: ${account.email}")
            Log.d(TAG, "Sending ID token to backend for verification")
            
            // Send ID token to backend for verification and session creation
            sendIdTokenToBackend(OAuthProvider.GOOGLE, idToken)
            
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign-in cancelled by user"
                GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign-in failed"
                GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign-in already in progress"
                GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid account selected"
                GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error during sign-in"
                12501 -> "Sign-in cancelled by user" // Common cancellation code
                else -> "Google Sign-In error: ${e.statusCode} - ${e.message}"
            }
            Log.e(TAG, "Google Sign-In failed: $errorMessage", e)
            Result.failure(OAuthException(errorMessage, e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            Result.failure(OAuthException("Authentication failed: ${e.message}", e))
        }
    }
    
    /**
     * Check if the user is already signed in with Google.
     */
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * Silent sign-in attempt using cached credentials.
     * Useful for automatic re-authentication.
     */
    suspend fun silentSignIn(): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val account = googleSignInClient.silentSignIn().await()
            val idToken = account?.idToken
            
            if (idToken == null) {
                return@withContext Result.failure(
                    OAuthException("Silent sign-in succeeded but no ID token")
                )
            }
            
            sendIdTokenToBackend(OAuthProvider.GOOGLE, idToken)
            
        } catch (e: ApiException) {
            Log.d(TAG, "Silent sign-in failed, user interaction required: ${e.statusCode}")
            Result.failure(OAuthException("Silent sign-in failed", e))
        } catch (e: Exception) {
            Log.e(TAG, "Silent sign-in error", e)
            Result.failure(OAuthException("Silent sign-in error: ${e.message}", e))
        }
    }
    
    /**
     * Send verified ID token to backend for session creation.
     */
    private suspend fun sendIdTokenToBackend(
        provider: OAuthProvider,
        idToken: String
    ): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        
        try {
            val request = OAuthTokenRequest(
                provider = provider.value,
                idToken = idToken,
                deviceInfo = buildDeviceInfo()
            )
            
            val response = RetrofitClient.authApiService.authenticateWithOAuth(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Backend authentication successful")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Backend authentication failed: ${response.code()} - $errorBody")
                Result.failure(OAuthException("Authentication failed: $errorBody"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to authenticate with backend", e)
            Result.failure(OAuthException("Network error: ${e.message}", e))
        }
    }
    
    /**
     * Build device information for the OAuth request.
     */
    private fun buildDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = Build.ID,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            osVersion = "Android ${Build.VERSION.RELEASE}",
            appVersion = BuildConfig.VERSION_NAME
        )
    }
    
    /**
     * Revokes Google Sign-In credentials and signs out the user.
     * This clears cached credentials and forces account selection on next login.
     */
    suspend fun signOut() {
        try {
            // Revoke access to completely disconnect the app
            googleSignInClient.revokeAccess().await()
            Log.d(TAG, "Google credentials revoked successfully")
        } catch (e: Exception) {
            // Fallback to sign out if revoke fails
            try {
                googleSignInClient.signOut().await()
                Log.d(TAG, "Google sign out successful")
            } catch (signOutException: Exception) {
                Log.w(TAG, "Google sign out failed: ${signOutException.message}")
                // Continue anyway - local token cleanup is more important
            }
        }
    }
}

/**
 * Custom exception for OAuth-related errors.
 */
class OAuthException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
