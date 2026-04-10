package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.repository.PairingRepository
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import com.samsung.android.health.sdk.sample.healthdiary.views.RoleSelectionScreen
import com.samsung.android.health.sdk.sample.healthdiary.wearable.ui.WatchOnboardingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                var isCheckingLinks by remember { mutableStateOf(true) }
                
                // Check for existing links on startup
                LaunchedEffect(Unit) {
                    checkForExistingLinks()
                    isCheckingLinks = false
                }
                
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isCheckingLinks) {
                        // Show loading while checking for existing links
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
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
                }
                
                // Help Dialog
                if (showHelpDialog) {
                    HelpDialog(onDismiss = { showHelpDialog = false })
                }
            }
        }
    }
    
    /**
     * Check if user has existing active links or if role was previously selected.
     * First syncs pairings from backend to ensure local DB is up-to-date.
     * Routes appropriately:
     * - Active links exist → RoleContinuationActivity ("Welcome back" screen)
     * - No active links but role selected → WatchOnboarding or HealthMain
     * - No active links and no role → show RoleSelectionScreen
     */
    private suspend fun checkForExistingLinks() {
        withContext(Dispatchers.IO) {
            try {
                // First, sync pairings from backend to ensure local DB is up-to-date
                // This handles cases where app was reinstalled or data was cleared
                val repository = PairingRepository(this@RoleSelectionActivity)
                val syncResult = repository.syncPairingsFromBackend()
                syncResult.fold(
                    onSuccess = { count ->
                        Log.i(TAG, "Synced $count pairings from backend")
                    },
                    onFailure = { error ->
                        // Continue even if sync fails (user might be offline)
                        // We'll still check local database
                        Log.w(TAG, "Failed to sync pairings from backend: ${error.message}")
                    }
                )
                
                val pairingDao = AppDatabase.getDatabase(this@RoleSelectionActivity).pairingDao()
                val activeLinks = pairingDao.getAllActiveSync()
                
                if (activeLinks.isNotEmpty()) {
                    // User has active pairings - show RoleContinuationActivity
                    val link = activeLinks.first()
                    Log.i(TAG, "Found existing link: pairingId=${link.pairingId}, role=${link.userRole}")
                    
                    // Determine the linked person's name based on role
                    val linkedPersonName = when (link.userRole) {
                        "caregiver" -> link.patientName ?: "Usuario"
                        "patient" -> link.caregiverName ?: "Usuario"
                        else -> "Usuario"
                    }
                    
                    val role = UserRole.fromValue(link.userRole ?: "patient") ?: UserRole.PATIENT
                    
                    // Navigate to RoleContinuationActivity
                    withContext(Dispatchers.Main) {
                        val intent = Intent(this@RoleSelectionActivity, RoleContinuationActivity::class.java).apply {
                            putExtra(RoleContinuationActivity.EXTRA_LINKED_PERSON_NAME, linkedPersonName)
                            putExtra(RoleContinuationActivity.EXTRA_PAIRING_ID, link.pairingId)
                            putExtra(RoleContinuationActivity.EXTRA_ROLE, link.userRole)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                } else if (UserRole.isRoleSelectionComplete(this@RoleSelectionActivity)) {
                    // No active links but role was previously selected - skip to next step
                    Log.i(TAG, "No active links but role already selected, skipping role selection")
                    
                    withContext(Dispatchers.Main) {
                        val targetActivity = if (com.samsung.android.health.sdk.sample.healthdiary.wearable.ui.isWatchOnboardingComplete(this@RoleSelectionActivity)) {
                            HealthMainActivity::class.java
                        } else {
                            WatchOnboardingActivity::class.java
                        }
                        
                        val intent = Intent(this@RoleSelectionActivity, targetActivity).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Log.i(TAG, "No existing links and no role selected, showing role selection")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for existing links", e)
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
