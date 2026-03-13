package com.samsung.android.health.sdk.sample.healthdiary.oauth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.health.sdk.sample.healthdiary.activity.LoginActivity
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

/**
 * Activity that handles OAuth redirect callbacks.
 * 
 * This activity is registered with the OAuth redirect URI scheme and
 * receives the authorization response from the OAuth provider.
 * It extracts the response and forwards it to the LoginActivity.
 */
class OAuthRedirectActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "OAuthRedirectActivity"
        const val EXTRA_OAUTH_RESPONSE = "oauth_response"
        const val EXTRA_OAUTH_EXCEPTION = "oauth_exception"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uri = intent.data
        Log.d(TAG, "Received OAuth callback: $uri")
        
        // Extract authorization response or exception from the intent
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)
        
        if (response != null) {
            Log.d(TAG, "Authorization response received: state=${response.state}")
        }
        
        if (exception != null) {
            Log.e(TAG, "Authorization exception: ${exception.error} - ${exception.errorDescription}")
        }
        
        // Forward to LoginActivity for processing
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            // Clear top to return to existing LoginActivity if present
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            
            // Pass the OAuth response data
            if (response != null) {
                putExtra(EXTRA_OAUTH_RESPONSE, response.jsonSerializeString())
            }
            if (exception != null) {
                putExtra(EXTRA_OAUTH_EXCEPTION, exception.toJsonString())
            }
        }
        
        startActivity(loginIntent)
        finish()
    }
}
