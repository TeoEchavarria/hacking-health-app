package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairedDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedDeviceDao {

    @Query("SELECT * FROM paired_devices ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PairedDeviceEntity>>
    
    @Query("SELECT * FROM paired_devices ORDER BY createdAt DESC")
    suspend fun getAllSync(): List<PairedDeviceEntity>

    @Query("SELECT * FROM paired_devices WHERE deviceId = :deviceId")
    suspend fun getById(deviceId: String): PairedDeviceEntity?

    @Query("SELECT * FROM paired_devices WHERE boundNodeId = :nodeId")
    suspend fun getByBoundNodeId(nodeId: String): PairedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: PairedDeviceEntity)

    @Update
    suspend fun update(device: PairedDeviceEntity)

    @Query("UPDATE paired_devices SET alias = :alias, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateAlias(deviceId: String, alias: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE paired_devices SET connectionStatus = :status, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateConnectionStatus(deviceId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE paired_devices SET lastSyncTimestamp = :timestamp, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateLastSync(deviceId: String, timestamp: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(device: PairedDeviceEntity)

    @Query("DELETE FROM paired_devices WHERE deviceId = :deviceId")
    suspend fun deleteById(deviceId: String)

    @Query("DELETE FROM paired_devices")
    suspend fun deleteAll()
}
