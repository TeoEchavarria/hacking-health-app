package com.samsung.android.health.sdk.sample.healthdiary.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class TxAgentQueryRequest(
    @SerializedName("query") val query: String,
    @SerializedName("medical_history") val medicalHistory: String? = null,
    @SerializedName("pdf_summaries") val pdfSummaries: List<String>? = null
)

data class TxAgentResponseDto(
    @SerializedName("reasoning_chain") val reasoningChain: String,
    @SerializedName("structured_output") val structuredOutput: String,
    @SerializedName("citations") val citations: List<CitationDto>
)

data class CitationDto(
    @SerializedName("source") val source: String,
    @SerializedName("text") val text: String
)

interface TxAgentApiService {
    @POST("txagent/query")
    suspend fun queryTxAgent(@Body request: TxAgentQueryRequest): TxAgentResponseDto
}
