package com.samsung.android.health.sdk.sample.healthdiary.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Test logging utility compatible with debug_logs.sh format.
 * 
 * Logs are structured as:
 * [TAG] <component>: <message>
 * 
 * Tags:
 * - TEST_EXPECT: Expected behavior/values
 * - TEST_ACTUAL: Actual behavior/values observed
 * - TEST_RESULT: Final test result (PASS/FAIL)
 * - TEST_PHONE: Phone-specific test logs
 */
object TestLogger {
    private const val TAG_RESULT = "TEST_RESULT"
    private const val TAG_EXPECT = "TEST_EXPECT"
    private const val TAG_ACTUAL = "TEST_ACTUAL"
    private const val TAG_PHONE = "TEST_PHONE"
    
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    
    /**
     * Log expected behavior or value for a test
     */
    fun expectation(testName: String, expectation: String) {
        val timestamp = timeFormat.format(Date())
        Log.i(TAG_EXPECT, "[$timestamp][$testName] Expected: $expectation")
    }
    
    /**
     * Log actual behavior or value observed
     */
    fun actual(testName: String, actual: String) {
        val timestamp = timeFormat.format(Date())
        Log.i(TAG_ACTUAL, "[$timestamp][$testName] Actual: $actual")
    }
    
    /**
     * Log successful test result with summary statistics
     */
    fun pass(testName: String, summary: String = "") {
        val timestamp = timeFormat.format(Date())
        val summaryText = if (summary.isNotEmpty()) " - $summary" else ""
        Log.i(TAG_RESULT, "[$timestamp][$testName]: PASS$summaryText")
    }
    
    /**
     * Log failed test result with reason
     */
    fun fail(testName: String, reason: String) {
        val timestamp = timeFormat.format(Date())
        Log.e(TAG_RESULT, "[$timestamp][$testName]: FAIL - $reason")
    }
    
    /**
     * Log mismatch between expected and actual values
     */
    fun mismatch(testName: String, expected: String, actual: String) {
        val timestamp = timeFormat.format(Date())
        Log.e(TAG_RESULT, "[$timestamp][$testName]: MISMATCH - Expected: $expected, Actual: $actual")
        fail(testName, "Value mismatch")
    }
    
    /**
     * Log phone-specific test information
     */
    fun phone(component: String, message: String) {
        val timestamp = timeFormat.format(Date())
        Log.d(TAG_PHONE, "[$timestamp][$component]: $message")
    }
    
    /**
     * Log test start
     */
    fun start(testName: String) {
        val timestamp = timeFormat.format(Date())
        Log.i(TAG_PHONE, "[$timestamp][$testName]: TEST STARTED")
    }
    
    /**
     * Log test cleanup
     */
    fun cleanup(testName: String) {
        val timestamp = timeFormat.format(Date())
        Log.d(TAG_PHONE, "[$timestamp][$testName]: Cleanup complete")
    }
    
    /**
     * Measure and log latency
     */
    inline fun <T> measureLatency(testName: String, operation: String, block: () -> T): Pair<T, Long> {
        val start = System.currentTimeMillis()
        val result = block()
        val latency = System.currentTimeMillis() - start
        phone("$testName.$operation", "latency=${latency}ms")
        return Pair(result, latency)
    }
}
