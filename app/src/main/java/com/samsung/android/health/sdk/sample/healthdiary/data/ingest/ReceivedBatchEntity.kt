package com.samsung.android.health.sdk.sample.healthdiary.data.ingest

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "received_batches",
    indices = [Index(value = ["seq"], unique = true)]
)
data class ReceivedBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seq: Long,
    val timestamp: Long,
    val type: String,
    val payload: ByteArray,
    val receivedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceivedBatchEntity

        if (id != other.id) return false
        if (seq != other.seq) return false
        if (timestamp != other.timestamp) return false
        if (type != other.type) return false
        if (!payload.contentEquals(other.payload)) return false
        if (receivedAt != other.receivedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + seq.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + receivedAt.hashCode()
        return result
    }
}
