package com.samsung.android.health.sdk.sample.healthdiary.workout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines ORDER BY name")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE routineId = :routineId")
    suspend fun getRoutineByRoutineId(routineId: String): RoutineEntity?

    @Query("SELECT * FROM blocks WHERE routineId = :routineId ORDER BY orderIndex")
    fun getBlocksForRoutine(routineId: String): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks WHERE routineId = :routineId ORDER BY orderIndex")
    suspend fun getBlocksForRoutineSync(routineId: String): List<BlockEntity>

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Insert
    suspend fun insertBlock(block: BlockEntity): Long

    @Insert
    suspend fun insertBlocks(blocks: List<BlockEntity>)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteBlock(block: BlockEntity)

    @Query("DELETE FROM blocks WHERE routineId = :routineId")
    suspend fun deleteBlocksForRoutine(routineId: String)

    @Transaction
    suspend fun insertRoutineWithBlocks(routine: RoutineEntity, blocks: List<BlockEntity>) {
        insertRoutine(routine)
        if (blocks.isNotEmpty()) {
            insertBlocks(blocks)
        }
    }

    @Transaction
    suspend fun deleteRoutineAndBlocks(routine: RoutineEntity) {
        deleteBlocksForRoutine(routine.routineId)
        deleteRoutine(routine)
    }

    @Transaction
    suspend fun saveRoutineWithBlocks(routine: RoutineEntity, blocks: List<BlockEntity>) {
        val existing = getRoutineByRoutineId(routine.routineId)
        if (existing != null) {
            updateRoutine(routine.copy(id = existing.id))
            deleteBlocksForRoutine(routine.routineId)
        } else {
            insertRoutine(routine)
        }
        blocks.forEachIndexed { i, b ->
            insertBlock(b.copy(routineId = routine.routineId, orderIndex = i))
        }
    }
}
