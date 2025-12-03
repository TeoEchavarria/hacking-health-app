package com.samsung.android.health.sdk.sample.healthdiary.config

import android.content.Context
import android.content.SharedPreferences
import com.samsung.android.health.sdk.sample.healthdiary.config.AppConstants.DEFAULT_TIMEOUT_SECONDS
import com.samsung.android.health.sdk.sample.healthdiary.config.AppConstants.MAX_RETRY_ATTEMPTS
import com.samsung.android.health.sdk.sample.healthdiary.config.AppConstants.RETRY_DELAY_MS

/**
 * TxAgent API Configuration
 * Manages API endpoints, authentication, and request/response settings
 */
object TxAgentConfig {
    
    private const val PREFS_NAME = "txagent_config"
    private const val KEY_BASE_URL = "txagent_base_url"
    private const val KEY_API_KEY = "txagent_api_key"
    private const val KEY_TIMEOUT = "txagent_timeout"
    private const val KEY_ENABLED = "txagent_enabled"
    
    // Default TxAgent settings
    private const val DEFAULT_BASE_URL = "https://txagent.example.com/api"
    private const val DEFAULT_API_VERSION = "v1"
    
    private var prefs: SharedPreferences? = null
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get TxAgent base URL
     */
    fun getBaseUrl(): String {
        return prefs?.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }
    
    /**
     * Set TxAgent base URL
     */
    fun setBaseUrl(url: String) {
        prefs?.edit()?.putString(KEY_BASE_URL, url)?.apply()
    }
    
    /**
     * Get API key for TxAgent authentication
     */
    fun getApiKey(): String {
        return prefs?.getString(KEY_API_KEY, "") ?: ""
    }
    
    /**
     * Set API key for TxAgent
     */
    fun setApiKey(apiKey: String) {
        prefs?.edit()?.putString(KEY_API_KEY, apiKey)?.apply()
    }
    
    /**
     * Get request timeout in seconds
     */
    fun getTimeout(): Long {
        return prefs?.getLong(KEY_TIMEOUT, DEFAULT_TIMEOUT_SECONDS) ?: DEFAULT_TIMEOUT_SECONDS
    }
    
    /**
     * Set request timeout
     */
    fun setTimeout(timeoutSeconds: Long) {
        prefs?.edit()?.putLong(KEY_TIMEOUT, timeoutSeconds)?.apply()
    }
    
    /**
     * Check if TxAgent integration is enabled
     */
    fun isEnabled(): Boolean {
        return prefs?.getBoolean(KEY_ENABLED, false) ?: false
    }
    
    /**
     * Enable or disable TxAgent integration
     */
    fun setEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }
    
    /**
     * Get retry policy for failed requests
     */
    fun getRetryPolicy(): RetryPolicy {
        return RetryPolicy(
            maxAttempts = MAX_RETRY_ATTEMPTS,
            delayMs = RETRY_DELAY_MS,
            backoffMultiplier = 2.0
        )
    }
    
    /**
     * Get full endpoint URL for a specific path
     */
    fun getEndpoint(path: String): String {
        val baseUrl = getBaseUrl().trimEnd('/')
        val cleanPath = path.trimStart('/')
        return "$baseUrl/$DEFAULT_API_VERSION/$cleanPath"
    }
    
    /**
     * Get authentication headers
     */
    fun getAuthHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val apiKey = getApiKey()
        
        if (apiKey.isNotEmpty()) {
            headers["X-API-Key"] = apiKey
        }
        
        headers["Content-Type"] = "application/json"
        headers["Accept"] = "application/json"
        
        return headers
    }
    
    /**
     * Validate configuration
     */
    fun isConfigured(): Boolean {
        return getBaseUrl().isNotEmpty() && getApiKey().isNotEmpty()
    }
    
    /**
     * Clear all TxAgent configuration
     */
    fun clearConfig() {
        prefs?.edit()?.clear()?.apply()
    }
}

/**
 * Retry policy for API requests
 */
data class RetryPolicy(
    val maxAttempts: Int,
    val delayMs: Long,
    val backoffMultiplier: Double
) {
    /**
     * Calculate delay for a specific retry attempt
     */
    fun getDelayForAttempt(attempt: Int): Long {
        return (delayMs * Math.pow(backoffMultiplier, attempt.toDouble())).toLong()
    }
}
