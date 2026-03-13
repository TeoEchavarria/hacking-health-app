package com.samsung.android.health.sdk.sample.healthdiary.oauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.coroutines.resume

/**
 * Repository handling OAuth authentication flows.
 * 
 * Implements OAuth2 Authorization Code Flow with PKCE for secure
 * mobile authentication.
 */
class OAuthRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "OAuthRepository"
    }
    
    // PKCE storage for code verifier (preserved across authorization request/response)
    private var codeVerifier: String? = null
    
    /**
     * Build Google OAuth authorization request with PKCE.
     */
    fun buildGoogleAuthRequest(): AuthorizationRequest {
        // Generate PKCE code verifier
        codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier!!)
        
        val serviceConfig = AuthorizationServiceConfiguration(
            OAuthConfig.Google.AUTHORIZATION_ENDPOINT,
            OAuthConfig.Google.TOKEN_ENDPOINT
        )
        
        return AuthorizationRequest.Builder(
            serviceConfig,
            OAuthConfig.Google.CLIENT_ID,
            ResponseTypeValues.CODE,
            OAuthConfig.REDIRECT_URI
        )
            .setScopes(OAuthConfig.Google.SCOPES)
            .setCodeVerifier(codeVerifier, codeChallenge, "S256")
            .setPrompt(OAuthConfig.Google.PROMPT)
            .build()
    }
    
    /**
     * Launch OAuth authorization flow using Chrome Custom Tabs.
     */
    fun launchAuthFlow(activity: Activity, request: AuthorizationRequest) {
        val authService = AuthorizationService(activity)
        
        // Build custom tabs intent for better UX
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        
        val authIntent = authService.getAuthorizationRequestIntent(request, customTabsIntent)
        activity.startActivityForResult(authIntent, REQUEST_CODE_AUTH)
    }
    
    /**
     * Handle OAuth authorization response.
     * 
     * This extracts the ID token from Google's authorization response
     * and sends it to our backend for verification and session creation.
     */
    suspend fun handleAuthorizationResponse(
        response: AuthorizationResponse?,
        exception: AuthorizationException?
    ): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        
        if (exception != null) {
            Log.e(TAG, "Authorization failed: ${exception.error} - ${exception.errorDescription}")
            return@withContext Result.failure(
                OAuthException("Authorization failed: ${exception.errorDescription ?: exception.error}")
            )
        }
        
        if (response == null) {
            return@withContext Result.failure(OAuthException("No authorization response"))
        }
        
        try {
            // Exchange authorization code for tokens
            val tokenResponse = exchangeCodeForTokens(response)
            
            // Get ID token from the token response
            val idToken = tokenResponse?.idToken
                ?: return@withContext Result.failure(OAuthException("No ID token received"))
            
            Log.d(TAG, "Received ID token from Google, sending to backend for verification")
            
            // Send ID token to our backend for verification
            val backendResponse = sendIdTokenToBackend(OAuthProvider.GOOGLE, idToken)
            
            // Clear PKCE verifier
            codeVerifier = null
            
            backendResponse
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling authorization response", e)
            Result.failure(OAuthException("Failed to complete authentication: ${e.message}", e))
        }
    }
    
    /**
     * Exchange authorization code for tokens using PKCE.
     */
    private suspend fun exchangeCodeForTokens(
        authResponse: AuthorizationResponse
    ): TokenResponse? = withContext(Dispatchers.IO) {
        
        val authService = AuthorizationService(context)
        
        val tokenRequest = authResponse.createTokenExchangeRequest()
        
        return@withContext suspendCancellableCoroutine { continuation ->
            authService.performTokenRequest(tokenRequest) { response, ex ->
                if (ex != null) {
                    Log.e(TAG, "Token exchange failed: ${ex.error}")
                    continuation.resume(null)
                } else {
                    continuation.resume(response)
                }
            }
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
     * Generate a secure random PKCE code verifier.
     */
    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    /**
     * Generate code challenge from verifier using SHA-256.
     */
    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }
    
    companion object {
        const val REQUEST_CODE_AUTH = 1001
    }
}

/**
 * Custom exception for OAuth-related errors.
 */
class OAuthException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
