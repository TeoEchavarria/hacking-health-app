package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.AudioParseResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.BloodPressureRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.BloodPressureResponse
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ParseBPVoiceRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.VoiceParseResult
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * API service for blood pressure monitoring endpoints.
 */
interface BloodPressureApiService {
    
    /**
     * Upload a single blood pressure reading.
     *
     * @param token Authorization token (Bearer format)
     * @param idempotencyKey Unique key to prevent duplicate entries on retry
     * @param request Blood pressure reading data
     * @return Classification result with stage/severity
     */
    @POST("health/blood-pressure")
    suspend fun uploadBloodPressure(
        @Header("Authorization") token: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: BloodPressureRequest
    ): BloodPressureResponse
    
    /**
     * Parse voice transcription to extract BP values using LLM.
     *
     * @param token Authorization token (Bearer format)
     * @param request Voice transcription to parse
     * @return Extracted BP values with confidence level
     */
    @POST("health/parse-bp-voice")
    suspend fun parseBPVoice(
        @Header("Authorization") token: String,
        @Body request: ParseBPVoiceRequest
    ): VoiceParseResult
    
    /**
     * Parse audio file to extract BP values.
     * Uses Whisper for transcription + LLM for extraction.
     *
     * @param token Authorization token (Bearer format)
     * @param audio Audio file as multipart
     * @return Extracted BP values + transcription
     */
    @Multipart
    @POST("health/parse-bp-audio")
    suspend fun parseBPAudio(
        @Header("Authorization") token: String,
        @Part audio: MultipartBody.Part
    ): AudioParseResult
}
