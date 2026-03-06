package com.samsung.android.health.sdk.sample.healthdiary.update.manager

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface WearUpdateManager {
    fun sendUpdateToWatch(file: File): Flow<TransferStatus>
}

sealed class TransferStatus {
    object Idle : TransferStatus()
    data class Sending(val progress: Int) : TransferStatus()
    object Completed : TransferStatus()
    data class Error(val message: String) : TransferStatus()
}

@Singleton
class WearUpdateManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WearUpdateManager {

    override fun sendUpdateToWatch(file: File): Flow<TransferStatus> = flow {
        emit(TransferStatus.Sending(0))

        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                emit(TransferStatus.Error("No watch connected"))
                return@flow
            }

            // Pick the first connected node
            val node = nodes.first()
            val channelClient = Wearable.getChannelClient(context)

            // Open a channel specifically for the APK
            val channel = channelClient.openChannel(node.id, "/update/apk").await()

            // Send the file
            // Note: In a production app, you might want to implement a progress listener
            // by using `registerChannelCallback` or writing to the OutputStream manually.
            channelClient.sendFile(channel, Uri.fromFile(file)).await()
            
            emit(TransferStatus.Sending(100))
            emit(TransferStatus.Completed)

            // Trigger install message (assuming watch listens for this path)
            Wearable.getMessageClient(context)
                .sendMessage(node.id, "/update/install", "install".toByteArray())
                .await()

        } catch (e: Exception) {
            emit(TransferStatus.Error(e.message ?: "Unknown error sending update to watch"))
        }
    }
}
