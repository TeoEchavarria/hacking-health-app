package com.samsung.android.health.sdk.sample.healthdiary.wearable.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.samsung.android.health.sdk.sample.healthdiary.activity.HealthMainActivity
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for watch onboarding/pairing flow.
 * 
 * This activity can be launched:
 * 1. After login (if onboarding not completed)
 * 2. From settings to re-configure watch connection
 * 
 * The activity allows users to:
 * - Detect connected Galaxy Watch
 * - Enable/disable health data sync
 * - Skip onboarding if desired
 */
@AndroidEntryPoint
class WatchOnboardingActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "WatchOnboardingActivity"
        const val EXTRA_FROM_SETTINGS = "from_settings"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val fromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        Log.i(TAG, "WatchOnboardingActivity started (fromSettings=$fromSettings)")
        
        setContent {
            SandboxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WatchOnboardingScreen(
                        onComplete = {
                            Log.i(TAG, "Onboarding completed")
                            navigateToMain()
                        },
                        onSkip = {
                            Log.i(TAG, "Onboarding skipped")
                            // Mark as completed even if skipped (so we don't show again)
                            val prefs = getSharedPreferences("watch_onboarding", MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("onboarding_completed", true)
                                .putBoolean("onboarding_skipped", true)
                                .apply()
                            navigateToMain()
                        }
                    )
                }
            }
        }
    }
    
    private fun navigateToMain() {
        val fromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        
        if (fromSettings) {
            // Just finish, return to where we came from
            finish()
        } else {
            // Navigate to main activity and clear back stack
            val intent = Intent(this, HealthMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
