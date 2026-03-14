package com.samsung.android.health.sdk.sample.healthdiary.oauth

import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig

/**
 * OAuth configuration for supported providers.
 * 
 * IMPORTANT for Google Sign-In SDK:
 * - The SDK uses the Android Client ID internally (based on package name + SHA-1)
 * - requestIdToken() requires the WEB Client ID to get a token verifiable by backend
 * - Both client IDs must be from the same Google Cloud project
 */
object OAuthConfig {
    
    /**
     * Google OAuth configuration.
     * 
     * For Google Sign-In SDK on Android:
     * - ANDROID_CLIENT_ID: Used internally by SDK (matches package + SHA-1 fingerprint)
     * - WEB_CLIENT_ID: Used with requestIdToken() for backend verification
     */
    object Google {
        /**
         * Web Client ID - Required for requestIdToken().
         * The backend will verify tokens issued to this client ID.
         */
        val WEB_CLIENT_ID: String
            get() = BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID
        
        /**
         * Android Client ID - Used internally by Google Sign-In SDK.
         * Must match the registered package name and SHA-1 fingerprint.
         */
        val ANDROID_CLIENT_ID: String
            get() = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
        
        // OpenID Connect scopes requested
        val SCOPES = listOf(
            "openid",
            "email",
            "profile"
        )
    }
    
    /**
     * GitHub OAuth configuration (for future use).
     */
    object GitHub {
        val SCOPES = listOf(
            "read:user",
            "user:email"
        )
    }
    
    /**
     * Check if a provider is properly configured.
     */
    fun isProviderConfigured(provider: OAuthProvider): Boolean {
        return when (provider) {
            OAuthProvider.GOOGLE -> Google.WEB_CLIENT_ID.isNotBlank()
            OAuthProvider.GITHUB -> false  // Not yet configured
            OAuthProvider.APPLE -> false   // Not yet configured
        }
    }
}
