package com.samsung.android.health.sdk.sample.healthdiary

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.openwearables.health.sdk.OpenWearablesHealthSDK
import com.openwearables.health.sdk.OWLogLevel
import com.samsung.android.health.sdk.sample.healthdiary.config.ConfigInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HealthDiaryApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Initialize all configuration modules early in app lifecycle
        // This ensures config is available for Services, BroadcastReceivers, etc.
        ConfigInitializer.initialize(this)
        
        // Initialize OpenWearables SDK
        initializeOpenWearablesSDK()
    }
    
    private fun initializeOpenWearablesSDK() {
        try {
            val sdk = OpenWearablesHealthSDK.initialize(this)
            
            // Configure log level based on build type
            sdk.logLevel = if (BuildConfig.DEBUG) OWLogLevel.DEBUG else OWLogLevel.NONE
            
            // Set up log listener
            sdk.logListener = { message ->
                Log.d("OpenWearablesSDK", message)
            }
            
            // Set up auth error listener
            sdk.authErrorListener = { statusCode, message ->
                Log.e("OpenWearablesSDK", "Auth error ($statusCode): $message")
            }
            
            // Configure with OpenWearables server host
            // Production: https://hh-openwearables.fly.dev
            // For local: http://10.0.2.2:8000 (emulator) or http://localhost:8000
            val owHost = "https://hh-openwearables.fly.dev"
            val shouldAutoRestore = sdk.configure(owHost)
            
            Log.i("HealthDiaryApp", "OpenWearables SDK initialized with host: $owHost (autoRestore=$shouldAutoRestore)")
        } catch (e: Exception) {
            Log.e("HealthDiaryApp", "Failed to initialize OpenWearables SDK", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
