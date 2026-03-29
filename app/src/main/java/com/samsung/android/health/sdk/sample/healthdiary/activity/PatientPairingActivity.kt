package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import com.samsung.android.health.sdk.sample.healthdiary.views.PatientPairingScreen
import com.samsung.android.health.sdk.sample.healthdiary.wearable.ui.WatchOnboardingActivity

/**
 * Activity for patient pairing flow.
 * 
 * Patient generates and displays 6-digit code for caregiver to enter.
 * Polls backend every 3 seconds to detect when caregiver uses the code.
 * On success, navigates to main health dashboard.
 */
class PatientPairingActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "PatientPairingActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "PatientPairingActivity started")
        
        setContent {
            SandboxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PatientPairingScreen(
                        onPairingSuccess = { pairingId, caregiverId, caregiverName ->
                            Log.i(TAG, "Pairing successful - ID: $pairingId, Caregiver: $caregiverName ($caregiverId)")
                            handlePairingSuccess(pairingId, caregiverId, caregiverName)
                        },
                        onBack = {
                            Log.i(TAG, "User cancelled pairing")
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Handle successful pairing and navigate to watch onboarding.
     * After pairing with caregiver, patient needs to connect Galaxy Watch.
     */
    private fun handlePairingSuccess(pairingId: String, caregiverId: String, caregiverName: String) {
        // TODO: Save pairing info to local preferences if needed
        
        // Navigate to watch onboarding (patient needs to connect Galaxy Watch)
        val intent = Intent(this, WatchOnboardingActivity::class.java).apply {
            // Clear back stack
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            // Pass caregiver info
            putExtra("PAIRED_CAREGIVER_ID", caregiverId)
            putExtra("PAIRED_CAREGIVER_NAME", caregiverName)
            putExtra("PAIRING_ID", pairingId)
        }
        
        startActivity(intent)
        finish()
    }
}
