package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.SyncRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SyncApiService {
    @POST(ApiConstants.API_PATH_SYNC)
    suspend fun syncData(
        @Body request: SyncRequest
    ): Response<SyncResponse>
}

