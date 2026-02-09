package com.samsung.android.health.sdk.sample.healthdiary.habit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitReminderTimeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules and cancels habit reminders via AlarmManager.
 * Each HabitReminderTime gets one alarm. Unique requestCode per habitId+reminderId.
 */
object HabitScheduler {

    private const val TAG = "HabitConfig"
    const val ACTION_HABIT_REMINDER = "com.samsung.android.health.sdk.sample.healthdiary.habit.REMINDER"
    const val EXTRA_HABIT_ID = "habitId"
    const val EXTRA_REMINDER_ID = "reminderId"

    fun scheduleHabit(context: Context, habit: HabitEntity, reminderTimes: List<HabitReminderTimeEntity>) {
        cancelHabit(context, habit.habitId)
        if (!habit.isEnabled) return

        reminderTimes.forEach { reminder ->
            scheduleReminder(context, habit, reminder)
        }
    }

    fun scheduleReminder(context: Context, habit: HabitEntity, reminder: HabitReminderTimeEntity) {
        if (!habit.isEnabled) return

        val (hour, minute) = parseTriggerTime(reminder.triggerTime) ?: return
        val triggerAt = nextTriggerDateTime(hour, minute, reminder.dayOfWeek)
        val intent = Intent(context, HabitAlarmReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habit.habitId)
            putExtra(EXTRA_REMINDER_ID, reminder.reminderId)
        }
        val requestCode = (habit.habitId + reminder.reminderId).hashCode() and 0x7FFFFFFF
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerMillis, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
        val dayInfo = if (reminder.dayOfWeek != null) " on ${reminder.dayOfWeek}" else ""
        Log.i(TAG, "Reminder scheduled habitId=${habit.habitId} reminderId=${reminder.reminderId} at ${reminder.triggerTime}$dayInfo")
    }

    fun cancelHabit(context: Context, habitId: String) {
        val db = AppDatabase.getDatabase(context)
        val reminders = runBlocking { db.habitReminderTimeDao().getByHabitIdSync(habitId) }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        reminders.forEach { reminder ->
            val intent = Intent(context, HabitAlarmReceiver::class.java).apply {
                action = ACTION_HABIT_REMINDER
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_REMINDER_ID, reminder.reminderId)
            }
            val requestCode = (habitId + reminder.reminderId).hashCode() and 0x7FFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.cancel(pendingIntent)
        }
        if (reminders.isNotEmpty()) {
            Log.i(TAG, "Habit reminders cancelled habitId=$habitId (${reminders.size} alarms)")
        }
    }

    fun rescheduleAllEnabledHabits(context: Context) {
        runBlocking {
            val db = AppDatabase.getDatabase(context)
            db.habitDao().getEnabledHabits().first().forEach { habit ->
                val reminders = db.habitReminderTimeDao().getByHabitIdSync(habit.habitId)
                scheduleHabit(context, habit, reminders)
            }
        }
    }

    private fun parseTriggerTime(triggerTime: String): Pair<Int, Int>? {
        return try {
            val parts = triggerTime.split(":")
            if (parts.size >= 2) {
                Pair(parts[0].toInt(), parts[1].toInt())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates the next trigger DateTime for a habit reminder.
     * @param hour Hour (0-23)
     * @param minute Minute (0-59)
     * @param dayOfWeek Day of week string ("mon", "tue", "wed", "thu", "fri", "sat", "sun") or null for daily/all days
     * @return Next LocalDateTime when the reminder should trigger
     */
    private fun nextTriggerDateTime(hour: Int, minute: Int, dayOfWeek: String?): LocalDateTime {
        val now = LocalDateTime.now()
        var trigger = LocalDate.now().atTime(hour, minute)
        
        // If dayOfWeek is null or empty, use daily behavior (backward compatible)
        if (dayOfWeek.isNullOrBlank()) {
            if (trigger.isBefore(now) || trigger.isEqual(now)) {
                trigger = trigger.plusDays(1)
            }
            return trigger
        }
        
        // Map YAML day strings to DayOfWeek enum
        val targetDayOfWeek = mapDayOfWeekString(dayOfWeek) ?: run {
            // If invalid day string, fall back to daily behavior
            if (trigger.isBefore(now) || trigger.isEqual(now)) {
                trigger = trigger.plusDays(1)
            }
            return trigger
        }
        
        // Find next occurrence of the target day of week
        val currentDayOfWeek = trigger.dayOfWeek
        val daysUntilTarget = calculateDaysUntilTargetDay(currentDayOfWeek, targetDayOfWeek)
        
        trigger = trigger.plusDays(daysUntilTarget)
        
        // If the time has already passed today and we're scheduling for today, move to next week
        if (daysUntilTarget == 0L && (trigger.isBefore(now) || trigger.isEqual(now))) {
            trigger = trigger.plusWeeks(1)
        }
        
        return trigger
    }
    
    /**
     * Maps YAML day string to Java DayOfWeek enum.
     * Returns null if the string is invalid.
     */
    private fun mapDayOfWeekString(dayString: String): DayOfWeek? {
        return when (dayString.lowercase()) {
            "mon", "monday" -> DayOfWeek.MONDAY
            "tue", "tuesday" -> DayOfWeek.TUESDAY
            "wed", "wednesday" -> DayOfWeek.WEDNESDAY
            "thu", "thursday" -> DayOfWeek.THURSDAY
            "fri", "friday" -> DayOfWeek.FRIDAY
            "sat", "saturday" -> DayOfWeek.SATURDAY
            "sun", "sunday" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
    
    /**
     * Calculates the number of days until the target day of week.
     * Returns 0 if today is the target day, or 1-6 days until the next occurrence.
     */
    private fun calculateDaysUntilTargetDay(current: DayOfWeek, target: DayOfWeek): Long {
        val currentValue = current.value
        val targetValue = target.value
        val diff = targetValue - currentValue
        return if (diff >= 0) diff.toLong() else (7 + diff).toLong()
    }
}
