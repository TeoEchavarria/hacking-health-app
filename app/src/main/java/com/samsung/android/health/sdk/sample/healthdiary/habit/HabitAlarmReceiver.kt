package com.samsung.android.health.sdk.sample.healthdiary.habit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.activity.HealthMainActivity
import com.samsung.android.health.sdk.sample.healthdiary.config.YamlConfigManager
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.Instant

/**
 * BroadcastReceiver triggered by AlarmManager at habit trigger time.
 * Sends /habit/reminder/start to connected watch nodes.
 * Launches HabitConfirmationActivity on phone.
 * Reschedules the specific reminder for next day.
 */
class HabitAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HabitSync"
        const val HABIT_REMINDER_START_PATH = "/habit/reminder/start"
        private const val CHANNEL_ID = "habit_channel"
        private const val NOTIFICATION_ID_BASE = 3000
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != HabitScheduler.ACTION_HABIT_REMINDER) return
        val habitId = intent.getStringExtra(HabitScheduler.EXTRA_HABIT_ID) ?: return
        val reminderId = intent.getStringExtra(HabitScheduler.EXTRA_REMINDER_ID)

        // Ensure YAML config is loaded to look up routineId
        if (!YamlConfigManager.isInitialized) {
            YamlConfigManager.initialize(context)
        }

        kotlinx.coroutines.runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                val habit = db.habitDao().getById(habitId) ?: run {
                    Log.w(TAG, "Habit not found: $habitId")
                    return@runBlocking
                }
                if (!habit.isEnabled) {
                    Log.d(TAG, "Habit disabled, skipping: $habitId")
                    return@runBlocking
                }

                val reminder = if (reminderId != null) {
                    db.habitReminderTimeDao().getByHabitIdSync(habitId).find { it.reminderId == reminderId }
                } else null

                // 1. Send to Watch (Start Alarm)
                sendReminderToWatch(context, habit)
                
                // 2. Launch Confirmation Activity (Phone)
                // We launch a full-screen activity to ask for availability
                val yamlHabit = YamlConfigManager.getHabitById(habit.habitId)
                val routineId = yamlHabit?.routine_id
                
                val confirmIntent = Intent(context, HabitConfirmationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(HabitConfirmationActivity.EXTRA_HABIT_ID, habit.habitId)
                    putExtra(HabitConfirmationActivity.EXTRA_TITLE, habit.title)
                    if (routineId != null) {
                        putExtra(HabitConfirmationActivity.EXTRA_ROUTINE_ID, routineId)
                    }
                }
                context.startActivity(confirmIntent)

                // 3. Reschedule
                if (reminder != null) {
                    HabitScheduler.scheduleReminder(context, habit, reminder)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in HabitAlarmReceiver", e)
            }
        }
    }

    private fun showHabitNotification(context: Context, habit: com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val yamlHabit = YamlConfigManager.getHabitById(habit.habitId)
        val routineId = yamlHabit?.routine_id

        // Deep link intent
        val activityIntent = Intent(context, HealthMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_training", true) // Fallback to training list
            if (routineId != null) {
                putExtra("open_routine_id", routineId) // Pass routine ID for potential deep link support
            }
        }

        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(activityIntent)
            getPendingIntent(
                habit.habitId.hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this resource exists, or use a generic one
            .setContentTitle("Habit Reminder")
            .setContentText("Time for: ${habit.title}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + habit.habitId.hashCode(), notification)
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for habit reminders"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendReminderToWatch(context: Context, habit: com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitEntity) {
        runBlocking(Dispatchers.IO) {
            val nodeClient: NodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected watch nodes for habit reminder")
                return@runBlocking
            }

            val payload = JSONObject().apply {
                put("habitId", habit.habitId)
                put("title", habit.title)
                put("triggerAt", Instant.now().toString())
            }
            val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)

            val messageClient = Wearable.getMessageClient(context)
            for (node in nodes) {
                try {
                    messageClient.sendMessage(node.id, HABIT_REMINDER_START_PATH, payloadBytes).await()
                    Log.i(TAG, "TX /habit/reminder/start habitId=${habit.habitId} to ${node.displayName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send habit reminder to ${node.displayName}", e)
                }
            }
        }
    }
}
