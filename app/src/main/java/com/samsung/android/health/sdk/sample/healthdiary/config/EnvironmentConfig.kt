package com.samsung.android.health.sdk.sample.healthdiary.config

import android.content.Context
import android.content.SharedPreferences
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig

/**
 * Environment configuration loader
 * Reads from BuildConfig, SharedPreferences, and provides fallback defaults
 */
object EnvironmentConfig {
    
    private const val PREFS_NAME = "environment_config"
    private const val KEY_API_BASE_URL = "api_base_url"
    private const val KEY_API_TIMEOUT = "api_timeout"
    private const val KEY_ENVIRONMENT = "environment"
    
    // Default values
    private const val DEFAULT_API_TIMEOUT = 30000L // 30 seconds
    private const val DEFAULT_API_BASE_URL = "https://api.example.com"
    
    private var prefs: SharedPreferences? = null
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get the API base URL
     * Priority: SharedPreferences > BuildConfig > Default
     */
    fun getApiBaseUrl(): String {
        return prefs?.getString(KEY_API_BASE_URL, null)
            ?: BuildConfig.API_BASE_URL.takeIf { it.isNotEmpty() }
            ?: DEFAULT_API_BASE_URL
    }
    
    /**
     * Set the API base URL at runtime
     */
    fun setApiBaseUrl(url: String) {
        prefs?.edit()?.putString(KEY_API_BASE_URL, url)?.apply()
    }
    
    /**
     * Get API timeout in milliseconds
     */
    fun getApiTimeout(): Long {
        return prefs?.getLong(KEY_API_TIMEOUT, DEFAULT_API_TIMEOUT)
            ?: DEFAULT_API_TIMEOUT
    }
    
    /**
     * Set API timeout at runtime
     */
    fun setApiTimeout(timeout: Long) {
        prefs?.edit()?.putLong(KEY_API_TIMEOUT, timeout)?.apply()
    }
    
    /**
     * Check if app is in debug mode
     */
    fun isDebugMode(): Boolean {
        return BuildConfig.DEBUG
    }
    
    /**
     * Get current environment
     */
    fun getEnvironment(): Environment {
        val envName = prefs?.getString(KEY_ENVIRONMENT, null)
            ?: BuildConfig.BUILD_TYPE
        
        return when (envName.lowercase()) {
            "debug" -> Environment.DEBUG
            "release" -> Environment.RELEASE
            "staging" -> Environment.STAGING
            else -> if (BuildConfig.DEBUG) Environment.DEBUG else Environment.RELEASE
        }
    }
    
    /**
     * Set environment at runtime
     */
    fun setEnvironment(environment: Environment) {
        prefs?.edit()?.putString(KEY_ENVIRONMENT, environment.name)?.apply()
    }
    
    /**
     * Clear all runtime configuration
     */
    fun clearRuntimeConfig() {
        prefs?.edit()?.clear()?.apply()
    }
}

enum class Environment {
    DEBUG,
    STAGING,
    RELEASE
}
