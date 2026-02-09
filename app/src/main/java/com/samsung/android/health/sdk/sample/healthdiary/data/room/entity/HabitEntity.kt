package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [Index(value = ["habitId"], unique = true)]
)
data class HabitEntity(
    @PrimaryKey val habitId: String,
    val title: String,
    val isEnabled: Boolean = true
)
