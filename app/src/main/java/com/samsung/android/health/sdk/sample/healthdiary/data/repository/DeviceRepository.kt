package com.samsung.android.health.sdk.sample.healthdiary.data.repository

import android.content.Context
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.PairedDeviceDao
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairedDeviceEntity
import com.samsung.android.health.sdk.sample.healthdiary.utils.ConnectionStateManager
import com.samsung.android.health.sdk.sample.healthdiary.utils.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing paired watch devices
 * Bridges between Room database, DeviceConfig, and ConnectionStateManager
 */
class DeviceRepository(context: Context) {
    
    private val pairedDeviceDao: PairedDeviceDao = 
        AppDatabase.getDatabase(context).pairedDeviceDao()
    
    /**
     * Get all paired devices as Flow for reactive updates
     */
    fun getAllDevices(): Flow<List<PairedDeviceEntity>> {
        return pairedDeviceDao.getAll()
    }
    
    /**
     * Get device by ID
     */
    suspend fun getDeviceById(deviceId: String): PairedDeviceEntity? = 
        withContext(Dispatchers.IO) {
            pairedDeviceDao.getById(deviceId)
        }
    
    /**
     * Get device by bound node ID
     */
    suspend fun getDeviceByNodeId(nodeId: String): PairedDeviceEntity? = 
        withContext(Dispatchers.IO) {
            pairedDeviceDao.getByBoundNodeId(nodeId)
        }
    
    /**
     * Add a new paired device
     * Also syncs with ConnectionStateManager
     */
    suspend fun addDevice(
        deviceId: String,
        deviceName: String,
        alias: String? = null,
        boundNodeId: String? = null,
        connectionStatus: String = "CONNECTED"
    ): Unit = withContext(Dispatchers.IO) {
        val device = PairedDeviceEntity(
            deviceId = deviceId,
            deviceName = deviceName,
            alias = alias,
            boundNodeId = boundNodeId,
            connectionStatus = connectionStatus,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        pairedDeviceDao.insert(device)
        
        // Sync to ConnectionStateManager
        ConnectionStateManager.setConnectedDevice(
            DeviceInfo(
                name = deviceName,
                id = deviceId,
                isNearby = true,
                alias = alias,
                lastSyncTimestamp = null
            )
        )
        
        // Save bound node ID to DeviceConfig if provided
        if (boundNodeId != null) {
            DeviceConfig.setBoundNodeId(boundNodeId)
        }
    }
    
    /**
     * Set or update device alias
     */
    suspend fun setDeviceAlias(deviceId: String, alias: String?): Unit = 
        withContext(Dispatchers.IO) {
            pairedDeviceDao.updateAlias(deviceId, alias)
            
            // Update ConnectionStateManager if this is the current device
            val currentDevice = ConnectionStateManager.connectedDevice.value
            if (currentDevice?.id == deviceId) {
                ConnectionStateManager.setConnectedDevice(
                    currentDevice.copy(alias = alias)
                )
            }
        }
    
    /**
     * Update device connection status
     */
    suspend fun updateConnectionStatus(deviceId: String, status: String): Unit = 
        withContext(Dispatchers.IO) {
            pairedDeviceDao.updateConnectionStatus(deviceId, status)
        }
    
    /**
     * Update last sync timestamp for a device
     */
    suspend fun updateLastSync(deviceId: String, timestamp: Long): Unit = 
        withContext(Dispatchers.IO) {
            pairedDeviceDao.updateLastSync(deviceId, timestamp)
            
            // Update ConnectionStateManager if this is the current device
            val currentDevice = ConnectionStateManager.connectedDevice.value
            if (currentDevice?.id == deviceId) {
                ConnectionStateManager.setConnectedDevice(
                    currentDevice.copy(lastSyncTimestamp = timestamp)
                )
            }
        }
    
    /**
     * Remove a paired device
     * Also cleans up DeviceConfig if this is the bound device
     */
    suspend fun removeDevice(deviceId: String): Unit = withContext(Dispatchers.IO) {
        val device = pairedDeviceDao.getById(deviceId)
        if (device != null) {
            pairedDeviceDao.deleteById(deviceId)
            
            // Clear from DeviceConfig if this was the bound device
            if (device.boundNodeId == DeviceConfig.getBoundNodeId()) {
                DeviceConfig.setBoundNodeId(null)
            }
            
            // Clear from ConnectionStateManager if this is the current device
            val currentDevice = ConnectionStateManager.connectedDevice.value
            if (currentDevice?.id == deviceId) {
                ConnectionStateManager.setConnectedDevice(null)
            }
        }
    }
    
    /**
     * Check if a device exists in the database
     */
    suspend fun deviceExists(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        pairedDeviceDao.getById(deviceId) != null
    }
    
    /**
     * Check if a device exists by bound node ID
     */
    suspend fun deviceExistsByNodeId(nodeId: String): Boolean = withContext(Dispatchers.IO) {
        pairedDeviceDao.getByBoundNodeId(nodeId) != null
    }
}
