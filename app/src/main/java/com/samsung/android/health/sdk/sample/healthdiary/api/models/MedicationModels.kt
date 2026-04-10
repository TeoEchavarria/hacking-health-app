package com.samsung.android.health.sdk.sample.healthdiary.api.models

import com.google.gson.annotations.SerializedName

// =========================================
// Medication Models
// =========================================

/**
 * Tipo de medicamento
 */
enum class MedicationType {
    @SerializedName("pill")
    PILL,
    @SerializedName("injection")
    INJECTION
}

/**
 * Modelo de medicamento
 */
data class Medication(
    @SerializedName("id")
    val id: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("dosage")
    val dosage: String,
    @SerializedName("time")
    val time: String, // HH:MM
    @SerializedName("instructions")
    val instructions: String,
    @SerializedName("medicationType")
    val medicationType: String, // "pill" o "injection"
    @SerializedName("isActive")
    val isActive: Boolean,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
) {
    /**
     * Obtiene el tipo de medicamento como enum
     */
    fun getMedicationTypeEnum(): MedicationType {
        return when (medicationType.lowercase()) {
            "injection" -> MedicationType.INJECTION
            else -> MedicationType.PILL
        }
    }
}

/**
 * Request para crear un medicamento
 */
data class MedicationCreateRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("dosage")
    val dosage: String = "",
    @SerializedName("time")
    val time: String,
    @SerializedName("instructions")
    val instructions: String = "",
    @SerializedName("medicationType")
    val medicationType: String = "pill"
)

/**
 * Request para actualizar un medicamento
 */
data class MedicationUpdateRequest(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("dosage")
    val dosage: String? = null,
    @SerializedName("time")
    val time: String? = null,
    @SerializedName("instructions")
    val instructions: String? = null,
    @SerializedName("medicationType")
    val medicationType: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean? = null
)

/**
 * Registro de toma de medicamento
 */
data class MedicationTake(
    @SerializedName("id")
    val id: String,
    @SerializedName("medicationId")
    val medicationId: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("takenAt")
    val takenAt: String,
    @SerializedName("date")
    val date: String, // YYYY-MM-DD
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null
)

/**
 * Request para registrar toma de medicamento
 */
data class TakeMedicationRequest(
    @SerializedName("medicationId")
    val medicationId: String,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("takenAt")
    val takenAt: String? = null // ISO datetime
)

/**
 * Medicamento con sus tomas del día
 */
data class MedicationWithTakes(
    @SerializedName("medication")
    val medication: Medication,
    @SerializedName("takes")
    val takes: List<MedicationTake>,
    @SerializedName("isTakenToday")
    val isTakenToday: Boolean
)

/**
 * Estadísticas mensuales por medicamento
 */
data class MonthlyMedicationStats(
    @SerializedName("medicationId")
    val medicationId: String,
    @SerializedName("medicationName")
    val medicationName: String,
    @SerializedName("totalDays")
    val totalDays: Int,
    @SerializedName("daysTaken")
    val daysTaken: Int,
    @SerializedName("adherencePercentage")
    val adherencePercentage: Float,
    @SerializedName("dailyTakes")
    val dailyTakes: Map<String, Int> // {date: count}
)

/**
 * Reporte mensual de adherencia
 */
data class MonthlyReportResponse(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("month")
    val month: String, // "YYYY-MM"
    @SerializedName("year")
    val year: Int,
    @SerializedName("monthName")
    val monthName: String,
    @SerializedName("medications")
    val medications: List<MonthlyMedicationStats>,
    @SerializedName("overallAdherence")
    val overallAdherence: Float
)

/**
 * Eventos del calendario por día
 */
data class CalendarEvent(
    @SerializedName("date")
    val date: String, // "YYYY-MM-DD"
    @SerializedName("hasMedication")
    val hasMedication: Boolean,
    @SerializedName("medicationsTaken")
    val medicationsTaken: Int,
    @SerializedName("totalMedications")
    val totalMedications: Int
)

/**
 * Mensaje de respuesta genérico
 */
data class MedicationMessageResponse(
    @SerializedName("message")
    val message: String
)
