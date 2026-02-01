package com.samsung.android.health.sdk.sample.healthdiary.workout

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.samsung.android.health.sdk.sample.healthdiary.workout.worker.SendWorkoutToWatchWorker
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules daily workout routine sends to connected Wear OS devices.
 * 
 * Uses WorkManager OneTimeWorkRequest with initialDelay to schedule the next occurrence.
 * After each worker runs, it immediately schedules the next day's work (self-rescheduling).
 * 
 * Note: WorkManager periodic work is not exact for "same time every day" use cases.
 * See: https://medium.com/androiddevelopers/workmanager-periodicity-ff35185ff006
 * 
 * Alternative: AlarmManager with exact alarms (requires SCHEDULE_EXACT_ALARM permission
 * and user action on Android 14+). See:
 * https://developer.android.com/develop/background-work/services/alarms
 * https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
 */
object WorkoutScheduler {
    private const val TAG = "WorkoutScheduler"
    private const val WORK_NAME = "send_workout_to_watch"
    
    /**
     * Schedules a daily workout send at the specified local time.
     * 
     * @param context Application context
     * @param time Local time (e.g., 07:30) in device's default timezone
     */
    fun scheduleDailyWorkoutSend(context: Context, time: LocalTime) {
        val workManager = WorkManager.getInstance(context)
        
        // Cancel any existing work with this name
        workManager.cancelUniqueWork(WORK_NAME)
        
        val delayMillis = calculateDelayToNextOccurrence(time)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
            .build()
        
        val workRequest = OneTimeWorkRequest.Builder(SendWorkoutToWatchWorker::class.java)
            .setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag("workout_send")
            .build()
        
        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        val nextRun = ZonedDateTime.now(ZoneId.systemDefault())
            .plusNanos(delayMillis * 1_000_000L)
        
        Log.i(TAG, "Scheduled workout send for ${time} (next run: $nextRun)")
    }
    
    /**
     * Calculates delay in milliseconds until the next occurrence of the specified time.
     * 
     * @param time Target local time
     * @return Milliseconds until next occurrence
     */
    private fun calculateDelayToNextOccurrence(time: LocalTime): Long {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val todayAtTime = now.toLocalDate().atTime(time).atZone(ZoneId.systemDefault())
        
        val delay = if (todayAtTime.isAfter(now)) {
            // Time hasn't passed today, schedule for today
            java.time.Duration.between(now, todayAtTime).toMillis()
        } else {
            // Time has passed, schedule for tomorrow
            val tomorrowAtTime = todayAtTime.plusDays(1)
            java.time.Duration.between(now, tomorrowAtTime).toMillis()
        }
        
        return delay.coerceAtLeast(0)
    }
    
    /**
     * Cancels the scheduled workout send.
     */
    fun cancelScheduledWorkout(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.i(TAG, "Cancelled scheduled workout send")
    }
}
