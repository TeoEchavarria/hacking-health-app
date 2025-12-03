package com.samsung.android.health.sdk.sample.healthdiary.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConnectionLog(
    val timestamp: Long,
    val type: LogType,
    val tag: String,
    val message: String
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

enum class LogType {
    INFO, ERROR, SUCCESS, TRAFFIC
}

object ConnectionLogManager {
    private val _logs = MutableStateFlow<List<ConnectionLog>>(emptyList())
    val logs: StateFlow<List<ConnectionLog>> = _logs.asStateFlow()

    fun log(type: LogType, tag: String, message: String) {
        val newLog = ConnectionLog(System.currentTimeMillis(), type, tag, message)
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, newLog) // Add to top
        if (currentLogs.size > 1000) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _logs.value = currentLogs
        
        // Also log to system logcat for persistence/filtering
        when (type) {
            LogType.ERROR -> android.util.Log.e(tag, message)
            else -> android.util.Log.d(tag, message)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
