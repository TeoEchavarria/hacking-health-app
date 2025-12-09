package com.samsung.android.health.sdk.sample.healthdiary.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that tracks upload health metrics for diagnostics and watchdog functionality.
 * Persists critical timestamps to SharedPreferences to survive process death.
 */
object UploadHealthMonitor {
    private const val TAG = "UploadHealthMonitor"
    private const val PREFS_NAME = "upload_health_prefs"
    private const val KEY_LAST_ATTEMPT_TIME = "last_attempt_time"
    private const val KEY_LAST_SUCCESS_TIME = "last_success_time"
    private const val KEY_LAST_FAILURE_TIME = "last_failure_time"
    private const val KEY_CONSECUTIVE_FAILURES = "consecutive_failures"
    private const val KEY_LAST_SCHEDULED_TIME = "last_scheduled_time"
    private const val KEY_TOTAL_RECORDS_SENT = "total_records_sent"
    
    // Stall detection threshold: if no attempt in this duration, consider stalled
    const val STALL_THRESHOLD_MS = 5 * 60 * 1000L // 5 minutes
    
    // Circuit breaker threshold
    const val MAX_CONSECUTIVE_FAILURES = 10
    
    private var prefs: SharedPreferences? = null
    
    // Observable state for UI
    private val _healthState = MutableStateFlow(UploadHealthState())
    val healthState: StateFlow<UploadHealthState> = _healthState.asStateFlow()
    
    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadState()
        }
    }
    
    private fun loadState() {
        prefs?.let { p ->
            _healthState.value = UploadHealthState(
                lastAttemptTime = p.getLong(KEY_LAST_ATTEMPT_TIME, 0),
                lastSuccessTime = p.getLong(KEY_LAST_SUCCESS_TIME, 0),
                lastFailureTime = p.getLong(KEY_LAST_FAILURE_TIME, 0),
                consecutiveFailures = p.getInt(KEY_CONSECUTIVE_FAILURES, 0),
                lastScheduledTime = p.getLong(KEY_LAST_SCHEDULED_TIME, 0),
                totalRecordsSent = p.getLong(KEY_TOTAL_RECORDS_SENT, 0)
            )
        }
    }
    
    fun recordAttempt() {
        val now = System.currentTimeMillis()
        updateState { it.copy(lastAttemptTime = now) }
        prefs?.edit()?.putLong(KEY_LAST_ATTEMPT_TIME, now)?.apply()
        Log.d(TAG, "UPLOAD_ATTEMPT at $now")
    }
    
    fun recordSuccess(recordCount: Int) {
        val now = System.currentTimeMillis()
        updateState { 
            it.copy(
                lastSuccessTime = now,
                consecutiveFailures = 0,
                totalRecordsSent = it.totalRecordsSent + recordCount
            )
        }
        prefs?.edit()
            ?.putLong(KEY_LAST_SUCCESS_TIME, now)
            ?.putInt(KEY_CONSECUTIVE_FAILURES, 0)
            ?.putLong(KEY_TOTAL_RECORDS_SENT, _healthState.value.totalRecordsSent)
            ?.apply()
        Log.d(TAG, "UPLOAD_SUCCESS: $recordCount records at $now")
    }
    
    fun recordFailure(reason: String) {
        val now = System.currentTimeMillis()
        val newFailureCount = _healthState.value.consecutiveFailures + 1
        updateState { 
            it.copy(
                lastFailureTime = now,
                consecutiveFailures = newFailureCount,
                lastFailureReason = reason
            )
        }
        prefs?.edit()
            ?.putLong(KEY_LAST_FAILURE_TIME, now)
            ?.putInt(KEY_CONSECUTIVE_FAILURES, newFailureCount)
            ?.apply()
        Log.e(TAG, "UPLOAD_FAILURE #$newFailureCount: $reason at $now")
    }
    
    fun recordScheduled() {
        val now = System.currentTimeMillis()
        updateState { it.copy(lastScheduledTime = now) }
        prefs?.edit()?.putLong(KEY_LAST_SCHEDULED_TIME, now)?.apply()
        Log.d(TAG, "UPLOAD_SCHEDULED at $now")
    }
    
    fun updatePendingCount(count: Int) {
        updateState { it.copy(pendingRecordCount = count) }
    }
    
    /**
     * Check if the upload pipeline appears stalled.
     * Returns true if:
     * - There are pending records AND
     * - No attempt has been made in STALL_THRESHOLD_MS
     */
    fun isStalled(): Boolean {
        val state = _healthState.value
        val now = System.currentTimeMillis()
        val timeSinceLastAttempt = now - state.lastAttemptTime
        
        val stalled = state.pendingRecordCount > 0 && 
                      state.lastAttemptTime > 0 && 
                      timeSinceLastAttempt > STALL_THRESHOLD_MS
        
        if (stalled) {
            Log.w(TAG, "UPLOAD_STALLED: ${timeSinceLastAttempt}ms since last attempt, ${state.pendingRecordCount} pending")
        }
        return stalled
    }
    
    /**
     * Check if circuit breaker should trip (too many consecutive failures)
     */
    fun isCircuitOpen(): Boolean {
        return _healthState.value.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES
    }
    
    fun resetCircuitBreaker() {
        updateState { it.copy(consecutiveFailures = 0) }
        prefs?.edit()?.putInt(KEY_CONSECUTIVE_FAILURES, 0)?.apply()
        Log.d(TAG, "Circuit breaker reset")
    }
    
    fun getHealthSummary(): String {
        val state = _healthState.value
        val now = System.currentTimeMillis()
        return buildString {
            appendLine("=== Upload Health Summary ===")
            appendLine("Pending records: ${state.pendingRecordCount}")
            appendLine("Total sent: ${state.totalRecordsSent}")
            appendLine("Consecutive failures: ${state.consecutiveFailures}")
            if (state.lastAttemptTime > 0) {
                appendLine("Last attempt: ${(now - state.lastAttemptTime) / 1000}s ago")
            }
            if (state.lastSuccessTime > 0) {
                appendLine("Last success: ${(now - state.lastSuccessTime) / 1000}s ago")
            }
            if (state.lastFailureTime > 0) {
                appendLine("Last failure: ${(now - state.lastFailureTime) / 1000}s ago")
                appendLine("Failure reason: ${state.lastFailureReason}")
            }
            appendLine("Stalled: ${isStalled()}")
            appendLine("Circuit open: ${isCircuitOpen()}")
        }
    }
    
    private inline fun updateState(transform: (UploadHealthState) -> UploadHealthState) {
        _healthState.value = transform(_healthState.value)
    }
}

data class UploadHealthState(
    val lastAttemptTime: Long = 0,
    val lastSuccessTime: Long = 0,
    val lastFailureTime: Long = 0,
    val consecutiveFailures: Int = 0,
    val lastScheduledTime: Long = 0,
    val totalRecordsSent: Long = 0,
    val pendingRecordCount: Int = 0,
    val lastFailureReason: String = ""
)
