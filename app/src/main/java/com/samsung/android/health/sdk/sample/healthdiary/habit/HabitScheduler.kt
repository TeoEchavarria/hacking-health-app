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
        val triggerAt = nextTriggerTime(hour, minute)
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
        Log.i(TAG, "Reminder scheduled habitId=${habit.habitId} reminderId=${reminder.reminderId} at ${reminder.triggerTime}")
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

    private fun nextTriggerTime(hour: Int, minute: Int): LocalDateTime {
        val now = LocalDateTime.now()
        var trigger = LocalDate.now().atTime(hour, minute)
        if (trigger.isBefore(now) || trigger.isEqual(now)) {
            trigger = trigger.plusDays(1)
        }
        return trigger
    }
}
