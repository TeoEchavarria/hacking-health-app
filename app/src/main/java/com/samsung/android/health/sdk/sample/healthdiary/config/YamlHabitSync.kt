package com.samsung.android.health.sdk.sample.healthdiary.config

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitReminderTimeEntity
import com.samsung.android.health.sdk.sample.healthdiary.habit.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Synchronizes YAML-defined habits to the database.
 * Called during app initialization to ensure habits from YAML are available in the app.
 */
object YamlHabitSync {
    private const val TAG = "YamlHabitSync"

    /**
     * Syncs all habits from YAML config to the database.
     * Only creates habits that don't already exist (by habitId).
     * This is safe to call multiple times.
     */
    fun syncHabitsToDatabase(context: Context) {
        if (!YamlConfigManager.isInitialized) {
            Log.w(TAG, "YamlConfigManager not initialized yet, skipping sync")
            return
        }

        val repository = HabitRepository(context)
        val habits = YamlConfigManager.habits

        if (habits.isEmpty()) {
            Log.i(TAG, "No habits found in YAML config")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                habits.forEach { yamlHabit ->
                    val existingHabit = repository.getHabitById(yamlHabit.habit_id)
                    
                    if (existingHabit == null) {
                        // Create habit entity
                        val habitEntity = HabitEntity(
                            habitId = yamlHabit.habit_id,
                            title = yamlHabit.title,
                            isEnabled = true, // Enable by default
                            routineId = yamlHabit.routine_id
                        )

                        // Create reminder time entities for each scheduled day/time
                        // Each day gets its own reminder entry with dayOfWeek set
                        val reminderTimes = yamlHabit.schedule.days.map { day ->
                            HabitReminderTimeEntity(
                                reminderId = UUID.randomUUID().toString(),
                                habitId = yamlHabit.habit_id,
                                triggerTime = yamlHabit.schedule.time, // "HH:mm" format
                                dayOfWeek = day // "mon", "tue", "wed", etc.
                            )
                        }

                        // Save to database and schedule alarms
                        repository.saveHabit(habitEntity, reminderTimes)
                        Log.i(TAG, "Synced habit: ${yamlHabit.title} (${reminderTimes.size} reminders)")
                    } else {
                        Log.d(TAG, "Habit already exists: ${yamlHabit.habit_id}, skipping")
                    }
                }
                Log.i(TAG, "Completed syncing ${habits.size} habits from YAML to database")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync habits from YAML", e)
            }
        }
    }
}
