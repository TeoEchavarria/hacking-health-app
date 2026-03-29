package com.samsung.android.health.sdk.sample.healthdiary.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.PairingEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing pairing data (caregiver-patient links).
 */
@Dao
interface PairingDao {

    /**
     * Get all pairings for the current user.
     * Returns a Flow that emits updates when pairings change.
     */
    @Query("SELECT * FROM pairings WHERE status = 'active' ORDER BY activatedAt DESC")
    fun getAllActive(): Flow<List<PairingEntity>>
    
    /**
     * Get all active pairings synchronously.
     */
    @Query("SELECT * FROM pairings WHERE status = 'active' ORDER BY activatedAt DESC")
    suspend fun getAllActiveSync(): List<PairingEntity>

    /**
     * Get a specific pairing by ID.
     */
    @Query("SELECT * FROM pairings WHERE pairingId = :pairingId")
    suspend fun getById(pairingId: String): PairingEntity?
    
    /**
     * Get the most recent pending pairing code for the patient.
     * Used to resume showing a previously generated code.
     */
    @Query("SELECT * FROM pairings WHERE status = 'pending' AND userRole = 'patient' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastPendingPatientCode(): PairingEntity?
    
    /**
     * Get all caregivers linked to the current patient.
     */
    @Query("SELECT * FROM pairings WHERE userRole = 'patient' AND status = 'active' ORDER BY activatedAt DESC")
    fun getPatientCaregivers(): Flow<List<PairingEntity>>
    
    /**
     * Get all patients linked to the current caregiver.
     */
    @Query("SELECT * FROM pairings WHERE userRole = 'caregiver' AND status = 'active' ORDER BY activatedAt DESC")
    fun getCaregiverPatients(): Flow<List<PairingEntity>>

    /**
     * Insert a new pairing.
     * Replaces existing pairing with same ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pairing: PairingEntity)

    /**
     * Update an existing pairing.
     */
    @Update
    suspend fun update(pairing: PairingEntity)

    /**
     * Update pairing status.
     */
    @Query("UPDATE pairings SET status = :status, updatedAt = :updatedAt WHERE pairingId = :pairingId")
    suspend fun updateStatus(
        pairingId: String,
        status: String,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Mark a pending code as active after validation.
     */
    @Query("""
        UPDATE pairings 
        SET status = 'active', 
            caregiverId = :caregiverId,
            caregiverName = :caregiverName,
            activatedAt = :activatedAt,
            updatedAt = :updatedAt 
        WHERE pairingId = :pairingId
    """)
    suspend fun activatePairing(
        pairingId: String,
        caregiverId: String,
        caregiverName: String,
        activatedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Delete a pairing.
     */
    @Delete
    suspend fun delete(pairing: PairingEntity)

    /**
     * Delete a pairing by ID.
     */
    @Query("DELETE FROM pairings WHERE pairingId = :pairingId")
    suspend fun deleteById(pairingId: String)
    
    /**
     * Delete all expired pending codes.
     */
    @Query("DELETE FROM pairings WHERE status = 'pending' AND expiresAt < :currentTime")
    suspend fun deleteExpiredCodes(currentTime: Long = System.currentTimeMillis())

    /**
     * Delete all pairings (for testing/reset).
     */
    @Query("DELETE FROM pairings")
    suspend fun deleteAll()
}
