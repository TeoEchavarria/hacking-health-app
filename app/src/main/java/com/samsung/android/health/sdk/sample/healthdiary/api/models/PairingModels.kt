package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request to create a new pairing code.
 * Patient generates this code to share with their caregiver.
 */
data class CreatePairingCodeRequest(
    @SerializedName("patientId")
    val patientId: String? = null // Will be inferred from auth token on backend
)

/**
 * Response containing the generated pairing code.
 */
data class CreatePairingCodeResponse(
    @SerializedName("pairingId")
    val pairingId: String,
    @SerializedName("code")
    val code: String, // 6-digit numeric code (e.g., "582910")
    @SerializedName("expiresAt")
    val expiresAt: Long, // Unix timestamp in milliseconds
    @SerializedName("createdAt")
    val createdAt: Long
)

/**
 * Request to validate a pairing code.
 * Caregiver submits code to link with patient.
 */
data class ValidatePairingCodeRequest(
    @SerializedName("code")
    val code: String, // 6-digit code entered by caregiver
    @SerializedName("caregiverId")
    val caregiverId: String? = null // Will be inferred from auth token on backend
)

/**
 * Response after validating pairing code.
 */
data class ValidatePairingCodeResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("pairingId")
    val pairingId: String?,
    @SerializedName("patientId")
    val patientId: String?,
    @SerializedName("patientName")
    val patientName: String?,
    @SerializedName("error")
    val error: String? = null
)

/**
 * Response for checking pairing status (polling).
 */
data class PairingStatusResponse(
    @SerializedName("status")
    val status: String, // "pending", "active", "expired", "used"
    @SerializedName("linked")
    val linked: Boolean,
    @SerializedName("caregiverId")
    val caregiverId: String? = null,
    @SerializedName("caregiverName")
    val caregiverName: String? = null
)

/**
 * Response for listing user pairings.
 */
data class ListUserPairingsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("count")
    val count: Int,
    @SerializedName("pairings")
    val pairings: List<PairingItem>
)

/**
 * Individual pairing item from the backend.
 */
data class PairingItem(
    @SerializedName("_id")
    val id: String,
    @SerializedName("patientId")
    val patientId: String?,
    @SerializedName("patientName")
    val patientName: String?,
    @SerializedName("caregiverId")
    val caregiverId: String?,
    @SerializedName("caregiverName")
    val caregiverName: String?,
    @SerializedName("status")
    val status: String,
    @SerializedName("createdAt")
    val createdAt: Long?,
    @SerializedName("activatedAt")
    val activatedAt: Long?
)

/**
 * Response after revoking a pairing.
 */
data class RevokePairingResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null
)

