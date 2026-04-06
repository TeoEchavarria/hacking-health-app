package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.HealthMetricsRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.HealthMetricsResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * API service for uploading health metrics to backend.
 */
interface HealthMetricsApiService {
    
    /**
     * Upload health metrics from watch (via phone app).
     * Called on:
     * - Immediate sync when watch sends data
     * - Periodic sync every 15 minutes
     */
    @POST("health/metrics")
    suspend fun uploadHealthMetrics(
        @Header("Authorization") token: String,
        @Body metrics: HealthMetricsRequest
    ): HealthMetricsResponse
}
