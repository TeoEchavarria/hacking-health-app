package com.samsung.android.health.sdk.sample.healthdiary.training

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Training session data models
 */

enum class BlockType {
    A, B, C, D
}

enum class CardioMode {
    CYCLING, RUNNING, SWIMMING
}

data class Exercise(
    val id: String,
    val name: String,
    val durationSeconds: Int? = null, // For timed exercises
    val reps: Int? = null, // For rep-based exercises
    val isTimed: Boolean = false
) {
    fun getDisplayText(): String {
        return when {
            isTimed && durationSeconds != null -> "$name - ${durationSeconds}s"
            reps != null -> "$name - $reps reps"
            else -> name
        }
    }
}

data class CardioSegment(
    val name: String,
    val durationMinutes: Int
)

data class CardioOption(
    val mode: CardioMode,
    val segments: List<CardioSegment>
)

data class BlockA(
    val selectedMode: CardioMode = CardioMode.CYCLING,
    val isCompleted: Boolean = false,
    val completedSegments: Set<String> = emptySet()
) {
    companion object {
        val CYCLING_OPTION = CardioOption(
            CardioMode.CYCLING,
            listOf(
                CardioSegment("Easy pace", 3),
                CardioSegment("Moderate pace", 8),
                CardioSegment("Easy pace", 4)
            )
        )
        
        val RUNNING_OPTION = CardioOption(
            CardioMode.RUNNING,
            listOf(
                CardioSegment("Brisk walk", 3),
                CardioSegment("Easy continuous run", 10),
                CardioSegment("Walking", 2)
            )
        )
        
        val SWIMMING_OPTION = CardioOption(
            CardioMode.SWIMMING,
            listOf(
                CardioSegment("Kick with board", 5),
                CardioSegment("Backstroke (easy)", 5),
                CardioSegment("Relaxed freestyle", 5)
            )
        )
    }
}

data class BlockB(
    val isCompleted: Boolean = false,
    val completedRounds: Int = 0,
    val completedExercises: Set<String> = emptySet(),
    val restDurationSeconds: Int = 30
) {
    companion object {
        val EXERCISES = listOf(
            Exercise("squats", "Squats", reps = 15),
            Exercise("lunges", "Alternating lunges", reps = 10),
            Exercise("glute_bridges", "Glute bridges", reps = 20),
            Exercise("calf_raises", "Calf raises", reps = 20),
            Exercise("hollow_hold", "Hollow body hold", durationSeconds = 30, isTimed = true),
            Exercise("superman", "Superman hold", durationSeconds = 30, isTimed = true)
        )
        const val TOTAL_ROUNDS = 3
    }
}

data class BlockC(
    val isCompleted: Boolean = false,
    val completedRounds: Int = 0,
    val completedExercises: Set<String> = emptySet()
) {
    companion object {
        val EXERCISES = listOf(
            Exercise("step_ups", "Step-ups", reps = 10),
            Exercise("pistol_squats", "Assisted pistol squats", reps = 5),
            Exercise("wall_sit", "Wall sit", durationSeconds = 45, isTimed = true),
            Exercise("bicycle_crunches", "Bicycle crunches", reps = 20),
            Exercise("forearm_plank", "Forearm plank", durationSeconds = 40, isTimed = true),
            Exercise("lateral_leg_raises", "Lateral leg raises", reps = 15)
        )
        const val TOTAL_ROUNDS = 3
    }
}

data class BlockD(
    val isCompleted: Boolean = false,
    val completedMobility: Set<String> = emptySet(),
    val completedWristRehab: Set<String> = emptySet(),
    val wristRehabEnabled: Boolean = false
) {
    companion object {
        val MOBILITY_EXERCISES = listOf(
            Exercise("hip_mobility", "Hip mobility drills", durationSeconds = 180, isTimed = true),
            Exercise("ankle_mobility", "Ankle mobility drills", durationSeconds = 120, isTimed = true),
            Exercise("spine_mobility", "Spine mobility (cat-cow)", durationSeconds = 180, isTimed = true)
        )
        
        val WRIST_REHAB_EXERCISES = listOf(
            Exercise("hand_open_close", "Hand open/close", durationSeconds = 120, isTimed = true),
            Exercise("forearm_pronation", "Forearm pronation/supination", durationSeconds = 120, isTimed = true),
            Exercise("wrist_flexion", "Gentle wrist flexion/extension", durationSeconds = 180, isTimed = true)
        )
    }
}

data class DailyTrainingState(
    val date: String, // YYYY-MM-DD format
    val blockA: BlockA = BlockA(),
    val blockB: BlockB = BlockB(),
    val blockC: BlockC = BlockC(),
    val blockD: BlockD = BlockD(),
    val activeBlock: BlockType? = null, // Track which block is currently active/running
    val activeSessionId: String? = null, // Track the active session ID for cancellation/navigation
    val reminderTimes: Map<BlockType, String> = mapOf(
        BlockType.A to "08:00",
        BlockType.B to "12:00",
        BlockType.C to "16:00",
        BlockType.D to "20:00"
    )
) {
    fun getCompletionPercentage(): Float {
        val totalBlocks = 4
        var completedBlocks = 0
        
        if (blockA.isCompleted) completedBlocks++
        if (blockB.isCompleted) completedBlocks++
        if (blockC.isCompleted) completedBlocks++
        if (blockD.isCompleted) completedBlocks++
        
        return (completedBlocks.toFloat() / totalBlocks) * 100f
    }
    
    fun isToday(): Boolean {
        val today = java.time.LocalDate.now().toString()
        return date == today
    }
}

/**
 * Helper for serialization/deserialization
 */
object TrainingStateSerializer {
    private val gson = Gson()
    
    fun serialize(state: DailyTrainingState): String {
        return gson.toJson(state)
    }
    
    fun deserialize(json: String): DailyTrainingState? {
        return try {
            gson.fromJson(json, DailyTrainingState::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun serializeReminderTimes(times: Map<BlockType, String>): String {
        val map = times.mapKeys { it.key.name }
        return gson.toJson(map)
    }
    
    fun deserializeReminderTimes(json: String): Map<BlockType, String> {
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val map: Map<String, String> = gson.fromJson(json, type)
            map.mapKeys { BlockType.valueOf(it.key) }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
