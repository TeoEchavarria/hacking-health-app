package com.samsung.android.health.sdk.sample.healthdiary.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface OmhSyncApiService {
    /**
     * Sends OMH data to the backend
     * @param method The endpoint method (steps, sleep, heart-rate, etc.)
     * @param request The request body containing the OMH data as JsonObject
     */
    @POST("/sync/{method}")
    suspend fun syncOmhData(
        @Path("method") method: String,
        @Body request: OmhSyncRequest
    ): Response<OmhSyncResponse>
}

/**
 * Request wrapper for OMH sync
 * The data field will contain the serialized OMH data point as JsonObject
 */
@Serializable
data class OmhSyncRequest(
    val data: JsonObject
)

/**
 * Response from OMH sync endpoint
 */
@Serializable
data class OmhSyncResponse(
    val success: Boolean,
    val message: String? = null
)

