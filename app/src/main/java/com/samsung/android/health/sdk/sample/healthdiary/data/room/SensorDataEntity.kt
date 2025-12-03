package com.samsung.android.health.sdk.sample.healthdiary.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_data")
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val type: String,
    val timestamp: Long,
    val values: String, // Storing float array as comma-separated string for simplicity
    val synced: Boolean = false,
    val receivedTimestamp: Long = System.currentTimeMillis()
)
