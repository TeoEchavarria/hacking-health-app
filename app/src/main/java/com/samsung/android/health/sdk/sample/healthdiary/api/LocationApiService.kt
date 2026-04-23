package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.LocationUpdateRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LocationUpdateResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.PairedLocationResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SharingToggleRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SharingStatusResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LocationHistoryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API service for location tracking and sharing functionality.
 *
 * Allows paired users (caregiver <-> patient) to share their locations
 * with each other in real-time.
 *
 * Flow:
 * 1. App periodically calls updateLocation() to send GPS coordinates
 * 2. App calls getPairedLocation() to get the paired user's location
 * 3. Both locations are displayed on the shared map
 */
interface LocationApiService {

    /**
     * Update the authenticated user's current location.
     *
     * Should be called periodically (e.g., every 5 minutes) by the
     * location sync service.
     *
     * @param authorization Bearer token for authentication
     * @param request Location data (lat, lng, accuracy, timestamp)
     * @return Success status and server timestamp
     */
    @POST("/api/location/update")
    suspend fun updateLocation(
        @Header("Authorization") authorization: String,
        @Body request: LocationUpdateRequest
    ): Response<LocationUpdateResponse>

    /**
     * Get the location of the paired user (caregiver or patient).
     *
     * Returns location only if:
     * - An active pairing exists
     * - The paired user has sharing enabled
     * - The location is recent (within 30 minutes)
     *
     * @param authorization Bearer token for authentication
     * @return Paired user's location if available, or explanation message
     */
    @GET("/api/location/paired")
    suspend fun getPairedLocation(
        @Header("Authorization") authorization: String
    ): Response<PairedLocationResponse>

    /**
     * Toggle location sharing preference.
     *
     * When disabled, the paired user will not be able to see your location.
     *
     * @param authorization Bearer token for authentication
     * @param request Sharing preference (enabled/disabled)
     * @return Updated sharing status
     */
    @PATCH("/api/location/sharing")
    suspend fun toggleSharing(
        @Header("Authorization") authorization: String,
        @Body request: SharingToggleRequest
    ): Response<SharingStatusResponse>

    /**
     * Get current sharing status.
     *
     * @param authorization Bearer token for authentication
     * @return Current sharing status
     */
    @GET("/api/location/sharing")
    suspend fun getSharingStatus(
        @Header("Authorization") authorization: String
    ): Response<SharingStatusResponse>

    /**
     * Get location history for the authenticated user.
     *
     * @param authorization Bearer token for authentication
     * @param hours Number of hours to look back (default: 24)
     * @param limit Maximum number of locations to return (default: 100)
     * @return List of location records
     */
    @GET("/api/location/history")
    suspend fun getLocationHistory(
        @Header("Authorization") authorization: String,
        @Query("hours") hours: Int = 24,
        @Query("limit") limit: Int = 100
    ): Response<LocationHistoryResponse>
}
