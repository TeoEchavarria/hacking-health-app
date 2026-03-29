package com.samsung.android.health.sdk.sample.healthdiary.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.samsung.android.health.sdk.sample.healthdiary.utils.TelemetryLogger
import com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig

import com.samsung.android.health.sdk.sample.healthdiary.update.data.api.UpdateApi
import com.samsung.android.health.sdk.sample.healthdiary.update.data.api.GitHubReleaseApi

object RetrofitClient {
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    
    // Simple interceptor that only adds Content-Type header (no auth)
    private val contentTypeInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header(ApiConstants.HEADER_CONTENT_TYPE, ApiConstants.CONTENT_TYPE_JSON)
            .build()
        chain.proceed(newRequest)
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        
        override fun log(message: String) {
            val timestamp = dateFormat.format(Date())
            Log.d("API_REQUEST", "[$timestamp] $message")
        }
    }).apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val customLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        val requestStartTime = System.currentTimeMillis()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        
        // Log request details
        Log.i("API_REQUEST", "═══════════════════════════════════════════════════════════")
        Log.i("API_REQUEST", "[$timestamp] ${request.method} ${request.url}")
        TelemetryLogger.log("API", "Request", "${request.method} ${request.url}")
        
        Log.i("API_REQUEST", "Request Headers:")
        request.headers.forEach { header ->
            val headerValue = if (header.first.equals("Authorization", ignoreCase = true)) {
                // Ocultar parte del token por seguridad
                val token = header.second
                if (token.startsWith("Bearer ")) {
                    val tokenValue = token.substring(7)
                    if (tokenValue.length > 10) {
                        "Bearer ${tokenValue.take(6)}...${tokenValue.takeLast(4)}"
                    } else {
                        "Bearer ***"
                    }
                } else {
                    header.second
                }
            } else {
                header.second
            }
            Log.i("API_REQUEST", "  ${header.first}: $headerValue")
        }
        
        // Log request body if present
        val requestBody = request.body
        if (requestBody != null) {
            try {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                val bodyString = buffer.readUtf8()
                if (bodyString.isNotEmpty()) {
                    Log.i("API_REQUEST", "Request Body:")
                    // Intentar formatear JSON
                    try {
                        val jsonElement: JsonElement = JsonParser.parseString(bodyString)
                        val formattedJson = GsonBuilder().setPrettyPrinting().create().toJson(jsonElement)
                        formattedJson.lines().forEach { line ->
                            Log.i("API_REQUEST", "  $line")
                        }
                    } catch (e: Exception) {
                        Log.i("API_REQUEST", "  $bodyString")
                    }
                }
            } catch (e: IOException) {
                Log.e("API_REQUEST", "Error reading request body: ${e.message}")
            }
        }
        
        // Execute request
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            val requestTime = System.currentTimeMillis() - requestStartTime
            Log.e("API_ERROR", "═══════════════════════════════════════════════════════════")
            Log.e("API_ERROR", "[$timestamp] Request failed after ${requestTime}ms")
            Log.e("API_ERROR", "Error: ${e.message}")
            Log.e("API_ERROR", "Stack trace: ${e.stackTraceToString()}")
            Log.e("API_ERROR", "═══════════════════════════════════════════════════════════")
            TelemetryLogger.log("API", "Error", "Request failed: ${e.message}")
            throw e
        }
        
        val requestTime = System.currentTimeMillis() - requestStartTime
        val responseTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        
        // Log response details
        Log.i("API_RESPONSE", "═══════════════════════════════════════════════════════════")
        Log.i("API_RESPONSE", "[$responseTimestamp] Response for ${request.method} ${request.url}")
        Log.i("API_RESPONSE", "Status Code: ${response.code} ${response.message}")
        Log.i("API_RESPONSE", "Response Time: ${requestTime}ms")
        TelemetryLogger.log("API", "Response", "${response.code} ${response.message} (${requestTime}ms)")

        Log.i("API_RESPONSE", "Response Headers:")
        response.headers.forEach { header ->
            Log.i("API_RESPONSE", "  ${header.first}: ${header.second}")
        }
        
        // Log response body
        val responseBody = response.peekBody(1024 * 1024) // Peek up to 1MB
        val responseBodyString = responseBody.string()
        if (responseBodyString.isNotEmpty()) {
            Log.i("API_RESPONSE", "Response Body:")
            try {
                val jsonElement: JsonElement = JsonParser.parseString(responseBodyString)
                val formattedJson = GsonBuilder().setPrettyPrinting().create().toJson(jsonElement)
                formattedJson.lines().forEach { line ->
                    Log.i("API_RESPONSE", "  $line")
                }
            } catch (e: Exception) {
                // Si no es JSON, mostrar como texto
                responseBodyString.lines().take(50).forEach { line ->
                    Log.i("API_RESPONSE", "  $line")
                }
                if (responseBodyString.lines().size > 50) {
                    Log.i("API_RESPONSE", "  ... (truncated)")
                }
            }
        }
        
        if (!response.isSuccessful) {
            Log.e("API_ERROR", "Request failed with status ${response.code}")
            Log.e("API_ERROR", "Error response: $responseBodyString")
            TelemetryLogger.log("API", "Error", "Status ${response.code}: $responseBodyString")
        }
        
        Log.i("API_RESPONSE", "═══════════════════════════════════════════════════════════")
        
        response
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(contentTypeInterceptor)
        .addInterceptor(customLoggingInterceptor)
        .addInterceptor(loggingInterceptor)
        // No authenticator - auth disabled for dev/testing
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig.getApiBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    // Kotlinx Serialization JSON instance for OMH
    private val omhJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    
    // Separate Retrofit instance for OMH sync using Kotlinx Serialization
    private val omhRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(com.samsung.android.health.sdk.sample.healthdiary.config.DeviceConfig.getApiBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(omhJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val syncApiService: SyncApiService by lazy { retrofit.create(SyncApiService::class.java) }
    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val omhSyncApiService: OmhSyncApiService by lazy { omhRetrofit.create(OmhSyncApiService::class.java) }
    val updateApi: UpdateApi by lazy { retrofit.create(UpdateApi::class.java) }
    val pairingApiService: PairingApiService by lazy { retrofit.create(PairingApiService::class.java) }
    
    // GitHub API client for releases (OTA updates)
    private val githubOkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val githubRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GitHubReleaseApi.BASE_URL)
            .client(githubOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    val gitHubReleaseApi: GitHubReleaseApi by lazy { 
        githubRetrofit.create(GitHubReleaseApi::class.java) 
    }
}

