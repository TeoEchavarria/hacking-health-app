package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitReminderTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitReminderTimeDao {

    @Query("SELECT * FROM habit_reminder_times WHERE habitId = :habitId ORDER BY triggerTime")
    fun getByHabitId(habitId: String): Flow<List<HabitReminderTimeEntity>>

    @Query("SELECT * FROM habit_reminder_times WHERE habitId = :habitId ORDER BY triggerTime")
    suspend fun getByHabitIdSync(habitId: String): List<HabitReminderTimeEntity>

    @Insert
    suspend fun insert(entity: HabitReminderTimeEntity)

    @Delete
    suspend fun delete(entity: HabitReminderTimeEntity)

    @Query("DELETE FROM habit_reminder_times WHERE habitId = :habitId")
    suspend fun deleteByHabitId(habitId: String)
}
