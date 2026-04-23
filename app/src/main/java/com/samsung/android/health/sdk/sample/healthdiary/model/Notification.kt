package com.samsung.android.health.sdk.sample.healthdiary.model

/**
 * Notification data model for displaying health-related notifications.
 * 
 * Types:
 * - MEDICATION: Medication reminders
 * - ACHIEVEMENT: Goal achievements (steps, sleep, etc.)
 * - WARNING: Warnings (low battery, abnormal readings)
 * - APPOINTMENT: Medical appointment reminders
 * - REPORT: Health report updates
 */
data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

/**
 * Type of notification
 */
enum class NotificationType {
    MEDICATION,      // 💊 Medication reminders
    ACHIEVEMENT,     // ✓ Goal achievements
    WARNING,         // ⚠ Warnings
    APPOINTMENT,     // 📅 Medical appointments
    REPORT,          // 📄 Health reports
    HEALTH_TIP,      // 💡 Health tips/advice
    VITALS           // ❤️ Vital signs notifications
}

/**
 * Priority level for notifications
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Helper function to get icon name for notification type
 */
fun NotificationType.getIconName(): String = when (this) {
    NotificationType.MEDICATION -> "pill"
    NotificationType.ACHIEVEMENT -> "check_circle"
    NotificationType.WARNING -> "warning"
    NotificationType.APPOINTMENT -> "calendar_today"
    NotificationType.REPORT -> "update"
    NotificationType.HEALTH_TIP -> "lightbulb"
    NotificationType.VITALS -> "favorite"
}

/**
 * Helper function to get color scheme for notification type
 */
fun NotificationType.getColorScheme(): Pair<String, String> = when (this) {
    NotificationType.MEDICATION -> "blue-50" to "blue-600"
    NotificationType.ACHIEVEMENT -> "green-50" to "green-600"
    NotificationType.WARNING -> "amber-50" to "amber-600"
    NotificationType.APPOINTMENT -> "purple-50" to "purple-600"
    NotificationType.REPORT -> "primary-fixed/30" to "primary"
    NotificationType.HEALTH_TIP -> "teal-50" to "teal-600"
    NotificationType.VITALS -> "red-50" to "red-600"
}
