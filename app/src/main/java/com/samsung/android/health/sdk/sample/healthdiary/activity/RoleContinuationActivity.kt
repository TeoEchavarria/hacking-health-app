package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.repository.PairingRepository
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import com.samsung.android.health.sdk.sample.healthdiary.views.ExistingLink
import com.samsung.android.health.sdk.sample.healthdiary.views.RoleContinuationScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for continuing with an existing role and link.
 * 
 * This screen is shown when:
 * - User already has a saved role (CAREGIVER or PATIENT)
 * - User has an active pairing/link with another person
 * 
 * Options:
 * - Continue with existing role → HealthMainActivity
 * - Choose new role → Clears current role and link, then RoleSelectionActivity
 */
class RoleContinuationActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "RoleContinuationActivity"
        
        // Intent extras
        const val EXTRA_LINKED_PERSON_NAME = "linked_person_name"
        const val EXTRA_PAIRING_ID = "pairing_id"
        const val EXTRA_ROLE = "role"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "RoleContinuationActivity started")
        
        // Get data from intent
        val linkedPersonName = intent.getStringExtra(EXTRA_LINKED_PERSON_NAME) ?: "Usuario"
        val pairingId = intent.getStringExtra(EXTRA_PAIRING_ID) ?: ""
        val roleValue = intent.getStringExtra(EXTRA_ROLE) ?: ""
        val role = UserRole.fromValue(roleValue) ?: UserRole.PATIENT
        
        Log.i(TAG, "Existing link: role=$role, linkedPerson=$linkedPersonName, pairingId=$pairingId")
        
        val existingLink = ExistingLink(
            role = role,
            linkedPersonName = linkedPersonName,
            pairingId = pairingId
        )
        
        setContent {
            SandboxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoleContinuationScreen(
                        existingLink = existingLink,
                        onContinueWithRole = { selectedRole ->
                            Log.i(TAG, "Continuing with role: ${selectedRole.displayName}")
                            handleContinueWithRole()
                        },
                        onChooseNewRole = {
                            Log.i(TAG, "User chose to select a new role")
                            handleChooseNewRole()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * User wants to continue with their existing role.
     * Navigate directly to HealthMainActivity.
     */
    private fun handleContinueWithRole() {
        val intent = Intent(this, HealthMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    /**
     * User wants to choose a new role.
     * Clear existing role and pairing, then navigate to RoleSelectionActivity.
     */
    private fun handleChooseNewRole() {
        // Get the pairing ID to clear from database
        val pairingId = intent.getStringExtra(EXTRA_PAIRING_ID) ?: ""
        
        // Launch coroutine to clear data
        val scope = kotlinx.coroutines.MainScope()
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Clear role selection
                    UserRole.clearRoleSelection(this@RoleContinuationActivity)
                    
                    // Revoke the pairing on backend first (if exists)
                    if (pairingId.isNotEmpty()) {
                        val repository = PairingRepository(this@RoleContinuationActivity)
                        val result = repository.revokePairing(pairingId)
                        
                        result.fold(
                            onSuccess = {
                                Log.i(TAG, "Pairing $pairingId revoked on backend")
                            },
                            onFailure = { error ->
                                // Log but continue - local update will still happen
                                Log.w(TAG, "Failed to revoke pairing on backend: ${error.message}")
                            }
                        )
                        
                        // Also update local database
                        val pairingDao = AppDatabase.getDatabase(this@RoleContinuationActivity).pairingDao()
                        pairingDao.updateStatus(
                            pairingId = pairingId,
                            status = "revoked"
                        )
                        Log.i(TAG, "Pairing $pairingId marked as revoked locally")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing role/pairing", e)
                }
            }
            
            // Navigate to RoleSelectionActivity
            withContext(Dispatchers.Main) {
                val intent = Intent(this@RoleContinuationActivity, RoleSelectionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}
