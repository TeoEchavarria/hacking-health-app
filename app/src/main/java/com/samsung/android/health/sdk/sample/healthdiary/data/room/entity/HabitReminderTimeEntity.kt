package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_reminder_times",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["habitId"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"])]
)
data class HabitReminderTimeEntity(
    @PrimaryKey val reminderId: String,
    val habitId: String,
    val triggerTime: String, // "HH:mm"
    val dayOfWeek: String? = null // "mon", "tue", "wed", "thu", "fri", "sat", "sun", or null for daily/all days
)
