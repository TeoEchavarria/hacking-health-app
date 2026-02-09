package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY title")
    fun getAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE habitId = :habitId")
    suspend fun getById(habitId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE isEnabled = 1 ORDER BY title")
    fun getEnabledHabits(): Flow<List<HabitEntity>>

    @Insert
    suspend fun insert(habit: HabitEntity)

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE habitId = :habitId")
    suspend fun deleteById(habitId: String)
}
