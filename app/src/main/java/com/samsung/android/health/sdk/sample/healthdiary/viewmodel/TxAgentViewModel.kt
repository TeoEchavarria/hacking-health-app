package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.TxAgentApiService
import com.samsung.android.health.sdk.sample.healthdiary.api.TxAgentQueryRequest
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.TxAgentQueryEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.TxAgentResponseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class Message(
    val text: String,
    val isUser: Boolean,
    val reasoning: String? = null,
    val citations: List<String> = emptyList()
)

data class TxAgentUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TxAgentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val _uiState = MutableStateFlow(TxAgentUiState())
    val uiState: StateFlow<TxAgentUiState> = _uiState.asStateFlow()

    private val apiService: TxAgentApiService

    init {
        // Initialize Retrofit here for simplicity, ideally use DI
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/") // Emulator localhost
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(TxAgentApiService::class.java)
        
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val queries = database.txAgentQueryDao().getAll()
            // TODO: Load responses as well and reconstruct chat history
            // For now, start empty or load last few
        }
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        val userMessage = Message(text = query, isUser = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                // Save query to DB
                val queryId = database.txAgentQueryDao().insert(
                    TxAgentQueryEntity(
                        queryText = query,
                        queryType = "general",
                        status = "pending"
                    )
                )

                // Call API
                val response = apiService.queryTxAgent(TxAgentQueryRequest(query = query))

                // Save response to DB
                database.txAgentResponseDao().insert(
                    TxAgentResponseEntity(
                        queryId = queryId,
                        responseText = response.structuredOutput,
                        sources = response.citations.joinToString("\n") { "${it.source}: ${it.text}" }
                    )
                )
                
                database.txAgentQueryDao().markAsCompleted(queryId, System.currentTimeMillis())

                val botMessage = Message(
                    text = response.structuredOutput,
                    isUser = false,
                    reasoning = response.reasoningChain,
                    citations = response.citations.map { "${it.source}: ${it.text}" }
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + botMessage,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}
