package com.samsung.android.health.sdk.sample.healthdiary.update.manager

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface AppUpdateManager {
    fun downloadUpdate(url: String, fileName: String): Flow<DownloadStatus>
    fun installUpdate(file: File)
}

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Int) : DownloadStatus()
    data class Completed(val file: File) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

@Singleton
class AppUpdateManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppUpdateManager {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    override fun downloadUpdate(url: String, fileName: String): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Downloading(0))

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Update")
            .setDescription("Downloading $fileName")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        var finished = false
        var pendingStartTime: Long = 0

        while (!finished) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        finished = true
                        emit(DownloadStatus.Completed(file))
                    }
                    DownloadManager.STATUS_FAILED -> {
                        finished = true
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(reasonIndex)
                        emit(DownloadStatus.Error("Download failed: $reason"))
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        finished = true
                        downloadManager.remove(downloadId)
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(reasonIndex)
                        emit(DownloadStatus.Error("Download paused: $reason"))
                    }
                    DownloadManager.STATUS_PENDING -> {
                        if (pendingStartTime == 0L) {
                            pendingStartTime = System.currentTimeMillis()
                        } else if (System.currentTimeMillis() - pendingStartTime > 30000) { // 30 seconds timeout
                            finished = true
                            downloadManager.remove(downloadId)
                            emit(DownloadStatus.Error("Download pending timeout"))
                        }
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        pendingStartTime = 0 // Reset pending timer
                        val totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        
                        val totalSize = cursor.getLong(totalSizeIndex)
                        val downloaded = cursor.getLong(downloadedIndex)
                        
                        if (totalSize > 0) {
                            val progress = ((downloaded * 100) / totalSize).toInt()
                            emit(DownloadStatus.Downloading(progress))
                        } else {
                            // If total size is unknown, show indeterminate or small progress
                            emit(DownloadStatus.Downloading(1))
                        }
                    }
                }
            } else {
                 emit(DownloadStatus.Error("Download cancelled"))
                 finished = true
            }
            cursor.close()
            if (!finished) {
                delay(1000) // Poll every second
            }
        }
    }

    override fun installUpdate(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
    }
}
