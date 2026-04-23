package com.samsung.android.health.sdk.sample.healthdiary.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

/**
 * API service for Drawing Challenge feature.
 * Fetches random drawings from Quick Draw dataset.
 */
interface DrawingApiService {
    /**
     * Get a random drawing as PNG image.
     * 
     * Response headers include:
     * - X-Drawing-Category: The name of the category (e.g., "cat", "house", "bicycle")
     * 
     * @return Response containing PNG image bytes
     */
    @GET("/drawing-challenge/random")
    suspend fun getRandomDrawing(): Response<ResponseBody>
}
