package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing paired watch devices.
 * Maintains device information, user-assigned aliases, and connection metadata.
 */
@Entity(
    tableName = "paired_devices",
    indices = [Index(value = ["deviceId"], unique = true)]
)
data class PairedDeviceEntity(
    @PrimaryKey
    val deviceId: String,
    
    /** Device name from Wearable API (e.g., "Galaxy Watch 5") */
    val deviceName: String,
    
    /** User-assigned alias for identifying who uses this device (e.g., "Dad", "Mom") */
    val alias: String? = null,
    
    /** Bound node ID from Google Wearable API */
    val boundNodeId: String? = null,
    
    /** Current connection status: DISCONNECTED, CONNECTING, CONNECTED, VERIFIED */
    val connectionStatus: String = "DISCONNECTED",
    
    /** Last successful sync timestamp */
    val lastSyncTimestamp: Long? = null,
    
    /** Timestamp when this device was first paired */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Timestamp when this record was last updated */
    val updatedAt: Long = System.currentTimeMillis()
)
