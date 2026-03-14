package com.samsung.android.health.sdk.sample.healthdiary.oauth

import android.net.Uri
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig

/**
 * OAuth configuration for supported providers.
 */
object OAuthConfig {
    
    /**
     * Generate redirect URI using reverse client ID format.
     * Google requires this format for Android apps using AppAuth.
     * 
     * Client ID: xxx.apps.googleusercontent.com
     * Redirect:  com.googleusercontent.apps.xxx:/oauth2redirect
     */
    val REDIRECT_URI: Uri
        get() {
            val clientId = Google.CLIENT_ID
            // Extract the prefix before .apps.googleusercontent.com
            val clientIdPrefix = clientId.replace(".apps.googleusercontent.com", "")
            val reverseClientId = "com.googleusercontent.apps.$clientIdPrefix"
            return Uri.parse("$reverseClientId:/oauth2redirect")
        }
    
    // Also expose the scheme for AndroidManifest
    val REDIRECT_SCHEME: String
        get() {
            val clientId = Google.CLIENT_ID
            val clientIdPrefix = clientId.replace(".apps.googleusercontent.com", "")
            return "com.googleusercontent.apps.$clientIdPrefix"
        }
    
    /**
     * Google OAuth configuration.
     */
    object Google {
        val CLIENT_ID: String
            get() = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
        
        val AUTHORIZATION_ENDPOINT: Uri = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
        val TOKEN_ENDPOINT: Uri = Uri.parse("https://oauth2.googleapis.com/token")
        
        // OpenID Connect scopes
        val SCOPES = listOf(
            "openid",
            "email",
            "profile"
        )
        
        // Additional parameters for Google OAuth
        const val PROMPT = "select_account"  // Always show account chooser
    }
    
    /**
     * GitHub OAuth configuration (for future use).
     */
    object GitHub {
        val AUTHORIZATION_ENDPOINT: Uri = Uri.parse("https://github.com/login/oauth/authorize")
        val TOKEN_ENDPOINT: Uri = Uri.parse("https://github.com/login/oauth/access_token")
        
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
            OAuthProvider.GOOGLE -> Google.CLIENT_ID.isNotBlank()
            OAuthProvider.GITHUB -> false  // Not yet configured
            OAuthProvider.APPLE -> false   // Not yet configured
        }
    }
}
