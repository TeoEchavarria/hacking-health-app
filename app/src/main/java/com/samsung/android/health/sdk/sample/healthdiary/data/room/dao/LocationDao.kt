package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

/**
 * Location Data Access Object
 * 
 * Provides queries for location tracking history.
 */
@Dao
interface LocationDao {
    
    /**
     * Insert a new location point
     */
    @Insert
    suspend fun insert(location: LocationPointEntity)
    
    /**
     * Insert multiple location points
     */
    @Insert
    suspend fun insertAll(locations: List<LocationPointEntity>)
    
    /**
     * Get all location points from the last 24 hours, ordered by timestamp (newest first)
     */
    @Query("""
        SELECT * FROM location_points 
        WHERE timestamp > :timestampCutoff 
        ORDER BY timestamp DESC
    """)
    fun getLast24Hours(timestampCutoff: Long): Flow<List<LocationPointEntity>>
    
    /**
     * Get the most recent location point
     */
    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): LocationPointEntity?
    
    /**
     * Get location point by ID
     */
    @Query("SELECT * FROM location_points WHERE id = :id")
    suspend fun getById(id: Long): LocationPointEntity?
    
    /**
     * Delete location points older than the specified timestamp
     */
    @Query("DELETE FROM location_points WHERE timestamp < :timestampCutoff")
    suspend fun deleteOlderThan(timestampCutoff: Long): Int
    
    /**
     * Delete all location points
     */
    @Query("DELETE FROM location_points")
    suspend fun deleteAll()
    
    /**
     * Get total count of location points
     */
    @Query("SELECT COUNT(*) FROM location_points")
    suspend fun getCount(): Int
    
    /**
     * Get count of location points in the last 24 hours
     */
    @Query("SELECT COUNT(*) FROM location_points WHERE timestamp > :timestampCutoff")
    suspend fun getCountLast24Hours(timestampCutoff: Long): Int
}
