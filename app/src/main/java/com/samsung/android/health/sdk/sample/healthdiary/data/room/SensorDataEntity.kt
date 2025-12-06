package com.samsung.android.health.sdk.sample.healthdiary.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_data")
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
