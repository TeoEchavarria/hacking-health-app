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
import com.samsung.android.health.sdk.sample.healthdiary.views.CaregiverPairingScreen

/**
 * Activity for caregiver pairing flow.
 * 
 * Caregiver enters 6-digit code from patient's device to link accounts.
 * On success, navigates to main health dashboard.
 */
class CaregiverPairingActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "CaregiverPairingActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "CaregiverPairingActivity started")
        
        setContent {
            SandboxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CaregiverPairingScreen(
                        onPairingSuccess = { pairingId, patientId, patientName ->
                            Log.i(TAG, "Pairing successful - ID: $pairingId, Patient: $patientName ($patientId)")
                            handlePairingSuccess(pairingId, patientId, patientName)
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
     * Handle successful pairing and navigate to main app.
     */
    private fun handlePairingSuccess(pairingId: String, patientId: String, patientName: String) {
        // TODO: Save pairing info to local preferences if needed
        
        // Navigate to main health dashboard
        val intent = Intent(this, HealthMainActivity::class.java).apply {
            // Clear back stack
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            // Pass patient info
            putExtra("PAIRED_PATIENT_ID", patientId)
            putExtra("PAIRED_PATIENT_NAME", patientName)
            putExtra("PAIRING_ID", pairingId)
        }
        
        startActivity(intent)
        finish()
    }
}
