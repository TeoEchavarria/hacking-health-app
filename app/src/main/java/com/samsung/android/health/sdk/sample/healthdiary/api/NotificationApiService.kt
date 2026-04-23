package com.samsung.android.health.sdk.sample.healthdiary.api

import com.samsung.android.health.sdk.sample.healthdiary.api.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for managing notifications and health tips.
 * 
 * All endpoints require Authorization header with Bearer token.
 * Operations are scoped to the authenticated user.
 */
interface NotificationApiService {
    
    /**
     * Get all notifications for the current user.
     * 
     * @param authorization Bearer token
     * @param notificationType Optional filter by notification type
     * @param includeRead Include read notifications (default true)
     * @param limit Maximum number of notifications to return (default 50)
     * @param patientId Optional patient ID for caregivers viewing patient's notifications
     * @return List of notifications sorted by timestamp (newest first)
     */
    @GET("/notifications")
    suspend fun getNotifications(
        @Header("Authorization") authorization: String,
        @Query("notification_type") notificationType: String? = null,
        @Query("include_read") includeRead: Boolean = true,
        @Query("limit") limit: Int = 50,
        @Query("patient_id") patientId: String? = null
    ): Response<List<NotificationResponse>>
    
    /**
     * Get unread notification count.
     * 
     * @param authorization Bearer token
     * @param patientId Optional patient ID for caregivers
     * @return Unread count
     */
    @GET("/notifications/unread-count")
    suspend fun getUnreadCount(
        @Header("Authorization") authorization: String,
        @Query("patient_id") patientId: String? = null
    ): Response<UnreadCountResponse>
    
    /**
     * Mark a notification as read.
     * 
     * @param authorization Bearer token
     * @param notificationId ID of the notification
     * @return Success message
     */
    @PATCH("/notifications/{notificationId}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") authorization: String,
        @Path("notificationId") notificationId: String
    ): Response<MessageResponse>
    
    /**
     * Mark all notifications as read.
     * 
     * @param authorization Bearer token
     * @return Success message with count
     */
    @PATCH("/notifications/read-all")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") authorization: String
    ): Response<MessageResponse>
    
    /**
     * Delete a notification.
     * 
     * @param authorization Bearer token
     * @param notificationId ID of the notification
     * @return Success message
     */
    @DELETE("/notifications/{notificationId}")
    suspend fun deleteNotification(
        @Header("Authorization") authorization: String,
        @Path("notificationId") notificationId: String
    ): Response<MessageResponse>
    
    /**
     * Get health tips for the current user.
     * 
     * @param authorization Bearer token
     * @param category Optional filter by category (heart, stress, activity, sleep, nutrition)
     * @param limit Maximum number of tips to return (default 10)
     * @param patientId Optional patient ID for caregivers
     * @return List of health tips
     */
    @GET("/notifications/health-tips")
    suspend fun getHealthTips(
        @Header("Authorization") authorization: String,
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("patient_id") patientId: String? = null
    ): Response<List<HealthTipResponse>>
    
    /**
     * Get a random health tip.
     * 
     * @param authorization Bearer token
     * @param category Optional filter by category
     * @param patientId Optional patient ID for caregivers
     * @return Random health tip or null
     */
    @GET("/notifications/health-tips/random")
    suspend fun getRandomHealthTip(
        @Header("Authorization") authorization: String,
        @Query("category") category: String? = null,
        @Query("patient_id") patientId: String? = null
    ): Response<HealthTipResponse?>
}
