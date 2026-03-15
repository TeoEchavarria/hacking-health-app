package com.samsung.android.health.sdk.sample.healthdiary.wearable

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.SensorRepository
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.WatchHealthIngestionRepository
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionLogManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.LogType
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.HealthDailySummary
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.HeartRateSample
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import com.samsung.android.health.sdk.sample.healthdiary.utils.TelemetryLogger

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class WearableReceiverService : WearableListenerService(), com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorRepository: SensorRepository
    private lateinit var watchHealthRepository: WatchHealthIngestionRepository
    
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate() {
        super.onCreate()
        sensorRepository = SensorRepository(applicationContext)
        watchHealthRepository = WatchHealthIngestionRepository(applicationContext)
        Log.d(TAG, "🚀 WearableReceiverService CREATED and READY to receive data")
        TelemetryLogger.log("PHONE", "Service", "WearableReceiverService started")
        
        // Explicitly register MessageClient listener just in case, 
        // although WearableListenerService should handle it if declared in Manifest with correct filters.
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val nodeId = messageEvent.sourceNodeId
        Log.d(TAG, "📨 Message received: $path from $nodeId")
        Log.d("ACCEL_PHONE_RECEIVER", "PHONE_MESSAGE_RECEIVED: path=$path")

        when (path) {
            "/sensor_data" -> {
                handleSensorData(messageEvent)
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown message path: $path")
            }
        }
    }

    private fun handleSensorData(messageEvent: MessageEvent) {
        scope.launch {
            try {
                val jsonString = String(messageEvent.data, Charsets.UTF_8)
                val data = json.decodeFromString<SensorData>(jsonString)
                // Log.d(TAG, "  📊 Sensor data received: ${data.timestamp}")
                
                sensorRepository.saveSensorData(data)
                
                val queueSize = sensorRepository.getUnsyncedCount()
                val logMsg = "Received single: deviceId=${data.deviceId}, timestamp=${data.timestamp}, success=true, queue_size=$queueSize"
                Log.d("ACCEL_WATCH_TO_PHONE", logMsg)
                ConnectionLogManager.log(LogType.TRAFFIC, "ACCEL_WATCH_TO_PHONE", logMsg)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing sensor data", e)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "📥 onDataChanged: Received ${dataEvents.count} event(s)")
        // TelemetryLogger.log("PHONE", "onDataChanged", "Received ${dataEvents.count} data events")
        
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                val nodeId = event.dataItem.uri.host ?: "unknown"
                Log.d(TAG, "  📍 Path: $path from $nodeId")
                
                when (path) {
                    // ===== ACTIVELY USED HEALTH DATA PATHS =====
                    Protocol.PATH_HEALTH_DAILY -> {
                        // Primary sync path: All metrics sent every 15 min
                        handleHealthDailySummary(event)
                    }
                    Protocol.PATH_HEALTH_HR -> {
                        // HR batch path: Sent when 5+ samples accumulated
                        handleHealthHeartRate(event)
                    }
                    
                    // ===== DEPRECATED HEALTH PATHS (handlers kept for compatibility) =====
                    // NOTE: Watch never sends these - all data comes via PATH_HEALTH_DAILY
                    Protocol.PATH_HEALTH_SLEEP -> {
                        // UNUSED: Sleep included in daily summary
                        handleHealthSleep(event)
                    }
                    Protocol.PATH_HEALTH_STEPS -> {
                        // UNUSED: Steps included in daily summary
                        handleHealthSteps(event)
                    }
                    
                    // ===== LEGACY SENSOR PATHS (kept for compatibility) =====
                    "/sensor_batch" -> {
                        handleSensorBatch(event)
                    }
                    "/accel_test" -> {
                        Log.d(TAG, "  🧪 ACCEL TEST received from Watch!")
                        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                        val accelData = dataMapItem.dataMap.getString("accel_data")
                        val timestamp = dataMapItem.dataMap.getLong("timestamp")
                        Log.d(TAG, "  📊 Test data: $accelData at $timestamp")
                        ConnectionLogManager.log(LogType.SUCCESS, TAG, "Received ACCEL TEST: $accelData")
                    }
                    "/handshake_response" -> {
                        Log.d(TAG, "  🤝 Handshake Response received!")
                        handleHandshakeResponse(event, nodeId)
                    }
                    else -> {
                        Log.w(TAG, "  ⚠️ Unknown path: $path")
                    }
                }
            }
        }
    }

    private fun handleSensorBatch(event: com.google.android.gms.wearable.DataEvent) {
        Log.d(TAG, "  🔍 Processing sensor batch...")
        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val byteArray = dataMapItem.dataMap.getByteArray("batch_data")
        
        if (byteArray != null) {
            scope.launch {
                try {
                    val jsonString = String(byteArray, Charsets.UTF_8)
                    val batch = Json.decodeFromString<List<SensorData>>(jsonString)
                    Log.d(TAG, "  ✅ Received batch of ${batch.size} items")
                    ConnectionLogManager.log(LogType.SUCCESS, TAG, "Received batch of ${batch.size} items")
                    Log.d("ACCEL_PHONE_RECEIVER", "PHONE_ACCEL_PACKET_ACCEPTED: count=${batch.size}")
                    
                    sensorRepository.saveBatch(batch)
                    Log.d(TAG, "  💾 Batch saved to database")
                    
                    val queueSize = sensorRepository.getUnsyncedCount()
                    if (batch.isNotEmpty()) {
                        val summary = "count=${batch.size}, range=[${batch.first().timestamp}..${batch.last().timestamp}]"
                        val logMsg = "Received batch: $summary, success=true, queue_size=$queueSize"
                        Log.d("ACCEL_WATCH_TO_PHONE", logMsg)
                        ConnectionLogManager.log(LogType.TRAFFIC, "ACCEL_WATCH_TO_PHONE", logMsg)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "  ❌ Error parsing batch", e)
                    ConnectionLogManager.log(LogType.ERROR, TAG, "Error parsing batch: ${e.message}")
                }
            }
        } else {
            Log.w(TAG, "  ⚠️ Batch data is null!")
        }
    }

    private fun handleHandshakeResponse(event: com.google.android.gms.wearable.DataEvent, nodeId: String) {
        try {
            val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
            val payload = dataMapItem.dataMap.getString("payload")
            val timestamp = dataMapItem.dataMap.getLong("timestamp")
            
            scope.launch {
                Log.d(TAG, "🤝 Handshake payload received: $payload")
                ConnectionLogManager.log(LogType.SUCCESS, TAG, "Handshake verified from $nodeId")
                
                // Update State Manager
                com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager.updateHandshake(timestamp)
                
                // Send Ack
                sendHandshakeAck(nodeId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling handshake", e)
            ConnectionLogManager.log(LogType.ERROR, TAG, "Handshake error: ${e.message}")
        }
    }

    // ============ HEALTH DATA HANDLERS (PRIMARY SOURCE OF TRUTH) ============

    private fun handleHealthDailySummary(event: DataEvent) {
        Log.i(TAG, "📊 Processing health daily summary...")
        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val byteArray = dataMapItem.dataMap.getByteArray("summary")
        val nodeId = event.dataItem.uri.host ?: "unknown"
        val receiveTimestamp = System.currentTimeMillis()
        
        Log.i(TAG, "RECEIVE_PAYLOAD: path=/health/daily, nodeId=$nodeId, size=${byteArray?.size ?: 0} bytes, timestamp=$receiveTimestamp")
        Log.i(TAG, "[DAILY_SUMMARY][PHONE][RECEIVED] nodeId=$nodeId, payload_size=${byteArray?.size ?: 0}")
        
        if (byteArray != null) {
            scope.launch {
                try {
                    val jsonString = String(byteArray, Charsets.UTF_8)
                    Log.d(TAG, "PARSE_START: Deserializing JSON payload (${byteArray.size} bytes)")
                    
                    val summary = json.decodeFromString<HealthDailySummary>(jsonString)
                    
                    Log.i(TAG, "📊 Daily summary: date=${summary.date}, steps=${summary.steps}, hr_samples=${summary.heartRateSamples.size}")
                    Log.i(TAG, "PARSE_SUCCESS: date=${summary.date}, steps=${summary.steps}, sleepMinutes=${summary.sleepMinutes}, hr_count=${summary.heartRateSamples.size}, avgHR=${summary.avgHeartRate}")
                    
                    // Forensic per-metric parse logs
                    Log.i(TAG, "[STEPS][PHONE][PARSED] date=${summary.date}, value=${summary.steps}")
                    if (summary.sleepMinutes != null) {
                        Log.i(TAG, "[SLEEP][PHONE][PARSED] date=${summary.date}, value=${summary.sleepMinutes}min")
                    } else {
                        Log.w(TAG, "[SLEEP][PHONE][PARSED] date=${summary.date}, value=NULL")
                    }
                    Log.i(TAG, "[HEART_RATE][PHONE][PARSED] date=${summary.date}, sample_count=${summary.heartRateSamples.size}, avgBPM=${summary.avgHeartRate}")
                    
                    // Log specific metric values for diagnostic purposes
                    if (summary.steps == 0) {
                        Log.w(TAG, "PARSE_RESULT: Steps value is ZERO - check watch-side collection")
                    }
                    if (summary.sleepMinutes == null) {
                        Log.w(TAG, "PARSE_RESULT: Sleep value is NULL - no sleep detected on watch")
                    } else {
                        Log.i(TAG, "PARSE_RESULT: Sleep detected: ${summary.sleepMinutes} minutes (${summary.sleepMinutes / 60.0} hours)")
                    }
                    
                    Log.d(TAG, "PERSIST_START: Calling ingestDailySummary()")
                    watchHealthRepository.ingestDailySummary(summary)
                    Log.i(TAG, "PERSIST_SUCCESS: Daily summary persisted to database")
                    
                    // Update handshake state - health data received means connection is alive
                    com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager.updateHandshake(System.currentTimeMillis())
                    
                    TelemetryLogger.log(
                        "HEALTH",
                        "Daily Summary",
                        "Received: ${summary.date} - Steps: ${summary.steps}, HR samples: ${summary.heartRateSamples.size}"
                    )
                    ConnectionLogManager.log(LogType.SUCCESS, TAG, "Health daily summary ingested: ${summary.date}")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing health daily summary", e)
                    Log.e(TAG, "PARSE_ERROR: ${e.javaClass.simpleName}: ${e.message}")
                    Log.e(TAG, "[DAILY_SUMMARY][PHONE][PARSED] ERROR: ${e.message}")
                    ConnectionLogManager.log(LogType.ERROR, TAG, "Error parsing daily summary: ${e.message}")
                }
            }
        } else {
            Log.e(TAG, "RECEIVE_ERROR: Byte array is null - invalid payload received")
            Log.e(TAG, "[DAILY_SUMMARY][PHONE][RECEIVED] ERROR: null_payload")
        }
    }

    private fun handleHealthHeartRate(event: DataEvent) {
        Log.d(TAG, "💓 Processing heart rate samples...")
        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val byteArray = dataMapItem.dataMap.getByteArray("samples")
        
        if (byteArray != null) {
            scope.launch {
                try {
                    val jsonString = String(byteArray, Charsets.UTF_8)
                    val samples = json.decodeFromString<List<HeartRateSample>>(jsonString)
                    
                    Log.d(TAG, "💓 Received ${samples.size} heart rate samples")
                    
                    watchHealthRepository.ingestHeartRateSamples(samples)
                    
                    // Update handshake state - health data received means connection is alive
                    com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager.updateHandshake(System.currentTimeMillis())
                    
                    if (samples.isNotEmpty()) {
                        val latest = samples.last()
                        TelemetryLogger.log(
                            "HEALTH",
                            "Heart Rate",
                            "Latest: ${latest.bpm} bpm (${samples.size} samples total)"
                        )
                        ConnectionLogManager.log(LogType.SUCCESS, TAG, "💓 HR received: ${latest.bpm} bpm")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing heart rate samples", e)
                }
            }
        }
    }

    private fun handleHealthSleep(event: DataEvent) {
        Log.d(TAG, "😴 Processing sleep data...")
        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val date = dataMapItem.dataMap.getString("date")
        val sleepMinutes = dataMapItem.dataMap.getInt("sleepMinutes")
        
        if (date != null) {
            scope.launch {
                try {
                    watchHealthRepository.ingestSleepUpdate(date, sleepMinutes)
                    
                    val hours = sleepMinutes / 60.0
                    TelemetryLogger.log(
                        "HEALTH",
                        "Sleep",
                        "$date: ${String.format("%.1f", hours)} hours"
                    )
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling sleep data", e)
                }
            }
        }
    }

    private fun handleHealthSteps(event: DataEvent) {
        Log.d(TAG, "👟 Processing steps data...")
        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val date = dataMapItem.dataMap.getString("date")
        val steps = dataMapItem.dataMap.getInt("steps")
        
        if (date != null) {
            scope.launch {
                try {
                    watchHealthRepository.ingestStepsUpdate(date, steps)
                    
                    TelemetryLogger.log(
                        "HEALTH",
                        "Steps",
                        "$date: $steps steps"
                    )
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling steps data", e)
                }
            }
        }
    }

    // ============ END HEALTH DATA HANDLERS ============

    private fun sendHandshakeAck(nodeId: String) {
         scope.launch {
            try {
                val dataClient = com.google.android.gms.wearable.Wearable.getDataClient(applicationContext)
                val putDataMapReq = com.google.android.gms.wearable.PutDataMapRequest.create("/handshake_ack")
                
                val jsonObj = org.json.JSONObject()
                jsonObj.put("type", "handshake_ack")
                jsonObj.put("source", "phone")
                jsonObj.put("timestamp", System.currentTimeMillis())
                
                putDataMapReq.dataMap.putString("payload", jsonObj.toString())
                putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
                
                val putDataReq = putDataMapReq.asPutDataRequest()
                putDataReq.setUrgent()
                dataClient.putDataItem(putDataReq).await()
                
                Log.d(TAG, "✅ Handshake ACK sent to $nodeId")
                ConnectionLogManager.log(LogType.TRAFFIC, TAG, "Sent Handshake ACK to $nodeId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending handshake ack", e)
            }
        }
    }


    private fun sendPong(nodeId: String) {
        scope.launch {
            try {
                Log.d(TAG, "📤 Sending PONG to $nodeId... (Message)")
                val messageClient = Wearable.getMessageClient(applicationContext)
                messageClient.sendMessage(nodeId, "/pong", "pong".toByteArray()).await()
                
                Log.d(TAG, "✅ PONG sent successfully to $nodeId")
                TelemetryLogger.log("PHONE", "Pong", "Sent PONG to $nodeId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending pong", e)
                TelemetryLogger.log("PHONE", "Error", "Failed to send PONG: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "WearableReceiver"
    }
}
