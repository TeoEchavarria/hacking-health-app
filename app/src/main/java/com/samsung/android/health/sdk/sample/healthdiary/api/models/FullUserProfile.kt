package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

/**
 * Information about a caregiver/patient connection.
 */
data class ConnectionInfo(
    @SerializedName("user_id") val userId: String,
    val name: String,
    val role: String,  // "caregiver" or "patient" - what this person is to the current user
    @SerializedName("profile_picture") val profilePicture: String? = null
)

/**
 * Complete user profile with inferred role and connections.
 * 
 * Role is inferred from active pairings:
 * - "caregiver": User has active pairings as caregiver
 * - "patient": User has active pairings as patient
 * - "none": No active pairings
 */
data class FullUserProfileResponse(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
    val role: String,  // "caregiver", "patient", or "none"
    val connections: List<ConnectionInfo> = emptyList(),
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
) {
    /**
     * Get patients that this user cares for (when user is caregiver).
     */
    fun getPatients(): List<ConnectionInfo> = connections.filter { it.role == "patient" }
    
    /**
     * Get caregivers that care for this user (when user is patient).
     */
    fun getCaregivers(): List<ConnectionInfo> = connections.filter { it.role == "caregiver" }
    
    /**
     * Check if user is a caregiver.
     */
    fun isCaregiver(): Boolean = role == "caregiver"
    
    /**
     * Check if user is a patient.
     */
    fun isPatient(): Boolean = role == "patient"
    
    /**
     * Get primary patient ID (first patient if caregiver has multiple).
     */
    fun getPrimaryPatientId(): String? = getPatients().firstOrNull()?.userId
}
