package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import com.samsung.android.health.sdk.sample.healthdiary.views.RoleSelectionScreen
import com.samsung.android.health.sdk.sample.healthdiary.wearable.ui.WatchOnboardingActivity

/**
 * Activity for user role selection during onboarding.
 * 
 * Users choose between:
 * - CAREGIVER: Monitors health of family members → navigates to FamilyLinkActivity
 * - PATIENT: Person being monitored → navigates to WatchOnboardingActivity
 * 
 * This screen is shown once after login, before any other onboarding steps.
 */
class RoleSelectionActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "RoleSelectionActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "RoleSelectionActivity started")
        
        setContent {
            SandboxTheme {
                var showHelpDialog by remember { mutableStateOf(false) }
                
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoleSelectionScreen(
                        onRoleSelected = { role ->
                            Log.i(TAG, "Role selected: ${role.displayName} (${role.value})")
                            handleRoleSelection(role)
                        },
                        onHelp = {
                            showHelpDialog = true
                        }
                    )
                }
                
                // Help Dialog
                if (showHelpDialog) {
                    HelpDialog(onDismiss = { showHelpDialog = false })
                }
            }
        }
    }
    
    /**
     * Handle role selection and navigate to appropriate screen.
     * - CAREGIVER → CaregiverPairingActivity (enter patient code)
     * - PATIENT → PatientPairingActivity (generate code for caregiver)
     */
    private fun handleRoleSelection(role: UserRole) {
        // Save role to SharedPreferences
        UserRole.saveToPreferences(this, role)
        
        // Navigate based on role
        val targetActivity = when (role) {
            UserRole.CAREGIVER -> {
                Log.i(TAG, "Navigating to CaregiverPairingActivity (caregiver flow)")
                CaregiverPairingActivity::class.java
            }
            UserRole.PATIENT -> {
                Log.i(TAG, "Navigating to PatientPairingActivity (patient flow)")
                PatientPairingActivity::class.java
            }
        }
        
        val intent = Intent(this, targetActivity).apply {
            // Clear back stack to prevent going back to role selection
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

/**
 * Simple help dialog with basic information about role selection.
 */
@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Selección de Rol")
        },
        text = {
            Text(
                "Elige tu rol según tu situación:\n\n" +
                "• Cuidador: Si vas a monitorear la salud de un familiar o ser querido.\n\n" +
                "• Persona a cuidar: Si tú serás monitoreado/a y compartirás tus datos de salud.\n\n" +
                "Esta configuración determina cómo se conectan los dispositivos y se comparten los datos."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}
