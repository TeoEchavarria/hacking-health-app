package com.samsung.android.health.sdk.sample.healthdiary.config

import android.content.Context

/**
 * Configuration initializer
 * Call this once during app startup to initialize all configuration modules
 */
object ConfigInitializer {
    
    private var isInitialized = false
    
    /**
     * Initialize all configuration modules
     * Should be called in Application.onCreate()
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            return
        }
        
        val appContext = context.applicationContext
        
        // Initialize all config modules
        EnvironmentConfig.initialize(appContext)
        DeviceConfig.initialize(appContext)
        TxAgentConfig.initialize(appContext)
        YamlConfigManager.initialize(appContext)
        
        // Sync YAML habits to database after YAML config is loaded
        YamlHabitSync.syncHabitsToDatabase(appContext)
        
        isInitialized = true
    }
    
    /**
     * Check if configuration has been initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Reset all configuration (useful for testing)
     */
    fun reset() {
        EnvironmentConfig.clearRuntimeConfig()
        DeviceConfig.clearWatchConfig()
        TxAgentConfig.clearConfig()
        isInitialized = false
    }
}
