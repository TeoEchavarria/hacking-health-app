package com.samsung.android.health.sdk.sample.healthdiary.data.domain

/**
 * Domain model for a batch of sensor data
 * Used in the business logic layer, separate from database entities
 */
data class SensorBatch(
    val id: Long = 0,
    val timestamp: Long,
    val sensorType: String,
    val samples: List<SensorSample>,
    val uploaded: Boolean = false,
    val uploadedAt: Long? = null,
    val receivedAt: Long = System.currentTimeMillis()
)

/**
 * Individual sensor sample within a batch
 */
data class SensorSample(
    val timestamp: Long,
    val values: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorSample
        if (timestamp != other.timestamp) return false
        if (!values.contentEquals(other.values)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}
