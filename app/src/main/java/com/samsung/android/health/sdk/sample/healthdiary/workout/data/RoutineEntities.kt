package com.samsung.android.health.sdk.sample.healthdiary.workout.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routines",
    indices = [Index(value = ["routineId"], unique = true)]
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: String,
    val name: String
)

@Entity(
    tableName = "blocks",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["routineId"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["routineId"]), Index(value = ["blockId"], unique = true)]
)
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockId: String,
    val routineId: String,
    val exerciseName: String,
    val sets: Int,
    val targetWeight: Float,
    val targetReps: Int? = null,
    val restSec: Int,
    val orderIndex: Int
)
