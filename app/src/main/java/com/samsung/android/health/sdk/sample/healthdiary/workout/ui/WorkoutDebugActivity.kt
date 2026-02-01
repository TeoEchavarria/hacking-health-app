package com.samsung.android.health.sdk.sample.healthdiary.workout.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.workout.WorkoutScheduler
import com.samsung.android.health.sdk.sample.healthdiary.workout.worker.SendWorkoutToWatchWorker
import java.time.LocalTime

/**
 * Debug screen for workout scheduling and manual testing.
 * 
 * Features:
 * - Set scheduled time
 * - Manually trigger "Send now" (bypasses schedule)
 * - Display last send status
 */
class WorkoutDebugActivity : AppCompatActivity() {
    
    private lateinit var timePicker: TimePicker
    private lateinit var setTimeButton: Button
    private lateinit var sendNowButton: Button
    private lateinit var statusText: TextView
    
    private val prefs by lazy {
        getSharedPreferences("workout_prefs", MODE_PRIVATE)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_debug)
        
        timePicker = findViewById(R.id.timePicker)
        setTimeButton = findViewById(R.id.setTimeButton)
        sendNowButton = findViewById(R.id.sendNowButton)
        statusText = findViewById(R.id.statusText)
        
        // Initialize time picker with saved time (default: 07:30)
        val savedHour = prefs.getInt("scheduled_hour", 7)
        val savedMinute = prefs.getInt("scheduled_minute", 30)
        timePicker.hour = savedHour
        timePicker.minute = savedMinute
        
        // Load last status
        updateStatusDisplay()
        
        setTimeButton.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            val time = LocalTime.of(hour, minute)
            
            prefs.edit()
                .putInt("scheduled_hour", hour)
                .putInt("scheduled_minute", minute)
                .apply()
            
            WorkoutScheduler.scheduleDailyWorkoutSend(this, time)
            
            statusText.text = "Scheduled for ${String.format("%02d:%02d", hour, minute)}\n" +
                    "Next run will be calculated automatically."
            
            Log.d("WorkoutDebug", "Scheduled workout for $time")
        }
        
        sendNowButton.setOnClickListener {
            sendNow()
        }
    }
    
    private fun sendNow() {
        sendNowButton.isEnabled = false
        statusText.text = "Sending workout to watch..."
        
        try {
            // Create a one-time work request with no delay
            val workManager = WorkManager.getInstance(this)
            val workRequest = androidx.work.OneTimeWorkRequest.Builder(
                SendWorkoutToWatchWorker::class.java
            ).build()
            
            workManager.enqueue(workRequest)
            
            statusText.text = "Send triggered. Check logs for details.\n" +
                    "Work ID: ${workRequest.id}"
            
            // Save status
            prefs.edit().putString("last_send_status", "Triggered at ${System.currentTimeMillis()}").apply()
            
            Log.d("WorkoutDebug", "Manual send triggered, work ID: ${workRequest.id}")
        } catch (e: Exception) {
            statusText.text = "Error: ${e.message}"
            Log.e("WorkoutDebug", "Failed to send", e)
        } finally {
            sendNowButton.isEnabled = true
        }
    }
    
    private fun updateStatusDisplay() {
        val lastStatus = prefs.getString("last_send_status", "No sends yet")
        val scheduledHour = prefs.getInt("scheduled_hour", 7)
        val scheduledMinute = prefs.getInt("scheduled_minute", 30)
        
        statusText.text = "Scheduled: ${String.format("%02d:%02d", scheduledHour, scheduledMinute)}\n" +
                "Last status: $lastStatus"
    }
}
