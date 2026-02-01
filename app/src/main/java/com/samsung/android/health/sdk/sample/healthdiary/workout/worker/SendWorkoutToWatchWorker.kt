package com.samsung.android.health.sdk.sample.healthdiary.workout.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.samsung.android.health.sdk.sample.healthdiary.workout.WorkoutScheduler
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.Routine
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.Segment
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.SegmentType
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Worker that sends workout routine to connected Wear OS devices via MessageClient.
 * 
 * After sending, immediately schedules the next day's work (self-rescheduling).
 * 
 * References:
 * - Wear OS Data Layer: https://developer.android.com/training/wearables/data/overview
 * - MessageClient API: https://developers.google.com/android/reference/com/google/android/gms/wearable/MessageClient
 */
class SendWorkoutToWatchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SendWorkoutToWatchWorker"
        const val MESSAGE_PATH = "/workout/push"
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting workout send worker")
            
            // Generate default routine if none exists
            val routine = generateDefaultRoutine()
            val jsonPayload = Gson().toJson(routine)
            
            Log.d(TAG, "Generated routine: $jsonPayload")
            
            // Get connected nodes
            val nodeClient: NodeClient = Wearable.getNodeClient(applicationContext)
            val nodes = nodeClient.connectedNodes.await()
            
            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected watch nodes found")
                // Still schedule next day - watch might connect later
                scheduleNextDay()
                return Result.retry() // Retry in case watch connects soon
            }
            
            // Send to all connected nodes
            val messageClient: MessageClient = Wearable.getMessageClient(applicationContext)
            var successCount = 0
            var failureCount = 0
            
            for (node in nodes) {
                try {
                    val result = messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH,
                        jsonPayload.toByteArray()
                    ).await()
                    
                    Log.i(TAG, "Sent workout to node ${node.displayName} (${node.id}): $result")
                    successCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send workout to node ${node.displayName}", e)
                    failureCount++
                }
            }
            
            Log.i(TAG, "Workout send completed: $successCount success, $failureCount failures")
            
            // Schedule next day regardless of success/failure
            scheduleNextDay()
            
            // Return success if at least one node received the message
            if (successCount > 0) {
                Result.success()
            } else {
                Result.retry() // Retry if all nodes failed
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in workout send worker", e)
            scheduleNextDay() // Still schedule next day
            Result.retry()
        }
    }
    
    /**
     * Generates a default workout routine.
     * In production, this could load from preferences or a database.
     */
    private fun generateDefaultRoutine(): Routine {
        val segments = listOf(
            Segment(SegmentType.WORK, "Push-ups", 30),
            Segment(SegmentType.REST, "Rest", 15),
            Segment(SegmentType.WORK, "Squats", 30),
            Segment(SegmentType.REST, "Rest", 15),
            Segment(SegmentType.WORK, "Plank", 30),
            Segment(SegmentType.REST, "Rest", 15)
        )
        
        return Routine(
            routineId = UUID.randomUUID().toString(),
            startAt = Instant.now().toString(), // ISO 8601
            segments = segments
        )
    }
    
    /**
     * Schedules the next day's workout send.
     * Reads scheduled time from preferences (default: 07:30).
     */
    private suspend fun scheduleNextDay() {
        val prefs = applicationContext.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
        val hour = prefs.getInt("scheduled_hour", 7)
        val minute = prefs.getInt("scheduled_minute", 30)
        val scheduledTime = LocalTime.of(hour, minute)
        
        WorkoutScheduler.scheduleDailyWorkoutSend(applicationContext, scheduledTime)
        Log.d(TAG, "Scheduled next workout send for $scheduledTime")
    }
}
