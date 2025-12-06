package com.samsung.android.health.sdk.sample.healthdiary.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_EXPIRY = "token_expiry"
    private const val MASTER_KEY_ALIAS = "_androidx_security_crypto_encrypted_prefs_key_"
    
    private var encryptedPrefs: SharedPreferences? = null
    
    fun initialize(context: Context) {
        if (encryptedPrefs == null) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                // Fallback to regular SharedPreferences if encryption fails
                encryptedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }
    
    fun saveAuthInfo(token: String, refreshToken: String, expiry: String) {
        encryptedPrefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putString(KEY_REFRESH, refreshToken)
            ?.putString(KEY_EXPIRY, expiry)
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
    
    fun clearToken() {
        encryptedPrefs?.edit()
            ?.remove(KEY_TOKEN)
            ?.remove(KEY_REFRESH)
            ?.remove(KEY_EXPIRY)
            ?.apply()
    }
    
    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }
}



