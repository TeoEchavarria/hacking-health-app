package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Location Point Entity
 * 
 * Stores GPS location history for tracking feature.
 * Used to create 24-hour timeline of user movements.
 */
@Entity(tableName = "location_points")
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    /** Latitude in degrees (-90 to 90) */
    val latitude: Double,
    
    /** Longitude in degrees (-180 to 180) */
    val longitude: Double,
    
    /** Human-readable address or place name */
    val address: String,
    
    /** Timestamp when location was recorded (milliseconds since epoch) */
    val timestamp: Long,
    
    /** Activity type: "Walking", "Still", "Driving", "Unknown" */
    val activityType: String,
    
    /** Optional additional details about the location */
    val notes: String? = null,
    
    /** When this record was created (for cleanup) */
    val createdAt: Long = System.currentTimeMillis()
)
