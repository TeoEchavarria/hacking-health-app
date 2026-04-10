package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.CreatePairingCodeRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CreatePairingCodeResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PairingStatusResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ValidatePairingCodeRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ValidatePairingCodeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API service for family pairing/linking functionality.
 * 
 * Allows caregivers and patients to link their accounts via a 6-digit code.
 * 
 * Flow:
 * 1. Patient calls createPairingCode() to generate a code
 * 2. Patient polls checkPairingStatus() to see when code is used
 * 3. Caregiver calls validatePairingCode() with the code
 * 4. Backend links the accounts
 */
interface PairingApiService {
    
    /**
     * Create a new pairing code for a patient.
     * Patient generates this code to share with their caregiver.
     * 
     * Requires Authorization header with Bearer token (patient).
     * 
     * @return 6-digit code valid for 10 minutes
     */
    @POST("/api/pairing/create")
    suspend fun createPairingCode(
        @Header("Authorization") authorization: String,
        @Body request: CreatePairingCodeRequest = CreatePairingCodeRequest()
    ): Response<CreatePairingCodeResponse>
    
    /**
     * Validate a pairing code entered by a caregiver.
     * Links caregiver account with patient account.
     * 
     * Requires Authorization header with Bearer token (caregiver).
     * 
     * @param request Contains the 6-digit code
     * @return Success status and patient info if valid
     */
    @POST("/api/pairing/validate")
    suspend fun validatePairingCode(
        @Header("Authorization") authorization: String,
        @Body request: ValidatePairingCodeRequest
    ): Response<ValidatePairingCodeResponse>
    
    /**
     * Check the status of a pairing code (for polling).
     * Patient calls this to see if caregiver has used the code.
     * 
     * Requires Authorization header with Bearer token (patient).
     * 
     * @param pairingId The ID returned when code was created
     * @return Current status: "pending", "active", "expired", "used"
     */
    @GET("/api/pairing/{pairingId}/status")
    suspend fun checkPairingStatus(
        @Header("Authorization") authorization: String,
        @Path("pairingId") pairingId: String
    ): Response<PairingStatusResponse>
    
    /**
     * List all active pairings for the current user.
     * Used to sync pairings from backend to local database on startup.
     * 
     * @param role "patient" or "caregiver" - determines which pairings to fetch
     * @return List of active pairings for the user
     */
    @GET("/api/pairing/user/list")
    suspend fun listUserPairings(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Query("role") role: String
    ): Response<com.samsung.android.health.sdk.sample.healthdiary.api.models.ListUserPairingsResponse>
    
    /**
     * Revoke an active pairing.
     * User must be either the patient or caregiver in the pairing.
     * 
     * @param pairingId ID of the pairing to revoke
     * @return Success status and message
     */
    @POST("/api/pairing/{pairingId}/revoke")
    suspend fun revokePairing(
        @Header("Authorization") authorization: String,
        @Path("pairingId") pairingId: String
    ): Response<com.samsung.android.health.sdk.sample.healthdiary.api.models.RevokePairingResponse>
}
