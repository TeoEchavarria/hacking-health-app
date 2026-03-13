package com.samsung.android.health.sdk.sample.healthdiary.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Secure token manager for authentication tokens.
 * 
 * Supports both JWT tokens (with built-in expiry claims) and
 * legacy tokens (with stored expiry timestamps).
 */
object TokenManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_EXPIRY = "token_expiry"
    private const val KEY_AUTH_METHOD = "auth_method"
    private const val MASTER_KEY_ALIAS = "_androidx_security_crypto_encrypted_prefs_key_"
    
    // Auth method constants
    const val AUTH_METHOD_PASSWORD = "password"
    const val AUTH_METHOD_OAUTH = "oauth"
    
    @Volatile
    private var encryptedPrefs: SharedPreferences? = null
    private val lock = Any()
    
    fun initialize(context: Context) {
        if (encryptedPrefs == null) {
            synchronized(lock) {
                if (encryptedPrefs == null) {
                    try {
                        val masterKey = MasterKey.Builder(context.applicationContext)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build()
                        
                        encryptedPrefs = EncryptedSharedPreferences.create(
                            context.applicationContext,
                            PREFS_NAME,
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                    } catch (e: Exception) {
                        // Fallback to regular SharedPreferences if encryption fails
                        encryptedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    }
                }
            }
        }
    }
    
    /**
     * Save authentication info from legacy login response.
     */
    fun saveAuthInfo(token: String, refreshToken: String, expiry: String) {
        encryptedPrefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putString(KEY_REFRESH, refreshToken)
            ?.putString(KEY_EXPIRY, expiry)
            ?.putString(KEY_AUTH_METHOD, AUTH_METHOD_PASSWORD)
            ?.apply()
    }
    
    /**
     * Save authentication info from OAuth response.
     * Extracts expiry from JWT token if available.
     */
    fun saveOAuthTokens(accessToken: String, refreshToken: String, expiresIn: Int) {
        // Try to extract expiry from JWT, otherwise calculate from expiresIn
        val expiry = getJwtExpiry(accessToken) 
            ?: LocalDateTime.now().plusSeconds(expiresIn.toLong())
        
        encryptedPrefs?.edit()
            ?.putString(KEY_TOKEN, accessToken)
            ?.putString(KEY_REFRESH, refreshToken)
            ?.putString(KEY_EXPIRY, expiry.toString())
            ?.putString(KEY_AUTH_METHOD, AUTH_METHOD_OAUTH)
            ?.apply()
    }
    
    fun saveToken(token: String) {
        encryptedPrefs?.edit()?.putString(KEY_TOKEN, token)?.apply()
    }
    
    fun getToken(): String? {
        return encryptedPrefs?.getString(KEY_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return encryptedPrefs?.getString(KEY_REFRESH, null)
    }
    
    fun getAuthMethod(): String? {
        return encryptedPrefs?.getString(KEY_AUTH_METHOD, null)
    }
    
    fun clearToken() {
        encryptedPrefs?.edit()
            ?.remove(KEY_TOKEN)
            ?.remove(KEY_REFRESH)
            ?.remove(KEY_EXPIRY)
            ?.remove(KEY_AUTH_METHOD)
            ?.apply()
    }
    
    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    /**
     * Check if the current token is expired.
     * 
     * For JWT tokens, reads the exp claim directly.
     * For legacy tokens, uses the stored expiry timestamp.
     */
    fun isTokenExpired(bufferMinutes: Long = 0): Boolean {
        val token = getToken() ?: return true
        
        // Try JWT expiry first
        val jwtExpiry = getJwtExpiry(token)
        if (jwtExpiry != null) {
            val now = LocalDateTime.now()
            return now.plusMinutes(bufferMinutes).isAfter(jwtExpiry)
        }
        
        // Fall back to stored expiry
        val expiryStr = encryptedPrefs?.getString(KEY_EXPIRY, null) ?: return true
        return try {
            val expiry = LocalDateTime.parse(expiryStr)
            val now = LocalDateTime.now()
            now.plusMinutes(bufferMinutes).isAfter(expiry)
        } catch (e: Exception) {
            true
        }
    }
    
    /**
     * Decode JWT payload without verification.
     * 
     * WARNING: This does NOT verify the signature.
     * Use only for reading non-sensitive claims like expiry.
     */
    fun decodeJwtPayload(jwt: String): JSONObject? {
        return try {
            val parts = jwt.split(".")
            if (parts.size != 3) return null
            
            // JWT payload is base64url encoded
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP),
                Charsets.UTF_8
            )
            JSONObject(payload)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get expiry time from JWT exp claim.
     */
    fun getJwtExpiry(jwt: String): LocalDateTime? {
        return try {
            val payload = decodeJwtPayload(jwt) ?: return null
            if (!payload.has("exp")) return null
            
            val expTimestamp = payload.getLong("exp")
            val instant = Instant.ofEpochSecond(expTimestamp)
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get user ID from JWT sub claim.
     */
    fun getUserIdFromToken(): String? {
        val token = getToken() ?: return null
        return try {
            val payload = decodeJwtPayload(token) ?: return null
            payload.optString("sub", null)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get email from JWT email claim.
     */
    fun getEmailFromToken(): String? {
        val token = getToken() ?: return null
        return try {
            val payload = decodeJwtPayload(token) ?: return null
            payload.optString("email", null)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get scopes from JWT scopes claim.
     */
    fun getScopesFromToken(): List<String> {
        val token = getToken() ?: return emptyList()
        return try {
            val payload = decodeJwtPayload(token) ?: return emptyList()
            val scopesArray = payload.optJSONArray("scopes") ?: return emptyList()
            
            (0 until scopesArray.length()).map { scopesArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}




