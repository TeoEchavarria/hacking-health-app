package com.samsung.android.health.sdk.sample.healthdiary

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
