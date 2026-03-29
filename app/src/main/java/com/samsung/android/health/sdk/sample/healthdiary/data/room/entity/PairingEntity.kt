package com.samsung.android.health.sdk.sample.healthdiary.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing family pairings (caregiver-patient links).
 * 
 * Maintains the relationship between caregivers and patients,
 * along with pairing code metadata and status.
 */
@Entity(
    tableName = "pairings",
    indices = [
        Index(value = ["pairingId"], unique = true),
        Index(value = ["code"]),
        Index(value = ["caregiverId"]),
        Index(value = ["patientId"])
    ]
)
data class PairingEntity(
    @PrimaryKey
    val pairingId: String,
    
    /** ID of the caregiver (person monitoring) */
    val caregiverId: String? = null,
    
    /** Name of the caregiver */
    val caregiverName: String? = null,
    
    /** ID of the patient (person being monitored) */
    val patientId: String? = null,
    
    /** Name of the patient */
    val patientName: String? = null,
    
    /** 6-digit numeric code (only stored for pending codes on patient side) */
    val code: String? = null,
    
    /**
     * Pairing status:
     * - "pending": Code created, waiting for caregiver to use it
     * - "active": Code validated, pairing is active
     * - "expired": Code expired without being used
     * - "revoked": Pairing manually removed by user
     */
    val status: String,
    
    /** Role of the current user in this pairing: "caregiver" or "patient" */
    val userRole: String,
    
    /** Timestamp when the pairing code was created */
    val createdAt: Long,
    
    /** Timestamp when the code expires (only for pending) */
    val expiresAt: Long? = null,
    
    /** Timestamp when the pairing became active (code was validated) */
    val activatedAt: Long? = null,
    
    /** Timestamp when this record was last updated */
    val updatedAt: Long = System.currentTimeMillis()
)
