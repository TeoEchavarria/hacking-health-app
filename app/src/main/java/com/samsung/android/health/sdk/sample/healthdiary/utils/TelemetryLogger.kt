package com.samsung.android.health.sdk.sample.healthdiary.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val source: String, // WATCH, PHONE, API
    val event: String,
    val details: String
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

object TelemetryLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(source: String, event: String, details: String) {
        val newEntry = LogEntry(source = source, event = event, details = details)
        val currentList = _logs.value.toMutableList()
        currentList.add(0, newEntry) // Add to top
        // Keep only last 100 logs to avoid memory issues
        if (currentList.size > 100) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logs.value = currentList
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
