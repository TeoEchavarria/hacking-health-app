package com.samsung.android.health.sdk.sample.healthdiary.wearable

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.activity.HealthMainActivity
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.SensorRepository
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType
import com.samsung.android.health.sdk.sample.healthdiary.utils.TelemetryLogger
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.SensorData
import com.samsung.android.health.sdk.sample.healthdiary.worker.UploadScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

class PhoneWearableService : LifecycleService(), 
    DataClient.OnDataChangedListener, 
    MessageClient.OnMessageReceivedListener {

    private lateinit var sensorRepository: SensorRepository
    private var wakeLock: PowerManager.WakeLock? = null
    
    private fun getTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PHONE_FOREGROUND_STARTED")
        
        sensorRepository = SensorRepository(applicationContext)
        
        // Acquire partial wake lock to keep CPU running for data reception
        acquireWakeLock()
        
        startForegroundService()
        
        // Manual registration for robust connection
        // This ensures we receive events as long as the service is alive (which is always)
        Wearable.getDataClient(this).addListener(this)
        Wearable.getMessageClient(this).addListener(this)
        
        TelemetryLogger.log(
            "PHONE", 
            "Service Started", 
            "[${getTimestamp()}] PhoneWearableService started. Listening for watch data on /sensor_batch."
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PHONE_FOREGROUND_KILLED")
        
        releaseWakeLock()
        Wearable.getDataClient(this).removeListener(this)
        Wearable.getMessageClient(this).removeListener(this)
        
        TelemetryLogger.log(
            "PHONE",
            "Service Stopped",
            "[${getTimestamp()}] PhoneWearableService destroyed."
        )
    }
    
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "HealthDiary::WearableServiceWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L) // 6 hours max
            }
            Log.d(TAG, "Wake lock acquired")
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released")
            }
        }
        wakeLock = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        if (intent == null) {
             Log.d(TAG, "Service restarted by system")
        }
        
        // Always ensure foreground is active
        startForegroundService()
        
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, HealthMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Health Diary Background Service")
            .setContentText("Listening for Watch Data...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= 31 && e is android.app.ForegroundServiceStartNotAllowedException) {
                    Log.e(TAG, "❌ Failed to start foreground service: Start not allowed from background", e)
                } else {
                    Log.w(TAG, "⚠️ Failed to start with type, retrying without type", e)
                    try {
                        startForeground(NOTIFICATION_ID, notification)
                    } catch (e2: Exception) {
                        Log.e(TAG, "❌ Failed to start foreground service even without type", e2)
                    }
                }
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "📥 onDataChanged: Received ${dataEvents.count} event(s)")
        
        TelemetryLogger.log(
            "WATCH",
            "Data Event",
            "[${getTimestamp()}] Received ${dataEvents.count} data event(s) from watch."
        )
        
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val nodeId = event.dataItem.uri.host ?: "unknown"
                Log.d(TAG, "  📍 Path: $path from $nodeId")
                
                when (path) {
                    "/sensor_batch" -> {
                        handleSensorBatch(event)
                        UploadScheduler.scheduleNext(applicationContext)
                    }
                    "/ping" -> {
                        Log.d(TAG, "  🏓 PING received from Watch!")
                        TelemetryLogger.log(
                            "WATCH",
                            "Ping Received",
                            "[${getTimestamp()}] Ping received from watch node: $nodeId"
                        )
                        sendPong(nodeId)
                    }
                    "/handshake_response" -> {
                        handleHandshakeResponse(event, nodeId)
                    }
                    else -> {
                        TelemetryLogger.log(
                            "WATCH",
                            "Unknown Path",
                            "[${getTimestamp()}] Received data on unknown path: $path"
                        )
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val nodeId = messageEvent.sourceNodeId
        Log.d(TAG, "📨 onMessageReceived: $path from $nodeId")
        
        when (path) {
            "/ping" -> {
                Log.d(TAG, "  🏓 PING Message received from Watch!")
                sendPongMessage(nodeId)
            }
        }
    }

    private fun sendPongMessage(nodeId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Wearable.getMessageClient(applicationContext)
                    .sendMessage(nodeId, "/pong", "pong".toByteArray())
                    .await()
                Log.d(TAG, "✅ PONG Message sent successfully to $nodeId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending PONG Message", e)
            }
        }
    }

    private fun handleSensorBatch(event: DataEvent) {
        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val byteArray = dataMapItem.dataMap.getByteArray("batch_data")
        
        if (byteArray != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = String(byteArray, Charsets.UTF_8)
                    val batch = Json.decodeFromString<List<SensorData>>(jsonString)
                    
                    Log.d("ACCEL_PHONE_RECEIVER", "PHONE_ACCEL_PACKET_ACCEPTED: count=${batch.size}")
                    
                    sensorRepository.saveBatch(batch)
                    
                    val queueSize = sensorRepository.getUnsyncedCount()
                    if (batch.isNotEmpty()) {
                        val summary = "count=${batch.size}, range=[${batch.first().timestamp}..${batch.last().timestamp}]"
                        val logMsg = "Received batch: $summary, success=true, queue_size=$queueSize"
                        Log.d("ACCEL_WATCH_TO_PHONE", logMsg)
                        ConnectionLogManager.log(LogType.TRAFFIC, "ACCEL_WATCH_TO_PHONE", logMsg)
                        
                        // Log to UI TelemetryLogger
                        TelemetryLogger.log(
                            "WATCH",
                            "Data Received",
                            "Received ${batch.size} sensor samples from watch. Queue: $queueSize pending."
                        )
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing batch", e)
                    TelemetryLogger.log(
                        "PHONE",
                        "Error",
                        "Failed to parse sensor batch: ${e.message}"
                    )
                }
            }
        } else {
            TelemetryLogger.log(
                "PHONE",
                "Warning",
                "Received sensor_batch event but batch_data was null"
            )
        }
    }

    private fun handleHandshakeResponse(event: DataEvent, nodeId: String) {
        try {
            val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
            val timestamp = dataMapItem.dataMap.getLong("timestamp")
            
            lifecycleScope.launch(Dispatchers.IO) {
                ConnectionStateManager.updateHandshake(timestamp)
                sendHandshakeAck(nodeId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling handshake", e)
        }
    }

    private fun sendHandshakeAck(nodeId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dataClient = Wearable.getDataClient(applicationContext)
                val putDataMapReq = PutDataMapRequest.create("/handshake_ack")
                
                val json = org.json.JSONObject()
                json.put("type", "handshake_ack")
                json.put("source", "phone")
                json.put("timestamp", System.currentTimeMillis())
                
                putDataMapReq.dataMap.putString("payload", json.toString())
                putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
                
                val putDataReq = putDataMapReq.asPutDataRequest()
                putDataReq.setUrgent()
                dataClient.putDataItem(putDataReq).await()
                
                Log.d(TAG, "✅ Handshake ACK sent to $nodeId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending handshake ack", e)
            }
        }
    }

    private fun sendPong(nodeId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use MessageClient to send PONG (same as how watch sends PING)
                val messageClient = Wearable.getMessageClient(applicationContext)
                messageClient.sendMessage(nodeId, "/pong", "pong".toByteArray()).await()
                Log.d(TAG, "✅ PONG Message sent successfully to $nodeId")
                
                TelemetryLogger.log(
                    "PHONE",
                    "Pong Sent",
                    "[${getTimestamp()}] Sent PONG response to watch node: $nodeId"
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending pong message", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Health Diary Background Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        private const val TAG = "PhoneWearableService"
        private const val CHANNEL_ID = "PhoneWearableServiceChannel"
        private const val NOTIFICATION_ID = 202
    }
}
