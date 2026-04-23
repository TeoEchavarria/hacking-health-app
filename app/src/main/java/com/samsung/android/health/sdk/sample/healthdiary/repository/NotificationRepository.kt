package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.util.Log
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.*
import com.samsung.android.health.sdk.sample.healthdiary.model.Notification
import com.samsung.android.health.sdk.sample.healthdiary.model.NotificationType
import com.samsung.android.health.sdk.sample.healthdiary.model.NotificationPriority
import com.samsung.android.health.sdk.sample.healthdiary.utils.AuthEventBus
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Repository for managing notifications and health tips.
 * 
 * Provides operations to fetch notifications, mark them as read, and get health tips.
 * All operations are scoped to the authenticated user.
 */
class NotificationRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationRepository"
        
        // ISO 8601 date format from API
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }
    
    private val apiService = RetrofitClient.notificationApiService
    
    /**
     * Get all notifications for the current user.
     * 
     * @param notificationType Optional filter by type
     * @param includeRead Include read notifications (default true)
     * @param limit Maximum number of notifications (default 50)
     * @param patientId Optional patient ID for caregivers
     * @return Result containing list of notifications or error
     */
    suspend fun getNotifications(
        notificationType: String? = null,
        includeRead: Boolean = true,
        limit: Int = 50,
        patientId: String? = null
    ): Result<List<Notification>> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getNotifications(
                authorization = "Bearer $token",
                notificationType = notificationType,
                includeRead = includeRead,
                limit = limit,
                patientId = patientId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val notifications = response.body()!!.map { it.toNotification() }
                Log.i(TAG, "Fetched ${notifications.size} notifications")
                Result.success(notifications)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "obtener notificaciones")
        }
    }
    
    /**
     * Get unread notification count.
     * 
     * @param patientId Optional patient ID for caregivers
     * @return Result containing unread count or error
     */
    suspend fun getUnreadCount(
        patientId: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getUnreadCount(
                authorization = "Bearer $token",
                patientId = patientId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val count = response.body()!!.unreadCount
                Log.i(TAG, "Unread count: $count")
                Result.success(count)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "obtener conteo de no leídas")
        }
    }
    
    /**
     * Mark a notification as read.
     * 
     * @param notificationId ID of the notification
     * @return Result indicating success or error
     */
    suspend fun markAsRead(
        notificationId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.markNotificationRead(
                authorization = "Bearer $token",
                notificationId = notificationId
            )
            
            if (response.isSuccessful) {
                Log.i(TAG, "Marked notification $notificationId as read")
                Result.success(Unit)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "marcar notificación como leída")
        }
    }
    
    /**
     * Mark all notifications as read.
     * 
     * @return Result indicating success or error
     */
    suspend fun markAllAsRead(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.markAllNotificationsRead(
                authorization = "Bearer $token"
            )
            
            if (response.isSuccessful) {
                Log.i(TAG, "Marked all notifications as read")
                Result.success(Unit)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "marcar todas las notificaciones como leídas")
        }
    }
    
    /**
     * Delete a notification.
     * 
     * @param notificationId ID of the notification
     * @return Result indicating success or error
     */
    suspend fun deleteNotification(
        notificationId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.deleteNotification(
                authorization = "Bearer $token",
                notificationId = notificationId
            )
            
            if (response.isSuccessful) {
                Log.i(TAG, "Deleted notification $notificationId")
                Result.success(Unit)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "eliminar notificación")
        }
    }
    
    /**
     * Get health tips for the current user.
     * 
     * @param category Optional filter by category (heart, stress, activity, sleep, nutrition)
     * @param limit Maximum number of tips (default 10)
     * @param patientId Optional patient ID for caregivers
     * @return Result containing list of health tips or error
     */
    suspend fun getHealthTips(
        category: String? = null,
        limit: Int = 10,
        patientId: String? = null
    ): Result<List<HealthTipResponse>> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getHealthTips(
                authorization = "Bearer $token",
                category = category,
                limit = limit,
                patientId = patientId
            )
            
            if (response.isSuccessful && response.body() != null) {
                val tips = response.body()!!
                Log.i(TAG, "Fetched ${tips.size} health tips")
                Result.success(tips)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "obtener consejos de salud")
        }
    }
    
    /**
     * Get a random health tip.
     * 
     * @param category Optional filter by category
     * @param patientId Optional patient ID for caregivers
     * @return Result containing health tip or error
     */
    suspend fun getRandomHealthTip(
        category: String? = null,
        patientId: String? = null
    ): Result<HealthTipResponse?> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No auth token available")
                return@withContext Result.failure(Exception("No has iniciado sesión"))
            }
            
            val response = apiService.getRandomHealthTip(
                authorization = "Bearer $token",
                category = category,
                patientId = patientId
            )
            
            if (response.isSuccessful) {
                val tip = response.body()
                Log.i(TAG, "Fetched random health tip: ${tip?.title}")
                Result.success(tip)
            } else {
                handleError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e, "obtener consejo de salud aleatorio")
        }
    }
    
    // ===================
    // Helper methods
    // ===================
    
    /**
     * Convert API response to domain model
     */
    private fun NotificationResponse.toNotification(): Notification {
        return Notification(
            id = this.id,
            type = parseNotificationType(this.type),
            title = this.title,
            message = this.message,
            timestamp = parseTimestamp(this.timestamp),
            isRead = this.isRead,
            priority = parseNotificationPriority(this.priority)
        )
    }
    
    private fun parseNotificationType(type: String): NotificationType {
        return try {
            NotificationType.valueOf(type.uppercase())
        } catch (e: Exception) {
            Log.w(TAG, "Unknown notification type: $type, defaulting to WARNING")
            NotificationType.WARNING
        }
    }
    
    private fun parseNotificationPriority(priority: String): NotificationPriority {
        return try {
            NotificationPriority.valueOf(priority.uppercase())
        } catch (e: Exception) {
            Log.w(TAG, "Unknown notification priority: $priority, defaulting to NORMAL")
            NotificationPriority.NORMAL
        }
    }
    
    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp == null) return System.currentTimeMillis()
        return try {
            ISO_DATE_FORMAT.parse(timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $timestamp")
            System.currentTimeMillis()
        }
    }
    
    private fun <T> handleError(code: Int, errorBody: String?): Result<T> {
        val errorMessage = when (code) {
            401 -> {
                // Emit session expired event for centralized handling
                return AuthEventBus.handleUnauthorized()
            }
            403 -> "No tienes permiso para realizar esta acción"
            404 -> "No encontrado"
            500 -> "Error del servidor"
            else -> "Error $code"
        }
        Log.e(TAG, "API error: $errorMessage ($errorBody)")
        return Result.failure(Exception(errorMessage))
    }
    
    private fun <T> handleException(e: Exception, action: String): Result<T> {
        Log.e(TAG, "Error al $action", e)
        val friendlyMessage = when (e) {
            is java.net.UnknownHostException -> "Sin conexión a internet"
            is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
            else -> e.message ?: "Error al $action"
        }
        return Result.failure(Exception(friendlyMessage))
    }
}
