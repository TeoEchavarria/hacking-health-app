package com.samsung.android.health.sdk.sample.healthdiary.workout.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    fun getSession(sessionId: String): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE status = 'RUNNING' ORDER BY startedAt DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)
    
    @Query("UPDATE workout_sessions SET status = 'FINISHED' WHERE sessionId = :sessionId")
    suspend fun finishSession(sessionId: String)
}
