package com.samsung.android.health.sdk.sample.healthdiary.update.data.api

import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.UpdateResponse
import retrofit2.Response
import retrofit2.http.GET

interface UpdateApi {
    @GET("/app/updates")
    suspend fun getUpdates(): Response<UpdateResponse>
}
