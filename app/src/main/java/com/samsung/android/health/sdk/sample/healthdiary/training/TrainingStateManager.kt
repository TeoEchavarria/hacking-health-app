package com.samsung.android.health.sdk.sample.healthdiary.training

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import com.google.gson.Gson
import java.io.File

/**
 * Manages daily training state persistence and reset logic
 */
class TrainingStateManager(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val debugGson = Gson()

    // #region agent log
    private fun debugLog(hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
        try {
            val payload = mapOf(
                "sessionId" to "debug-session",
                "runId" to "pre-fix",
                "hypothesisId" to hypothesisId,
                "location" to "TrainingStateManager.kt",
                "message" to message,
                "data" to data,
                "timestamp" to System.currentTimeMillis()
            )
            File("/Users/teoechavarria/Documents/hh/.cursor/debug.log")
                .appendText(debugGson.toJson(payload) + "\n")
        } catch (_: Exception) {
            // best-effort logging only
        }
    }
    // #endregion
    
    companion object {
        private const val PREFS_NAME = "training_state"
        private const val KEY_CURRENT_STATE = "current_state"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_REMINDER_TIMES = "reminder_times"
    }
    
    /**
     * Get or create today's training state
     */
    fun getTodayState(): DailyTrainingState {
        val today = LocalDate.now().toString()
        val lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, null)
        
        // Reset if it's a new day
        if (lastResetDate != today) {
            resetForToday()
        }
        
        // Load current state
        val stateJson = prefs.getString(KEY_CURRENT_STATE, null)
        val state = if (stateJson != null) {
            TrainingStateSerializer.deserialize(stateJson) ?: createDefaultState(today)
        } else {
            createDefaultState(today)
        }
        
        // Ensure state date matches today
        return if (state.date == today) {
            state
        } else {
            createDefaultState(today)
        }
    }
    
    /**
     * Save current state
     */
    fun saveState(state: DailyTrainingState) {
        prefs.edit()
            .putString(KEY_CURRENT_STATE, TrainingStateSerializer.serialize(state))
            .putString(KEY_LAST_RESET_DATE, state.date)
            .apply()
    }
    
    /**
     * Reset state for today
     */
    fun resetForToday() {
        val today = LocalDate.now().toString()
        val reminderTimes = getReminderTimes()
        val newState = DailyTrainingState(
            date = today,
            reminderTimes = reminderTimes
        )
        saveState(newState)
    }
    
    /**
     * Get reminder times
     */
    fun getReminderTimes(): Map<BlockType, String> {
        val json = prefs.getString(KEY_REMINDER_TIMES, null)
        val times = if (json != null) {
            TrainingStateSerializer.deserializeReminderTimes(json)
        } else {
            mapOf(
                BlockType.A to "08:00",
                BlockType.B to "12:00",
                BlockType.C to "16:00",
                BlockType.D to "20:00"
            )
        }
        // #region agent log
        debugLog("H4", "getReminderTimes", mapOf("count" to times.size))
        // #endregion
        return times
    }
    
    /**
     * Save reminder times
     */
    fun saveReminderTimes(times: Map<BlockType, String>) {
        prefs.edit()
            .putString(KEY_REMINDER_TIMES, TrainingStateSerializer.serializeReminderTimes(times))
            .apply()
    }
    
    /**
     * Set the currently active block
     */
    fun setActiveBlock(blockType: BlockType?) {
        val state = getTodayState()
        saveState(state.copy(activeBlock = blockType))
    }
    
    /**
     * Set the currently active session ID
     */
    fun setActiveWorkoutSession(sessionId: String?) {
        val state = getTodayState()
        saveState(state.copy(activeSessionId = sessionId))
    }

    /**
     * Update block A completion
     */
    fun updateBlockA(blockA: BlockA) {
        val state = getTodayState()
        saveState(state.copy(blockA = blockA))
    }
    
    /**
     * Update block B completion
     */
    fun updateBlockB(blockB: BlockB) {
        val state = getTodayState()
        saveState(state.copy(blockB = blockB))
    }
    
    /**
     * Update block C completion
     */
    fun updateBlockC(blockC: BlockC) {
        val state = getTodayState()
        saveState(state.copy(blockC = blockC))
    }
    
    /**
     * Update block D completion
     */
    fun updateBlockD(blockD: BlockD) {
        val state = getTodayState()
        saveState(state.copy(blockD = blockD))
    }
    
    /**
     * Toggle exercise completion in a block
     */
    fun toggleExercise(blockType: BlockType, exerciseId: String) {
        val state = getTodayState()
        when (blockType) {
            BlockType.A -> {
                val blockA = state.blockA
                val newCompletedSegments = if (blockA.completedSegments.contains(exerciseId)) {
                    blockA.completedSegments - exerciseId
                } else {
                    blockA.completedSegments + exerciseId
                }
                val option = when (blockA.selectedMode) {
                    CardioMode.CYCLING -> BlockA.CYCLING_OPTION
                    CardioMode.RUNNING -> BlockA.RUNNING_OPTION
                    CardioMode.SWIMMING -> BlockA.SWIMMING_OPTION
                }
                val allSegmentsCompleted = option.segments.all { 
                    newCompletedSegments.contains(it.name) 
                }
                updateBlockA(blockA.copy(
                    completedSegments = newCompletedSegments,
                    isCompleted = allSegmentsCompleted
                ))
            }
            BlockType.B -> {
                val blockB = state.blockB
                val wasCompleted = blockB.completedExercises.contains(exerciseId)
                val newCompleted = if (wasCompleted) {
                    blockB.completedExercises - exerciseId
                } else {
                    blockB.completedExercises + exerciseId
                }
                val allExercisesInRound = BlockB.EXERCISES.all { newCompleted.contains(it.id) }
                val wasAllCompleted = BlockB.EXERCISES.all { blockB.completedExercises.contains(it.id) }
                
                // If we just completed all exercises (not unchecking), increment round
                val (newRounds, finalCompleted) = if (allExercisesInRound && !wasAllCompleted && !wasCompleted && blockB.completedRounds < BlockB.TOTAL_ROUNDS) {
                    // Completed a round - increment and clear for next round
                    Pair(blockB.completedRounds + 1, emptySet<String>())
                } else {
                    Pair(blockB.completedRounds, newCompleted)
                }
                
                updateBlockB(blockB.copy(
                    completedExercises = finalCompleted,
                    completedRounds = newRounds,
                    isCompleted = newRounds >= BlockB.TOTAL_ROUNDS
                ))
            }
            BlockType.C -> {
                val blockC = state.blockC
                val wasCompleted = blockC.completedExercises.contains(exerciseId)
                val newCompleted = if (wasCompleted) {
                    blockC.completedExercises - exerciseId
                } else {
                    blockC.completedExercises + exerciseId
                }
                val allExercisesInRound = BlockC.EXERCISES.all { newCompleted.contains(it.id) }
                val wasAllCompleted = BlockC.EXERCISES.all { blockC.completedExercises.contains(it.id) }
                
                // If we just completed all exercises (not unchecking), increment round
                val (newRounds, finalCompleted) = if (allExercisesInRound && !wasAllCompleted && !wasCompleted && blockC.completedRounds < BlockC.TOTAL_ROUNDS) {
                    // Completed a round - increment and clear for next round
                    Pair(blockC.completedRounds + 1, emptySet<String>())
                } else {
                    Pair(blockC.completedRounds, newCompleted)
                }
                
                updateBlockC(blockC.copy(
                    completedExercises = finalCompleted,
                    completedRounds = newRounds,
                    isCompleted = newRounds >= BlockC.TOTAL_ROUNDS
                ))
            }
            BlockType.D -> {
                val blockD = state.blockD
                val isMobility = BlockD.MOBILITY_EXERCISES.any { it.id == exerciseId }
                if (isMobility) {
                    val newCompleted = if (blockD.completedMobility.contains(exerciseId)) {
                        blockD.completedMobility - exerciseId
                    } else {
                        blockD.completedMobility + exerciseId
                    }
                    val allMobilityCompleted = BlockD.MOBILITY_EXERCISES.all { 
                        newCompleted.contains(it.id) 
                    }
                    updateBlockD(blockD.copy(
                        completedMobility = newCompleted,
                        isCompleted = allMobilityCompleted && 
                            (!blockD.wristRehabEnabled || blockD.completedWristRehab.size == BlockD.WRIST_REHAB_EXERCISES.size)
                    ))
                } else {
                    val newCompleted = if (blockD.completedWristRehab.contains(exerciseId)) {
                        blockD.completedWristRehab - exerciseId
                    } else {
                        blockD.completedWristRehab + exerciseId
                    }
                    val allWristCompleted = BlockD.WRIST_REHAB_EXERCISES.all { 
                        newCompleted.contains(it.id) 
                    }
                    updateBlockD(blockD.copy(
                        completedWristRehab = newCompleted,
                        isCompleted = blockD.completedMobility.size == BlockD.MOBILITY_EXERCISES.size && 
                            (!blockD.wristRehabEnabled || allWristCompleted)
                    ))
                }
            }
        }
    }
    
    private fun createDefaultState(date: String): DailyTrainingState {
        val reminderTimes = getReminderTimes()
        return DailyTrainingState(
            date = date,
            reminderTimes = reminderTimes
        )
    }
}
