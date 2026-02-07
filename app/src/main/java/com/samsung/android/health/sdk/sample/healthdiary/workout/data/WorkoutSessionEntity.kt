package com.samsung.android.health.sdk.sample.healthdiary.workout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class BlockSnapshot(
    val blockId: String,
    val exerciseName: String,
    val sets: Int,
    val targetWeight: Float,
    val targetReps: Int?,
    val restSec: Int,
    val orderIndex: Int
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val sessionId: String,
    val routineId: String,
    val routineName: String,
    val startedAt: Long,
    val status: String, // "RUNNING", "FINISHED"
    val blocksSnapshotJson: String, // JSON of List<BlockSnapshot>
    val completionStateJson: String, // JSON of Map<String, List<Boolean>>
    val activeBlockIndex: Int,
    val activeSetIndex: Int
)
