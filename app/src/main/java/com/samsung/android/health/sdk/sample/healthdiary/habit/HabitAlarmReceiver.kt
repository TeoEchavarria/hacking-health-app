package com.samsung.android.health.sdk.sample.healthdiary.habit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.Instant

/**
 * BroadcastReceiver triggered by AlarmManager at habit trigger time.
 * Sends /habit/reminder/start to connected watch nodes.
 * Reschedules the specific reminder for next day.
 */
class HabitAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HabitSync"
        const val HABIT_REMINDER_START_PATH = "/habit/reminder/start"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != HabitScheduler.ACTION_HABIT_REMINDER) return
        val habitId = intent.getStringExtra(HabitScheduler.EXTRA_HABIT_ID) ?: return
        val reminderId = intent.getStringExtra(HabitScheduler.EXTRA_REMINDER_ID)

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

                sendReminderToWatch(context, habit)
                if (reminder != null) {
                    HabitScheduler.scheduleReminder(context, habit, reminder)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in HabitAlarmReceiver", e)
            }
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
