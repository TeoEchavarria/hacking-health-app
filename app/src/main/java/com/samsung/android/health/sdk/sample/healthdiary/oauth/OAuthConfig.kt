package com.samsung.android.health.sdk.sample.healthdiary.oauth

import android.net.Uri
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig

/**
 * OAuth configuration for supported providers.
 */
object OAuthConfig {
    
    // Redirect URI for OAuth callback
    const val REDIRECT_SCHEME = "com.samsung.android.health.sdk.sample.healthdiary"
    const val REDIRECT_HOST = "oauth"
    const val REDIRECT_PATH = "/callback"
    val REDIRECT_URI: Uri = Uri.Builder()
        .scheme(REDIRECT_SCHEME)
        .authority(REDIRECT_HOST)
        .path(REDIRECT_PATH)
        .build()
    
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
