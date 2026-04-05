package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.FullUserProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * API service for user-related operations.
 * 
 * All endpoints require Authorization header with Bearer token.
 */
interface UserApiService {
    
    /**
     * Get full user profile with inferred role and connections.
     * 
     * Role is inferred from active pairings:
     * - "caregiver": User has active pairings as caregiver
     * - "patient": User has active pairings as patient
     * - "none": No active pairings
     * 
     * Connections include:
     * - For caregivers: List of patients they care for
     * - For patients: List of their caregivers
     * 
     * @param authorization Bearer token
     * @return Full user profile with role and connections
     */
    @GET("/user/profile/full")
    suspend fun getFullProfile(
        @Header("Authorization") authorization: String
    ): Response<FullUserProfileResponse>
}
