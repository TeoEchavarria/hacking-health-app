package com.samsung.android.health.sdk.sample.healthdiary.update.worker

import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig
import com.samsung.android.health.sdk.sample.healthdiary.activity.HealthMainActivity
import com.samsung.android.health.sdk.sample.healthdiary.update.domain.usecase.CheckForUpdatesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkForUpdatesUseCase: CheckForUpdatesUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = checkForUpdatesUseCase(BuildConfig.VERSION_NAME)
            
            result.getOrNull()?.let { update ->
                // Update found!
                val version = update.mobile.version
                showNotification(version)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(version: String) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for app updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, HealthMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("New Update Available")
            .setContentText("Version $version is ready.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
