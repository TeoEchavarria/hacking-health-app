package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.app.Activity
import android.util.Log
import com.openwearables.health.sdk.OpenWearablesHealthSDK
import com.openwearables.health.sdk.Outcome
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connection state for OpenWearables
 */
sealed class OpenWearablesState {
    object NotInitialized : OpenWearablesState()
    object Disconnected : OpenWearablesState()
    object Connecting : OpenWearablesState()
    data class Connected(val provider: String) : OpenWearablesState()
    object Syncing : OpenWearablesState()
    data class Error(val message: String) : OpenWearablesState()
}

/**
 * Provider info from SDK
 */
data class HealthProviderInfo(
    val id: String,
    val displayName: String,
    val isAvailable: Boolean
)

/**
 * Repository for OpenWearables SDK operations.
 * Provides a clean interface for health data providers (Samsung Health, Health Connect).
 */
@Singleton
class OpenWearablesRepository @Inject constructor() {
    
    companion object {
        private const val TAG = "OpenWearablesRepo"
    }
    
    private val _connectionState = MutableStateFlow<OpenWearablesState>(OpenWearablesState.NotInitialized)
    val connectionState: StateFlow<OpenWearablesState> = _connectionState.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val syncStatus: StateFlow<Map<String, Any?>> = _syncStatus.asStateFlow()
    
    private var sdk: OpenWearablesHealthSDK? = null
    
    /**
     * Initialize the repository with SDK instance.
     * Automatically signs in if credentials are available from login.
     */
    fun initialize() {
        try {
            sdk = OpenWearablesHealthSDK.getInstance()
            _connectionState.value = OpenWearablesState.Disconnected
            Log.d(TAG, "Repository initialized with SDK")
            
            // Auto-login if we have stored credentials from the login flow
            autoSignInIfCredentialsAvailable()
        } catch (e: Exception) {
            Log.e(TAG, "SDK not initialized: ${e.message}")
            _connectionState.value = OpenWearablesState.NotInitialized
        }
    }
    
    /**
     * Automatically sign in to OpenWearables if credentials are stored.
     * Called during initialization to provide seamless experience.
     */
    private fun autoSignInIfCredentialsAvailable() {
        if (!TokenManager.hasOpenWearablesCredentials()) {
            Log.d(TAG, "No OpenWearables credentials stored, skipping auto-login")
            return
        }
        
        val userId = TokenManager.getOpenWearablesUserId() ?: return
        val accessToken = TokenManager.getOpenWearablesAccessToken() ?: return
        val refreshToken = TokenManager.getOpenWearablesRefreshToken()
        
        try {
            sdk?.signIn(userId, accessToken, refreshToken, null)
            Log.i(TAG, "Auto-signed in to OpenWearables as user: $userId")
            _connectionState.value = OpenWearablesState.Disconnected // Ready to connect
        } catch (e: Exception) {
            Log.w(TAG, "Auto-sign in failed: ${e.message}")
            // Clear invalid credentials
            TokenManager.clearOpenWearablesCredentials()
        }
    }
    
    /**
     * Set the current activity for permission requests
     */
    fun setActivity(activity: Activity?) {
        sdk?.setActivity(activity)
    }
    
    /**
     * Get list of available health data providers
     */
    fun getAvailableProviders(): List<HealthProviderInfo> {
        return sdk?.getAvailableProviders()?.map { provider ->
            HealthProviderInfo(
                id = provider["id"] as? String ?: "",
                displayName = provider["displayName"] as? String ?: "",
                isAvailable = provider["isAvailable"] as? Boolean ?: false
            )
        } ?: emptyList()
    }
    
    /**
     * Select a health data provider
     */
    fun setProvider(providerId: String): Boolean {
        return sdk?.setProvider(providerId) ?: false
    }
    
    /**
     * Sign in to OpenWearables backend
     */
    suspend fun signIn(
        userId: String,
        accessToken: String,
        refreshToken: String? = null
    ): Result<Unit> {
        val sdkInstance = sdk ?: return Result.failure(IllegalStateException("SDK not initialized"))
        
        return try {
            _connectionState.value = OpenWearablesState.Connecting
            sdkInstance.signIn(userId, accessToken, refreshToken, null)
            Log.d(TAG, "Signed in to OpenWearables")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}")
            _connectionState.value = OpenWearablesState.Error(e.message ?: "Sign in failed")
            Result.failure(e)
        }
    }
    
    /**
     * Sign out from OpenWearables
     */
    suspend fun signOut() {
        try {
            sdk?.signOut()
            _connectionState.value = OpenWearablesState.Disconnected
            Log.d(TAG, "Signed out from OpenWearables")
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
        }
    }
    
    /**
     * Request authorization for health data types
     */
    suspend fun requestAuthorization(types: List<String>): Result<Boolean> {
        val sdkInstance = sdk ?: return Result.failure(IllegalStateException("SDK not initialized"))
        
        return try {
            val outcome = sdkInstance.requestAuthorization(types)
            when (outcome) {
                is Outcome.Success -> {
                    Log.d(TAG, "Authorization granted for types: $types")
                    Result.success(outcome.value)
                }
                is Outcome.Error -> {
                    Log.e(TAG, "Authorization failed: ${outcome.message}")
                    Result.failure(outcome.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authorization request error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if authorization is granted for health data types
     */
    suspend fun checkAuthorization(types: List<String>): Result<Boolean> {
        val sdkInstance = sdk ?: return Result.failure(IllegalStateException("SDK not initialized"))
        
        return try {
            val outcome = sdkInstance.checkAuthorization(types)
            when (outcome) {
                is Outcome.Success -> Result.success(outcome.value)
                is Outcome.Error -> Result.failure(outcome.exception)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Start background sync with the OpenWearables backend
     */
    suspend fun startBackgroundSync(syncDaysBack: Int? = null): Result<Unit> {
        val sdkInstance = sdk ?: return Result.failure(IllegalStateException("SDK not initialized"))
        
        return try {
            _connectionState.value = OpenWearablesState.Syncing
            sdkInstance.startBackgroundSync(syncDaysBack)
            
            // Update state based on current provider
            val providers = getAvailableProviders()
            val activeProvider = providers.find { it.isAvailable }?.displayName ?: "Unknown"
            _connectionState.value = OpenWearablesState.Connected(activeProvider)
            
            Log.d(TAG, "Background sync started")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sync: ${e.message}")
            _connectionState.value = OpenWearablesState.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }
    
    /**
     * Stop background sync
     */
    suspend fun stopBackgroundSync(): Result<Unit> {
        val sdkInstance = sdk ?: return Result.failure(IllegalStateException("SDK not initialized"))
        
        return try {
            sdkInstance.stopBackgroundSync()
            _connectionState.value = OpenWearablesState.Disconnected
            Log.d(TAG, "Background sync stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop sync: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Trigger an immediate sync
     */
    suspend fun syncNow(): Result<Unit> {
        val sdkInstance = sdk ?: return Result.failure(IllegalStateException("SDK not initialized"))
        
        return try {
            sdkInstance.syncNow()
            updateSyncStatus()
            Log.d(TAG, "Sync completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get current sync status
     */
    fun updateSyncStatus() {
        sdk?.let { sdkInstance ->
            _syncStatus.value = sdkInstance.getSyncStatus()
        }
    }
    
    /**
     * Read latest health data
     */
    suspend fun readLatestData(types: List<String>): Result<Map<String, Any?>> {
        val sdkInstance = sdk ?: return Result.failure(IllegalStateException("SDK not initialized"))
        
        return try {
            val outcome = sdkInstance.readLatestData(types)
            when (outcome) {
                is Outcome.Success -> {
                    Log.d(TAG, "Read latest data: ${outcome.value}")
                    Result.success(outcome.value)
                }
                is Outcome.Error -> {
                    Log.e(TAG, "Read data failed: ${outcome.message}")
                    Result.failure(outcome.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read data error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Reset sync anchors (triggers full re-sync)
     */
    fun resetSyncAnchors() {
        sdk?.resetAnchors()
        Log.d(TAG, "Sync anchors reset")
    }
    
    /**
     * Notify SDK of foreground state
     */
    fun onForeground() {
        sdk?.onForeground()
    }
    
    /**
     * Notify SDK of background state
     */
    fun onBackground() {
        sdk?.onBackground()
    }
    
    /**
     * Check if there's a resumable sync session
     */
    fun hasResumableSyncSession(): Boolean {
        return sdk?.hasResumableSyncSession() ?: false
    }
    
    /**
     * Resume interrupted sync
     */
    suspend fun resumeSync(): Result<Unit> {
        val sdkInstance = sdk ?: return Result.failure(IllegalStateException("SDK not initialized"))
        
        return try {
            sdkInstance.resumeSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
