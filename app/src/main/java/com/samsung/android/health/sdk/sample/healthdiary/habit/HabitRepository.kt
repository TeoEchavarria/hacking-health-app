package com.samsung.android.health.sdk.sample.healthdiary.habit

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitReminderTimeEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class HabitRepository(private val context: Context) {

    private val habitDao = AppDatabase.getDatabase(context).habitDao()
    private val reminderTimeDao = AppDatabase.getDatabase(context).habitReminderTimeDao()

    private companion object {
        private const val TAG = "HabitConfig"
    }

    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAll()

    fun getReminderTimes(habitId: String): Flow<List<HabitReminderTimeEntity>> =
        reminderTimeDao.getByHabitId(habitId)

    suspend fun getHabitById(habitId: String): HabitEntity? = habitDao.getById(habitId)

    suspend fun getReminderTimesSync(habitId: String): List<HabitReminderTimeEntity> =
        reminderTimeDao.getByHabitIdSync(habitId)

    suspend fun saveHabit(habit: HabitEntity, reminderTimes: List<HabitReminderTimeEntity>) {
        val existing = habitDao.getById(habit.habitId)
        if (existing != null) {
            habitDao.update(habit)
            reminderTimeDao.deleteByHabitId(habit.habitId)
        } else {
            habitDao.insert(habit)
        }
        reminderTimes.forEach { reminderTimeDao.insert(it) }
        HabitScheduler.scheduleHabit(context, habit, reminderTimes)
    }

    suspend fun setHabitEnabled(habitId: String, isEnabled: Boolean) {
        val habit = habitDao.getById(habitId) ?: return
        val updated = habit.copy(isEnabled = isEnabled)
        habitDao.update(updated)
        val reminders = reminderTimeDao.getByHabitIdSync(habitId)
        HabitScheduler.scheduleHabit(context, updated, reminders)
        Log.i(TAG, if (isEnabled) "Habit enabled" else "Habit disabled")
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        HabitScheduler.cancelHabit(context, habit.habitId)
        habitDao.delete(habit)
    }

    suspend fun deleteHabitById(habitId: String) {
        HabitScheduler.cancelHabit(context, habitId)
        habitDao.deleteById(habitId)
    }
}
