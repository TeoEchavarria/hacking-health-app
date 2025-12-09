package com.samsung.android.health.sdk.sample.healthdiary.data.ingest

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IngestDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBatch(batch: ReceivedBatchEntity): Long

    @Query("SELECT MAX(seq) FROM received_batches")
    suspend fun getMaxReceivedSeq(): Long?

    @Query("SELECT MAX(seq) FROM received_batches")
    fun getMaxReceivedSeqFlow(): Flow<Long?>

    // Debugging
    @Query("SELECT COUNT(*) FROM received_batches")
    fun getBatchCount(): Flow<Int>
}
