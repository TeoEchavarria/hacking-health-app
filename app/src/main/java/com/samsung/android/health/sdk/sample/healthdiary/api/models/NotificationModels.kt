package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

// =========================================
// Notification Models (API)
// =========================================

/**
 * Respuesta de notificación del servidor
 */
data class NotificationResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("isRead")
    val isRead: Boolean,
    @SerializedName("metadata")
    val metadata: Map<String, Any>? = null,
    @SerializedName("timestamp")
    val timestamp: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * Respuesta de consejo de salud del servidor
 */
data class HealthTipResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("source")
    val source: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * Respuesta de conteo de notificaciones no leídas
 */
data class UnreadCountResponse(
    @SerializedName("unreadCount")
    val unreadCount: Int
)

/**
 * Respuesta genérica de mensaje
 */
data class MessageResponse(
    @SerializedName("message")
    val message: String
)
