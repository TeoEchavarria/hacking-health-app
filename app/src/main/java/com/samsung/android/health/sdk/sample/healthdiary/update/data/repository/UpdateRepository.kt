package com.samsung.android.health.sdk.sample.healthdiary.update.data.repository

import com.samsung.android.health.sdk.sample.healthdiary.update.data.api.UpdateApi
import com.samsung.android.health.sdk.sample.healthdiary.update.data.model.UpdateResponse
import javax.inject.Inject
import javax.inject.Singleton

interface UpdateRepository {
    suspend fun getUpdates(): Result<UpdateResponse>
}

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val api: UpdateApi
) : UpdateRepository {
    override suspend fun getUpdates(): Result<UpdateResponse> {
        return try {
            val response = api.getUpdates()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
