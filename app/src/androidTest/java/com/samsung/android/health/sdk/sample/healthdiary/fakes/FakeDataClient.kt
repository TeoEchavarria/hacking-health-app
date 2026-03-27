package com.samsung.android.health.sdk.sample.healthdiary.fakes

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataRequest
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple fake for DataClient - does not implement full interface.
 * Use this for basic data item tracking in tests.
 */
class FakeDataClient(
    private val latencyMs: Long = 50L
) {
    
    private val listeners = mutableListOf<DataClient.OnDataChangedListener>()
    private val dataItems = ConcurrentHashMap<String, ByteArray>()
    
    var isConnected = true
    
    /**
     * Simulate data change from watch
     */
    suspend fun simulateDataChange(path: String, data: ByteArray) {
        if (!isConnected) return
        
        kotlinx.coroutines.delay(latencyMs)
        dataItems[path] = data
        
        val event = object : DataEvent {
            override fun getType(): Int = DataEvent.TYPE_CHANGED
            override fun getDataItem(): DataItem {
                return object : DataItem {
                    override fun getUri(): Uri = Uri.parse("wear://$path")
                    override fun getData(): ByteArray = data
                    override fun setData(newData: ByteArray?): DataItem = this
                    override fun getAssets(): MutableMap<String, com.google.android.gms.wearable.DataItemAsset> = mutableMapOf()
                    override fun freeze(): DataItem = this
                    override fun isDataValid(): Boolean = true
                }
            }
            override fun freeze(): DataEvent = this
            override fun isDataValid(): Boolean = true
        }
        
        // Note: DataEventBuffer requires more complex setup, 
        // so we'll trigger listeners individually for now
        listeners.forEach { listener ->
            // In real tests, you may need to trigger this differently
            // For now, this is a simplified version
        }
    }
    
    /**
     * Get data stored at a path
     */
    fun getDataAt(path: String): ByteArray? = dataItems[path]
    
    /**
     * Clear all data
     */
    fun clearData() {
        dataItems.clear()
    }
    
    /**
     * Simplified putDataItem for testing
     */
    fun putDataItem(request: PutDataRequest): Task<DataItem> {
        return if (isConnected) {
            val path = request.uri.path ?: ""
            val data = request.data
            if (data != null) {
                dataItems[path] = data
            }
            Tasks.forResult(object : DataItem {
                override fun getUri(): Uri = Uri.parse("wear://$path")
                override fun getData(): ByteArray = data ?: byteArrayOf()
                override fun setData(newData: ByteArray?): DataItem = this
                override fun getAssets(): MutableMap<String, com.google.android.gms.wearable.DataItemAsset> = mutableMapOf()
                override fun freeze(): DataItem = this
                override fun isDataValid(): Boolean = true
            })
        } else {
            Tasks.forException(Exception("Not connected"))
        }
    }
    
    /**
     * Add data listener
     */
    fun addListener(listener: DataClient.OnDataChangedListener): Task<Void> {
        listeners.add(listener)
        return Tasks.forResult(null)
    }
    
    /**
     * Remove data listener
     */
    fun removeListener(listener: DataClient.OnDataChangedListener): Task<Boolean> {
        val removed = listeners.remove(listener)
        return Tasks.forResult(removed)
    }
    
    /**
     * Delete data items at URI
     */
    fun deleteDataItems(uri: Uri): Task<Int> {
        val path = uri.path ?: ""
        val removed = dataItems.remove(path)
        return Tasks.forResult(if (removed != null) 1 else 0)
    }
}
