package com.samsung.android.health.sdk.sample.healthdiary.config

/**
 * Models that represent the YAML configuration files:
 * - routines.yaml
 * - habits.yaml
 *
 * These are intentionally simple POJOs so that SnakeYAML can deserialize them
 * without needing any reflection annotations.
 */

data class YamlExercise(
    val name: String = "",
    val duration_sec: Int = 0,
    val rest_after_sec: Int = 0,
    val notes: String = "",
    val wrist_safe: Boolean = true,
    val alternatives: List<String> = emptyList()
)

data class YamlRoutine(
    val routine_id: String = "",
    val name: String = "",
    val description: String = "",
    val total_duration_min: Int = 0,
    val exercises: List<YamlExercise> = emptyList()
)

data class YamlSchedule(
    val days: List<String> = emptyList(),
    val time: String = "",
    val timezone: String = "",
    val window_min: Int = 30
)

data class YamlReminders(
    val snooze_min: Int = 10,
    val max_snoozes: Int = 2,
    val escalation_after_missed: String = ""
)

data class YamlHabit(
    val habit_id: String = "",
    val title: String = "",
    val routine_id: String = "",
    val schedule: YamlSchedule = YamlSchedule(),
    val reminders: YamlReminders = YamlReminders()
)

