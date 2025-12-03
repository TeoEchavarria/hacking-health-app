package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a batch of sensor data received from the watch
 * Data is received approximately every 5 minutes
 */
@Entity(tableName = "sensor_batches")
data class SensorBatchEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    /** Timestamp when the batch was created (milliseconds since epoch) */
    val timestamp: Long,
    
    /** Type of sensor data (e.g., "accelerometer", "heart_rate", "gyroscope") */
    val sensorType: String,
    
    /** Serialized JSON array of sensor readings in the batch */
    val dataJson: String,
    
    /** Number of individual samples in this batch */
    val sampleCount: Int,
    
    /** Whether this batch has been successfully uploaded to backend */
    val uploaded: Boolean = false,
    
    /** Timestamp when the batch was uploaded (null if not uploaded yet) */
    val uploadedAt: Long? = null,
    
    /** Timestamp when the batch was received by the phone */
    val receivedAt: Long = System.currentTimeMillis()
)
