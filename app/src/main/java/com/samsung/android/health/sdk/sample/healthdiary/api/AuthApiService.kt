package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.RefreshRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST(ApiConstants.API_PATH_LOGIN)
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/auth/refresh")
    fun refresh(
        @Body request: RefreshRequest
    ): retrofit2.Call<LoginResponse>

    @POST("/auth/logout")
    suspend fun logout(
        @Body request: RefreshRequest
    ): Response<Map<String, Boolean>>
}

