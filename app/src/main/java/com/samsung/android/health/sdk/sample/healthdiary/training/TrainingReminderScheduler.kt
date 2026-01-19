package com.samsung.android.health.sdk.sample.healthdiary.training

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import com.google.gson.Gson
import java.io.File

/**
 * Schedules daily reminders for training blocks
 */
class TrainingReminderScheduler(private val context: Context) {
    
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    companion object {
        private const val REQUEST_CODE_BASE = 1000
        private const val ACTION_REMINDER = "com.samsung.android.health.sdk.sample.healthdiary.training.REMINDER"
    }

    private val debugGson = Gson()

    // #region agent log
    private fun debugLog(hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
        try {
            val payload = mapOf(
                "sessionId" to "debug-session",
                "runId" to "pre-fix",
                "hypothesisId" to hypothesisId,
                "location" to "TrainingReminderScheduler.kt",
                "message" to message,
                "data" to data,
                "timestamp" to System.currentTimeMillis()
            )
            File("/Users/teoechavarria/Documents/hh/.cursor/debug.log")
                .appendText(debugGson.toJson(payload) + "\n")
        } catch (_: Exception) {
            // best-effort logging only
        }
    }
    // #endregion
    
    /**
     * Schedule all reminders for today and future days
     */
    fun scheduleAllReminders() {
        val stateManager = TrainingStateManager(context)
        val reminderTimes = stateManager.getReminderTimes()
        // #region agent log
        debugLog("H4", "scheduleAllReminders start", mapOf("count" to reminderTimes.size))
        // #endregion
        reminderTimes.forEach { (blockType, timeString) ->
            scheduleReminder(blockType, timeString)
        }
    }
    
    /**
     * Schedule a reminder for a specific block
     */
    fun scheduleReminder(blockType: BlockType, timeString: String) {
        val (hour, minute) = parseTime(timeString)
        // #region agent log
        debugLog("H4", "scheduleReminder parsed time", mapOf("block" to blockType.name, "time" to timeString, "hour" to hour, "minute" to minute))
        // #endregion
        
        // Cancel existing reminder
        cancelReminder(blockType)
        
        // Create intent
        val intent = Intent(context, TrainingReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra("block_type", blockType.name)
        }
        
        val requestCode = REQUEST_CODE_BASE + blockType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Set calendar for today
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If time has passed today, schedule for tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        // #region agent log
        debugLog("H4", "scheduleReminder computed time", mapOf("block" to blockType.name, "triggerAt" to calendar.timeInMillis, "now" to System.currentTimeMillis(), "sdk" to Build.VERSION.SDK_INT))
        // #endregion
        
        // Schedule alarm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fall back to inexact scheduling when exact alarms are not permitted.
                    debugLog("H4", "Exact alarm not permitted, using inexact", mapOf("block" to blockType.name))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            // #region agent log
            debugLog("H4", "scheduleReminder success", mapOf("block" to blockType.name))
            // #endregion
        } catch (e: SecurityException) {
            // Avoid crashing the app if exact alarm permission is missing.
            debugLog("H4", "scheduleReminder security exception", mapOf("block" to blockType.name, "error" to e.javaClass.simpleName))
        } catch (e: Exception) {
            // #region agent log
            debugLog("H4", "scheduleReminder failed", mapOf("block" to blockType.name, "error" to e.javaClass.simpleName))
            // #endregion
        }
    }
    
    /**
     * Cancel a specific reminder
     */
    fun cancelReminder(blockType: BlockType) {
        val intent = Intent(context, TrainingReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra("block_type", blockType.name)
        }
        
        val requestCode = REQUEST_CODE_BASE + blockType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Cancel all reminders
     */
    fun cancelAllReminders() {
        BlockType.values().forEach { cancelReminder(it) }
    }
    
    /**
     * Reschedule all reminders (call when reminder times change)
     */
    fun rescheduleAllReminders() {
        cancelAllReminders()
        scheduleAllReminders()
    }
    
    private fun parseTime(timeString: String): Pair<Int, Int> {
        val parts = timeString.split(":")
        return if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1].toIntOrNull() ?: 0
            Pair(hour, minute)
        } else {
            Pair(0, 0)
        }
    }
}
