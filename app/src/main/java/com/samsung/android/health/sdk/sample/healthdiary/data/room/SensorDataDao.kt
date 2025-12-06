package com.samsung.android.health.sdk.sample.healthdiary.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SensorDataDao {
    @Insert
    suspend fun insert(data: SensorDataEntity)

    @Insert
    suspend fun insertAll(data: List<SensorDataEntity>)

    @Query("SELECT * FROM sensor_data ORDER BY timestamp ASC")
    suspend fun getAll(): List<SensorDataEntity>

    @Query("SELECT * FROM sensor_data WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedBatch(limit: Int): List<SensorDataEntity>

    @Query("SELECT COUNT(*) FROM sensor_data WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("DELETE FROM sensor_data WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    @Query("SELECT COUNT(*) FROM sensor_data")
    suspend fun getCount(): Int
}
