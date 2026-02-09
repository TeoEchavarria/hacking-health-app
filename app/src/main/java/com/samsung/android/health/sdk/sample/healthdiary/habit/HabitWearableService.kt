package com.samsung.android.health.sdk.sample.healthdiary.habit

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.samsung.android.health.sdk.sample.healthdiary.utils.TelemetryLogger
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Receives habit-related messages from the watch:
 * - /habit/reminder/done: User completed the habit on watch
 * - /habit/reminder/postpone: User postponed the reminder
 * - /habit/log: Habit reminder log (triggered/done/postponed)
 */
class HabitWearableService : WearableListenerService() {

    companion object {
        private const val TAG = "HabitSync"
        const val PATH_DONE = "/habit/reminder/done"
        const val PATH_POSTPONE = "/habit/reminder/postpone"
        const val PATH_LOG = "/habit/log"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            PATH_DONE -> handleDone(messageEvent)
            PATH_POSTPONE -> handlePostpone(messageEvent)
            PATH_LOG -> handleLog(messageEvent)
            else -> { }
        }
    }

    private fun handleLog(messageEvent: MessageEvent) {
        try {
            val payload = JSONObject(String(messageEvent.data, Charsets.UTF_8))
            val title = payload.optString("title", "Habit")
            val triggeredAt = payload.optLong("triggeredAt", System.currentTimeMillis())
            val status = payload.optString("status", "TRIGGERED")
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(triggeredAt))
            val details = "Habit $title triggered at $timeStr - $status"
            TelemetryLogger.log("HABIT", "Habit Reminder", details, triggeredAt)
            Log.i(TAG, "Habit log received: $details")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse habit log payload", e)
        }
    }

    private fun handleDone(messageEvent: MessageEvent) {
        try {
            val payload = JSONObject(String(messageEvent.data, Charsets.UTF_8))
            val habitId = payload.optString("habitId", "")
            Log.i(TAG, "Habit reminder completed habitId=$habitId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse done payload", e)
        }
    }

    private fun handlePostpone(messageEvent: MessageEvent) {
        try {
            val payload = JSONObject(String(messageEvent.data, Charsets.UTF_8))
            val habitId = payload.optString("habitId", "")
            val postponedUntil = payload.optString("postponedUntil", "")
            Log.i(TAG, "Habit reminder postponed habitId=$habitId until=$postponedUntil")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse postpone payload", e)
        }
    }
}
