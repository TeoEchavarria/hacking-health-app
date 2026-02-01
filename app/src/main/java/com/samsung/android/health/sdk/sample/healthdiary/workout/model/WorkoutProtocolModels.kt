package com.samsung.android.health.sdk.sample.healthdiary.workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Routine payload sent from phone to watch via /workout/start.
 * Shared schema between phone and watch apps.
 */
@Serializable
data class RoutinePayload(
    @SerialName("sessionId") val sessionId: String,
    @SerialName("routineId") val routineId: String,
    @SerialName("routineName") val routineName: String,
    @SerialName("blocks") val blocks: List<RoutineBlockPayload>,
    @SerialName("sentAt") val sentAt: String
)

@Serializable
data class RoutineBlockPayload(
    @SerialName("blockId") val blockId: String,
    @SerialName("exerciseName") val exerciseName: String,
    @SerialName("sets") val sets: Int,
    @SerialName("targetWeight") val targetWeight: Float,
    @SerialName("targetReps") val targetReps: Int? = null,
    @SerialName("restSec") val restSec: Int
)
