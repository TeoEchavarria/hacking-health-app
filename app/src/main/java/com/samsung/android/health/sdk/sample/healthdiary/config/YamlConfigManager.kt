package com.samsung.android.health.sdk.sample.healthdiary.config

import android.content.Context
import android.util.Log
import org.yaml.snakeyaml.Yaml
import java.io.InputStreamReader

/**
 * Loads and exposes YAML-based routines and habits from the assets folder.
 *
 * Files:
 * - assets/routines.yaml
 * - assets/habits.yaml
 */
object YamlConfigManager {

    private const val TAG = "YamlConfigManager"
    private const val ROUTINES_FILE = "routines.yaml"
    private const val HABITS_FILE = "habits.yaml"

    @Volatile
    private var initialized = false

    private var routinesInternal: List<YamlRoutine> = emptyList()
    private var habitsInternal: List<YamlHabit> = emptyList()

    val routines: List<YamlRoutine>
        get() = routinesInternal

    val habits: List<YamlHabit>
        get() = habitsInternal

    val isInitialized: Boolean
        get() = initialized

    /**
     * Initialize by loading both YAML files from assets.
     * Safe to call multiple times; will only load once.
     */
    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            try {
                val appContext = context.applicationContext
                routinesInternal = loadListFromYaml(
                    appContext,
                    ROUTINES_FILE,
                    Array<YamlRoutine>::class.java
                ).toList()

                habitsInternal = loadListFromYaml(
                    appContext,
                    HABITS_FILE,
                    Array<YamlHabit>::class.java
                ).toList()

                Log.i(TAG, "Loaded ${routinesInternal.size} routines and ${habitsInternal.size} habits from YAML.")
                initialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize YAML config", e)
                // Fail gracefully; keep empty lists to avoid crashes
                routinesInternal = emptyList()
                habitsInternal = emptyList()
            }
        }
    }

    private fun <T> loadListFromYaml(
        context: Context,
        assetName: String,
        arrayType: Class<T>
    ): T {
        val assetManager = context.assets
        assetManager.open(assetName).use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                val yaml = Yaml()
                @Suppress("UNCHECKED_CAST")
                return yaml.loadAs(reader, arrayType)
            }
        }
    }

    fun getRoutineById(routineId: String): YamlRoutine? {
        return routinesInternal.firstOrNull { it.routine_id == routineId }
    }

    fun getHabitsForRoutine(routineId: String): List<YamlHabit> {
        return habitsInternal.filter { it.routine_id == routineId }
    }

    fun getHabitById(habitId: String): YamlHabit? {
        return habitsInternal.firstOrNull { it.habit_id == habitId }
    }
}

