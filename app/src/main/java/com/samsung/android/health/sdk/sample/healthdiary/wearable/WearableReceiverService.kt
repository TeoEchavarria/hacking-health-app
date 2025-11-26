package com.samsung.android.health.sdk.sample.healthdiary.wearable

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.samsung.android.health.sdk.sample.healthdiary.data.repository.SensorRepository
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WearableReceiverService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Repository will be injected or instantiated here. For now, we'll instantiate it.
    // In a real app, use Hilt/Dagger.
    private lateinit var sensorRepository: SensorRepository

    override fun onCreate() {
        super.onCreate()
        sensorRepository = SensorRepository(applicationContext)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: ${dataEvents.count} events")
        
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == "/sensor_batch") {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val byteArray = dataMapItem.dataMap.getByteArray("batch_data")
                    
                    if (byteArray != null) {
                        scope.launch {
                            try {
                                val jsonString = String(byteArray, Charsets.UTF_8)
                                val batch = Json.decodeFromString<List<SensorData>>(jsonString)
                                Log.d(TAG, "Received batch of ${batch.size} items")
                                
                                sensorRepository.saveBatch(batch)
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing batch", e)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WearableReceiver"
    }
}
