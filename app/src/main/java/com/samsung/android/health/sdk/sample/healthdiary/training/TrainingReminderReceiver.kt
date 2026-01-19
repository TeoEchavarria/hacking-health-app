package com.samsung.android.health.sdk.sample.healthdiary.training

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.activity.HealthMainActivity

/**
 * Broadcast receiver for training reminders
 */
class TrainingReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.samsung.android.health.sdk.sample.healthdiary.training.REMINDER") {
            val blockTypeName = intent.getStringExtra("block_type") ?: return
            val blockType = try {
                BlockType.valueOf(blockTypeName)
            } catch (e: Exception) {
                return
            }
            
            showNotification(context, blockType)
            
            // Reschedule for next day
            val scheduler = TrainingReminderScheduler(context)
            val stateManager = TrainingStateManager(context)
            val reminderTimes = stateManager.getReminderTimes()
            val timeString = reminderTimes[blockType] ?: return
            scheduler.scheduleReminder(blockType, timeString)
        }
    }
    
    private fun showNotification(context: Context, blockType: BlockType) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel
        createNotificationChannel(context, notificationManager)
        
        // Create intent to open training screen
        val mainIntent = Intent(context, HealthMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_training", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            blockType.ordinal,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val blockName = when (blockType) {
            BlockType.A -> "Block A - Cardio Base"
            BlockType.B -> "Block B - Lower Body + Core"
            BlockType.C -> "Block C - Calisthenics"
            BlockType.D -> "Block D - Mobility + Recovery"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Training Reminder")
            .setContentText("Time for $blockName (15 minutes)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(blockType.ordinal, notification)
    }
    
    private fun createNotificationChannel(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Training Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for daily training blocks"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    companion object {
        private const val CHANNEL_ID = "training_reminders"
    }
}
