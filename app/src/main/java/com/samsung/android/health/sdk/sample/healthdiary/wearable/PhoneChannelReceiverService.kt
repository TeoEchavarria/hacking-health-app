package com.samsung.android.health.sdk.sample.healthdiary.wearable

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.google.android.gms.wearable.*
import com.samsung.android.health.sdk.sample.healthdiary.data.ingest.ReceivedBatchEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.ingest.SensorIngestDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException

class PhoneChannelReceiverService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ingestDao by lazy { 
        SensorIngestDatabase.getDatabase(applicationContext).ingestDao() 
    }
    private val dataClient by lazy { Wearable.getDataClient(applicationContext) }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        super.onChannelOpened(channel)
        Log.d(TAG, "Channel opened: ${channel.path}")

        if (channel.path == Protocol.PATH_SENSOR_STREAM) {
            handleSensorStream(channel)
        }
    }

    private fun handleSensorStream(channel: ChannelClient.Channel) {
        val channelClient = Wearable.getChannelClient(applicationContext)
        
        scope.launch {
            try {
                val inputStream = channelClient.getInputStream(channel).await()
                val dis = DataInputStream(BufferedInputStream(inputStream))
                
                Log.d(TAG, "Started reading from channel...")
                
                while (true) {
                    try {
                        // Read Header
                        val version = dis.readByte().toInt()
                        if (version != Protocol.VERSION) {
                            Log.w(TAG, "Unknown protocol version: $version")
                            // We might want to skip bytes or close, for now just continue reading 
                            // assuming frame structure is similar or risk crash
                        }
                        
                        val seq = dis.readLong()
                        val timestamp = dis.readLong()
                        val type = dis.readUTF()
                        val payloadSize = dis.readInt()
                        
                        val payload = ByteArray(payloadSize)
                        dis.readFully(payload)
                        
                        // Persist
                        ingestDao.insertBatch(
                            ReceivedBatchEntity(
                                seq = seq,
                                timestamp = timestamp,
                                type = type,
                                payload = payload
                            )
                        )
                        
                        // Ack periodically
                        publishAck(seq)
                        
                    } catch (e: EOFException) {
                        Log.d(TAG, "End of stream")
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading frame", e)
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open input stream", e)
            } finally {
                channelClient.close(channel)
            }
        }
    }

    private var lastPublishedAck = 0L
    private var lastPublishTime = 0L

    private suspend fun publishAck(seq: Long) {
        // Debounce: Only publish if seq changed significantly or time passed
        // For store-and-forward, we want to be somewhat responsive so watch deletes buffer
        val now = System.currentTimeMillis()
        if (seq > lastPublishedAck && (now - lastPublishTime > 2000 || seq - lastPublishedAck > 10)) {
            try {
                val request = PutDataMapRequest.create(Protocol.PATH_PHONE_STATE).apply {
                    dataMap.putLong(Protocol.KEY_PROTOCOL_VERSION, Protocol.VERSION.toLong())
                    dataMap.putLong(Protocol.KEY_LAST_RECEIVED_SEQ, seq)
                    dataMap.putString(Protocol.KEY_STATE, "READY")
                    dataMap.putLong(Protocol.KEY_TIMESTAMP, now)
                }.asPutDataRequest().setUrgent()
                
                dataClient.putDataItem(request).await()
                
                lastPublishedAck = seq
                lastPublishTime = now
                Log.d(TAG, "Published ACK: $seq")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to publish ACK", e)
            }
        }
    }
    
    // Also listen for DataItems to log watch state
    override fun onDataChanged(dataEvents: DataEventBuffer) {
         for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && 
                event.dataItem.uri.path == Protocol.PATH_WATCH_STATE) {
                val item = DataMapItem.fromDataItem(event.dataItem)
                val state = item.dataMap.getString(Protocol.KEY_STATE)
                val backlog = item.dataMap.getInt(Protocol.KEY_BACKLOG_COUNT)
                Log.d(TAG, "Watch State: $state, Backlog: $backlog")
            }
        }
    }

    companion object {
        private const val TAG = "PhoneChannelReceiver"
    }
}
