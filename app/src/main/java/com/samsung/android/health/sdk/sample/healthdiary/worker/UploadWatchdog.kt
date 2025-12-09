package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Watchdog that monitors upload health and triggers recovery when stalls are detected.
 * 
 * This runs independently of WorkManager to ensure the upload pipeline never silently dies.
 * It periodically checks if uploads are stalled and forces recovery if needed.
 */
object UploadWatchdog {
    private const val TAG = "UploadWatchdog"
    
    // Check interval: how often to check for stalls
    private const val CHECK_INTERVAL_MS = 60_000L // 1 minute
    
    // Stall threshold: if no upload attempt in this time, consider stalled
    private const val STALL_THRESHOLD_MS = 5 * 60_000L // 5 minutes
    
    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var context: Context? = null
    
    /**
     * Start the watchdog. Should be called once at app startup.
     * Safe to call multiple times - will not create duplicate jobs.
     */
    fun start(appContext: Context) {
        if (watchdogJob?.isActive == true) {
            Log.d(TAG, "Watchdog already running")
            return
        }
        
        context = appContext.applicationContext
        UploadHealthMonitor.initialize(appContext)
        
        watchdogJob = scope.launch {
            Log.d(TAG, "WATCHDOG_STARTED")
            
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                checkHealth()
            }
        }
    }
    
    /**
     * Stop the watchdog.
     */
    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
        Log.d(TAG, "WATCHDOG_STOPPED")
    }
    
    private fun checkHealth() {
        val ctx = context ?: return
        val state = UploadHealthMonitor.healthState.value
        val now = System.currentTimeMillis()
        
        // Log current health status
        val timeSinceLastAttempt = if (state.lastAttemptTime > 0) {
            (now - state.lastAttemptTime) / 1000
        } else {
            -1
        }
        val timeSinceLastSuccess = if (state.lastSuccessTime > 0) {
            (now - state.lastSuccessTime) / 1000
        } else {
            -1
        }
        
        Log.d(TAG, "WATCHDOG_CHECK: pending=${state.pendingRecordCount}, " +
                "lastAttempt=${timeSinceLastAttempt}s ago, " +
                "lastSuccess=${timeSinceLastSuccess}s ago, " +
                "failures=${state.consecutiveFailures}")
        
        // Check for stall condition
        if (isStalled(state, now)) {
            Log.w(TAG, "WATCHDOG_STALL_DETECTED: Triggering recovery")
            triggerRecovery(ctx)
        }
        
        // Check for circuit breaker condition
        if (UploadHealthMonitor.isCircuitOpen()) {
            Log.e(TAG, "WATCHDOG_CIRCUIT_OPEN: ${state.consecutiveFailures} consecutive failures")
            // Log diagnostic snapshot but don't reset circuit automatically
            // This requires manual intervention or app restart
            logDiagnosticSnapshot(state)
        }
    }
    
    private fun isStalled(state: UploadHealthState, now: Long): Boolean {
        // No stall if no pending records
        if (state.pendingRecordCount <= 0) {
            return false
        }
        
        // No stall if we've never attempted (fresh start)
        if (state.lastAttemptTime == 0L) {
            return false
        }
        
        // Stalled if too long since last attempt
        val timeSinceLastAttempt = now - state.lastAttemptTime
        return timeSinceLastAttempt > STALL_THRESHOLD_MS
    }
    
    private fun triggerRecovery(ctx: Context) {
        Log.w(TAG, "WATCHDOG_RECOVERY: Forcing immediate upload")
        
        // Force an immediate upload attempt
        UploadScheduler.forceImmediateUpload(ctx)
        
        // Also ensure the periodic safety net is active
        UploadScheduler.ensurePeriodicSafetyNet(ctx)
    }
    
    private fun logDiagnosticSnapshot(state: UploadHealthState) {
        Log.e(TAG, "=== DIAGNOSTIC SNAPSHOT ===")
        Log.e(TAG, "Pending records: ${state.pendingRecordCount}")
        Log.e(TAG, "Total sent: ${state.totalRecordsSent}")
        Log.e(TAG, "Consecutive failures: ${state.consecutiveFailures}")
        Log.e(TAG, "Last failure reason: ${state.lastFailureReason}")
        Log.e(TAG, "Last attempt: ${state.lastAttemptTime}")
        Log.e(TAG, "Last success: ${state.lastSuccessTime}")
        Log.e(TAG, "Last failure: ${state.lastFailureTime}")
        Log.e(TAG, "===========================")
    }
    
    /**
     * Manually trigger a health check and recovery if needed.
     * Useful for debugging or manual intervention.
     */
    fun forceCheck() {
        checkHealth()
    }
}
