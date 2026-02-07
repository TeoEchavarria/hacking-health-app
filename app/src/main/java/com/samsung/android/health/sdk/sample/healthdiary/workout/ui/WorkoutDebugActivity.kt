package com.samsung.android.health.sdk.sample.healthdiary.workout.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.workout.WorkoutScheduler
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.RoutineBlockPayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.RoutinePayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.WorkoutAckPayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.WorkoutStatePayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.WorkoutStateRequestPayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.worker.SendWorkoutToWatchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

/**
 * Workout sync screen: start workout from phone, mirror watch state in real time.
 * Watch is source of truth; phone mirrors state via /workout/state messages.
 */
class WorkoutDebugActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WorkoutDebug"
        private const val WORKOUT_SYNC_TAG = "WorkoutSync"
        private const val WORKOUT_START_PATH = "/workout/start"
        private const val WORKOUT_STATE_PATH = "/workout/state"
        private const val WORKOUT_ACK_PATH = "/workout/ack"
        private const val WORKOUT_STATE_REQUEST_PATH = "/workout/state_request"

        private val json = Json { ignoreUnknownKeys = true }
    }

    private lateinit var timePicker: TimePicker
    private lateinit var setTimeButton: Button
    private lateinit var sendNowButton: Button
    private lateinit var startWorkoutButton: Button
    private lateinit var routineNameText: TextView
    private lateinit var exerciseNameText: TextView
    private lateinit var setProgressText: TextView
    private lateinit var statusText: TextView

    private val prefs by lazy { getSharedPreferences("workout_prefs", MODE_PRIVATE) }
    private var activeSessionId: String? = null
    private var messageListener: MessageClient.OnMessageReceivedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_debug)

        timePicker = findViewById(R.id.timePicker)
        setTimeButton = findViewById(R.id.setTimeButton)
        sendNowButton = findViewById(R.id.sendNowButton)
        startWorkoutButton = findViewById(R.id.startWorkoutButton)
        routineNameText = findViewById(R.id.routineNameText)
        exerciseNameText = findViewById(R.id.exerciseNameText)
        setProgressText = findViewById(R.id.setProgressText)
        statusText = findViewById(R.id.statusText)

        val savedHour = prefs.getInt("scheduled_hour", 7)
        val savedMinute = prefs.getInt("scheduled_minute", 30)
        timePicker.hour = savedHour
        timePicker.minute = savedMinute

        updateWorkoutDisplay(null)

        setTimeButton.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            val time = LocalTime.of(hour, minute)
            prefs.edit()
                .putInt("scheduled_hour", hour)
                .putInt("scheduled_minute", minute)
                .apply()
            WorkoutScheduler.scheduleDailyWorkoutSend(this, time)
            Log.d(TAG, "Scheduled workout for $time")
            Log.d(TAG, "Scheduled workout for $time")
        }

        sendNowButton.setOnClickListener { sendNowLegacy() }
        startWorkoutButton.setOnClickListener { startWorkout() }
    }

    override fun onResume() {
        super.onResume()
        registerMessageListener()
        activeSessionId = prefs.getString("active_session_id", null)
        if (activeSessionId != null) {
            requestStateSync()
        }
        updateStartButtonState()
    }

    override fun onPause() {
        super.onPause()
        unregisterMessageListener()
    }

    private fun registerMessageListener() {
        if (messageListener != null) return
        messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
            when (messageEvent.path) {
                WORKOUT_STATE_PATH -> handleWorkoutState(messageEvent.data)
                WORKOUT_ACK_PATH -> handleWorkoutAck(messageEvent.data)
                else -> { }
            }
        }
        Wearable.getMessageClient(this).addListener(messageListener!!)
        Log.d(TAG, "Message listener registered for /workout/state and /workout/ack")
    }

    private fun unregisterMessageListener() {
        messageListener?.let { listener ->
            Wearable.getMessageClient(this).removeListener(listener)
            messageListener = null
            Log.d(TAG, "Message listener unregistered")
        }
    }

    private fun handleWorkoutState(data: ByteArray) {
        lifecycleScope.launch {
            try {
                val jsonStr = String(data, Charsets.UTF_8)
                val payload = json.decodeFromString<WorkoutStatePayload>(jsonStr)
                if (activeSessionId != null && payload.sessionId != activeSessionId) {
                    Log.d(WORKOUT_SYNC_TAG, "Ignoring state: sessionId mismatch (ours=$activeSessionId, theirs=${payload.sessionId})")
                    return@launch
                }
                Log.i(WORKOUT_SYNC_TAG, "State received: sessionId=${payload.sessionId} set=${payload.currentSet}/${payload.totalSets} mode=${payload.mode}")
                withContext(Dispatchers.Main) {
                    updateWorkoutDisplay(payload)
                    if (payload.mode == "FINISHED" || payload.mode == "IDLE") {
                        clearActiveSession()
                    } else {
                        activeSessionId = payload.sessionId
                        prefs.edit().putString("active_session_id", payload.sessionId).apply()
                    }
                    updateStartButtonState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse workout state", e)
            }
        }
    }

    private fun handleWorkoutAck(data: ByteArray) {
        lifecycleScope.launch {
            try {
                val jsonStr = String(data, Charsets.UTF_8)
                val payload = json.decodeFromString<WorkoutAckPayload>(jsonStr)
                Log.i(WORKOUT_SYNC_TAG, "Ack received: sessionId=${payload.sessionId} status=${payload.status}")
                withContext(Dispatchers.Main) {
                    if (payload.status == "STARTED") {
                        statusText.text = "Workout started on watch. Syncing..."
                        activeSessionId = payload.sessionId
                        prefs.edit().putString("active_session_id", payload.sessionId).apply()
                        updateStartButtonState()
                    } else if (payload.status == "REJECTED") {
                        statusText.text = "Watch rejected: ${payload.reason ?: "unknown"}"
                        clearActiveSession()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse workout ack", e)
            }
        }
    }

    private fun updateWorkoutDisplay(state: WorkoutStatePayload?) {
        if (state == null) {
            routineNameText.text = "Routine: —"
            exerciseNameText.text = "Exercise: —"
            setProgressText.text = "Sets: —"
            statusText.text = "Idle"
            return
        }
        routineNameText.text = "Routine: ${state.routineId.ifBlank { "—" }}"
        exerciseNameText.text = "Exercise: ${state.exerciseName.ifBlank { "—" }}"
        setProgressText.text = "Sets: ${state.currentSet} / ${state.totalSets}"
        statusText.text = when (state.mode) {
            "WORK" -> "Running"
            "REST" -> "Rest"
            "FINISHED", "IDLE" -> "Finished"
            else -> state.mode
        }
    }

    private fun updateStartButtonState() {
        startWorkoutButton.isEnabled = activeSessionId == null
    }

    private fun clearActiveSession() {
        activeSessionId = null
        prefs.edit().remove("active_session_id").apply()
        updateWorkoutDisplay(null)
    }

    private fun startWorkout() {
        lifecycleScope.launch {
            startWorkoutButton.isEnabled = false
            statusText.text = "Starting workout on watch..."
            try {
                val sessionId = UUID.randomUUID().toString()
                val routineId = UUID.randomUUID().toString()
                val payload = createDefaultRoutinePayload(sessionId, routineId)
                val nodes = Wearable.getNodeClient(this@WorkoutDebugActivity).connectedNodes.await()
                if (nodes.isEmpty()) {
                    statusText.text = "No connected watch found"
                    startWorkoutButton.isEnabled = true
                    return@launch
                }
                val jsonPayload = json.encodeToString(RoutinePayload.serializer(), payload)
                val bytes = jsonPayload.toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    Wearable.getMessageClient(this@WorkoutDebugActivity)
                        .sendMessage(node.id, WORKOUT_START_PATH, bytes).await()
                    Log.i(TAG, "Sent /workout/start to ${node.displayName}")
                }
                activeSessionId = sessionId
                prefs.edit().putString("active_session_id", sessionId).apply()
                statusText.text = "Sent start to watch. Waiting for ack..."
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start workout", e)
                statusText.text = "Error: ${e.message}"
                startWorkoutButton.isEnabled = true
            }
        }
    }

    private fun requestStateSync() {
        val sid = activeSessionId ?: return
        lifecycleScope.launch {
            try {
                val nodes = Wearable.getNodeClient(this@WorkoutDebugActivity).connectedNodes.await()
                val payload = WorkoutStateRequestPayload(sessionId = sid)
                val bytes = json.encodeToString(WorkoutStateRequestPayload.serializer(), payload).toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    Wearable.getMessageClient(this@WorkoutDebugActivity)
                        .sendMessage(node.id, WORKOUT_STATE_REQUEST_PATH, bytes).await()
                    Log.i(TAG, "Sent /workout/state_request to ${node.displayName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request state sync", e)
            }
        }
    }

    private fun createDefaultRoutinePayload(sessionId: String, routineId: String): RoutinePayload {
        val blocks = listOf(
            RoutineBlockPayload(
                blockId = "block-1",
                exerciseName = "Push-ups",
                sets = 3,
                targetWeight = 0f,
                targetReps = 10,
                restSec = 15
            ),
            RoutineBlockPayload(
                blockId = "block-2",
                exerciseName = "Squats",
                sets = 3,
                targetWeight = 0f,
                targetReps = 15,
                restSec = 15
            ),
            RoutineBlockPayload(
                blockId = "block-3",
                exerciseName = "Plank",
                sets = 3,
                targetWeight = 0f,
                targetReps = null,
                restSec = 15
            )
        )
        return RoutinePayload(
            sessionId = sessionId,
            routineId = routineId,
            routineName = "Default Workout",
            blocks = blocks,
            sentAt = Instant.now().toString()
        )
    }

    private fun sendNowLegacy() {
        sendNowButton.isEnabled = false
        statusText.text = "Sending legacy workout to watch..."
        try {
            val workRequest = androidx.work.OneTimeWorkRequest.Builder(
                SendWorkoutToWatchWorker::class.java
            ).build()
            WorkManager.getInstance(this).enqueue(workRequest)
            statusText.text = "Legacy send triggered. Work ID: ${workRequest.id}"
            prefs.edit().putString("last_send_status", "Triggered at ${System.currentTimeMillis()}").apply()
        } catch (e: Exception) {
            statusText.text = "Error: ${e.message}"
            Log.e(TAG, "Failed to send", e)
        } finally {
            sendNowButton.isEnabled = true
        }
    }
}
