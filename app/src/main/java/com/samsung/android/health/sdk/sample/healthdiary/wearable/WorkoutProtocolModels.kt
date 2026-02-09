package com.samsung.android.health.sdk.sample.healthdiary.wearable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Protocol v2: lightweight messages independent from UI models.
 * Paths: phone -> watch "/workout/start", watch -> phone "/workout/ack".
 */

object WorkoutProtocol {
    const val PATH_START = "/workout/start"
    const val PATH_ACK = "/workout/ack"
    const val PROTOCOL_VERSION = 2
    const val TYPE_WORKOUT_START = "WORKOUT_START"
    const val TYPE_WORKOUT_ACK = "WORKOUT_ACK"

    object ReasonCode {
        const val NO_NODES = "NO_NODES"
        const val SEND_FAILED = "SEND_FAILED"
        const val ACK_TIMEOUT = "ACK_TIMEOUT"
        const val INVALID_JSON = "INVALID_JSON"
        const val INVALID_SCHEMA = "INVALID_SCHEMA"
        const val ROUTINE_NOT_FOUND = "ROUTINE_NOT_FOUND"
        const val BLOCK_NOT_FOUND = "BLOCK_NOT_FOUND"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"
    }
}

@Serializable
data class WorkoutStartMessage(
    @SerialName("protocolVersion") val protocolVersion: Int,
    @SerialName("type") val type: String,
    @SerialName("sessionId") val sessionId: String,
    @SerialName("attemptId") val attemptId: String,
    @SerialName("routineId") val routineId: String,
    @SerialName("routineName") val routineName: String,
    @SerialName("blockId") val blockId: String? = null,
    @SerialName("sentAt") val sentAt: Long
)

@Serializable
data class WorkoutAckMessage(
    @SerialName("protocolVersion") val protocolVersion: Int,
    @SerialName("type") val type: String,
    @SerialName("sessionId") val sessionId: String,
    @SerialName("attemptId") val attemptId: String,
    @SerialName("success") val success: Boolean,
    @SerialName("reasonCode") val reasonCode: String? = null,
    @SerialName("reasonMessage") val reasonMessage: String? = null,
    @SerialName("sentAt") val sentAt: Long
)
