package com.samsung.android.health.sdk.sample.healthdiary.workout.data

import android.content.Context
import androidx.room.withTransaction
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import java.util.UUID

object DailyRoutinesSeeder {

    suspend fun seed(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val routineDao = db.routineDao()

        db.withTransaction {
            seedRoutine(routineDao, "daily_block_c", "Block C - Calisthenics", blockC)
            seedRoutine(routineDao, "daily_block_d", "Block D - Mobility + Recovery", blockD)
            seedRoutine(routineDao, "daily_block_b_30", "Block B - Lower Body + Core (30s)", blockB(30))
            seedRoutine(routineDao, "daily_block_b_45", "Block B - Lower Body + Core (45s)", blockB(45))
            seedRoutine(routineDao, "daily_block_a_cycling", "Block A - Cardio Base (Cycling)", blockACycling)
            seedRoutine(routineDao, "daily_block_a_running", "Block A - Cardio Base (Running)", blockARunning)
            seedRoutine(routineDao, "daily_block_a_swimming", "Block A - Cardio Base (Swimming)", blockASwimming)
        }
    }

    private suspend fun seedRoutine(
        dao: RoutineDao,
        routineId: String,
        name: String,
        blocks: List<BlockEntity>
    ) {
        val existing = dao.getRoutineByRoutineId(routineId)
        if (existing == null) {
            dao.insertRoutine(RoutineEntity(routineId = routineId, name = name))
            
            val blocksWithIds = blocks.mapIndexed { index, block ->
                block.copy(
                    blockId = UUID.randomUUID().toString(),
                    routineId = routineId,
                    orderIndex = index
                )
            }
            dao.insertBlocks(blocksWithIds)
        }
    }

    private val blockC = listOf(
        createBlock("Step-ups", 3, 0f, 10, 30),
        createBlock("Assisted pistol squats", 3, 0f, 5, 30),
        createBlock("Wall sit", 3, 0f, null, 30),
        createBlock("Bicycle crunches", 3, 0f, 20, 30),
        createBlock("Forearm plank", 3, 0f, null, 30),
        createBlock("Lateral leg raises", 3, 0f, 15, 30)
    )

    private val blockD = listOf(
        createBlock("Hip mobility drills", 1, 0f, null, 0),
        createBlock("Ankle mobility drills", 1, 0f, null, 0),
        createBlock("Spine mobility (cat-cow)", 1, 0f, null, 0),
        createBlock("Hand open/close", 1, 0f, null, 0),
        createBlock("Forearm pronation", 1, 0f, null, 0),
        createBlock("Gentle wrist flexion", 1, 0f, null, 0)
    )

    private fun blockB(restSec: Int) = listOf(
        createBlock("Squats", 3, 0f, 15, restSec),
        createBlock("Alternating lunges", 3, 0f, 10, restSec),
        createBlock("Glute bridges", 3, 0f, 20, restSec),
        createBlock("Calf raises", 3, 0f, 20, restSec),
        createBlock("Hollow body hold", 3, 0f, null, restSec),
        createBlock("Superman hold", 3, 0f, null, restSec)
    )

    private val blockACycling = listOf(
        createBlock("Easy pace", 1, 0f, null, 0),
        createBlock("Moderate pace", 1, 0f, null, 0),
        createBlock("Easy pace", 1, 0f, null, 0)
    )

    private val blockARunning = listOf(
        createBlock("Brisk walk", 1, 0f, null, 0),
        createBlock("Easy continuous run", 1, 0f, null, 0),
        createBlock("Walking", 1, 0f, null, 0)
    )

    private val blockASwimming = listOf(
        createBlock("Kick with board", 1, 0f, null, 0),
        createBlock("Backstroke (easy)", 1, 0f, null, 0),
        createBlock("Relaxed freestyle", 1, 0f, null, 0)
    )

    private fun createBlock(name: String, sets: Int, weight: Float, reps: Int?, rest: Int): BlockEntity {
        return BlockEntity(
            blockId = "", // assigned later
            routineId = "", // assigned later
            exerciseName = name,
            sets = sets,
            targetWeight = weight,
            targetReps = reps,
            restSec = rest,
            orderIndex = 0
        )
    }
}
