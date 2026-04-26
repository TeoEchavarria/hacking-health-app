package com.samsung.android.health.sdk.sample.healthdiary.health.classification

import com.samsung.android.health.sdk.sample.healthdiary.api.models.BPClassificationResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CrisisResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.HRClassificationResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ValidationResult
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Pure classification functions for cardiovascular health metrics.
 *
 * These functions are stateless and have no side effects, making them
 * easily unit-testable. They mirror the Python implementation exactly
 * for parity testing.
 *
 * Clinical thresholds follow AHA/ACC 2025 guidelines.
 */
object BloodPressureClassification {
    
    // =========================================
    // Blood Pressure Classification (AHA/ACC 2025)
    // =========================================
    
    private val BP_STAGES = mapOf(
        "hypertensive_crisis" to BPStageInfo("Hypertensive Crisis", "urgent"),
        "hypertension_stage_2" to BPStageInfo("Stage 2 Hypertension", "high"),
        "hypertension_stage_1" to BPStageInfo("Stage 1 Hypertension", "moderate"),
        "elevated" to BPStageInfo("Elevated Blood Pressure", "info"),
        "normal" to BPStageInfo("Normal Blood Pressure", "info")
    )
    
    private data class BPStageInfo(val label: String, val severity: String)
    
    /**
     * Classify blood pressure reading according to AHA/ACC 2025 guidelines.
     *
     * Stages (evaluated in order, first match wins):
     * - hypertensive_crisis: SBP > 180 OR DBP > 120
     * - hypertension_stage_2: SBP >= 140 OR DBP >= 90
     * - hypertension_stage_1: SBP 130-139 OR DBP 80-89
     * - elevated: SBP 120-129 AND DBP < 80
     * - normal: SBP < 120 AND DBP < 80
     */
    fun classifyBloodPressure(systolic: Int, diastolic: Int): BPClassificationResult {
        val stage = when {
            // Hypertensive Crisis
            systolic > 180 || diastolic > 120 -> "hypertensive_crisis"
            // Stage 2 Hypertension
            systolic >= 140 || diastolic >= 90 -> "hypertension_stage_2"
            // Stage 1 Hypertension
            systolic in 130..139 || diastolic in 80..89 -> "hypertension_stage_1"
            // Elevated
            systolic in 120..129 && diastolic < 80 -> "elevated"
            // Normal
            else -> "normal"
        }
        
        val stageInfo = BP_STAGES[stage]!!
        return BPClassificationResult(
            stage = stage,
            severity = stageInfo.severity,
            label = stageInfo.label,
            guideline = "AHA/ACC 2025"
        )
    }
    
    // =========================================
    // Heart Rate Classification
    // =========================================
    
    private val HR_CATEGORIES = mapOf(
        "critical_bradycardia" to HRCategoryInfo("Critical Bradycardia", "urgent"),
        "bradycardia" to HRCategoryInfo("Bradycardia", "moderate"),
        "normal" to HRCategoryInfo("Normal Heart Rate", "info"),
        "tachycardia" to HRCategoryInfo("Tachycardia", "moderate"),
        "critical_tachycardia" to HRCategoryInfo("Critical Tachycardia", "urgent")
    )
    
    private data class HRCategoryInfo(val label: String, val severity: String)
    
    /**
     * Classify heart rate reading for adult resting heart rate.
     *
     * Categories:
     * - critical_bradycardia: BPM < 40
     * - bradycardia: BPM 40-59
     * - normal: BPM 60-100
     * - tachycardia: BPM 101-150
     * - critical_tachycardia: BPM > 150
     */
    fun classifyHeartRate(bpm: Int): HRClassificationResult {
        val category = when {
            bpm < 40 -> "critical_bradycardia"
            bpm < 60 -> "bradycardia"
            bpm <= 100 -> "normal"
            bpm <= 150 -> "tachycardia"
            else -> "critical_tachycardia"
        }
        
        val categoryInfo = HR_CATEGORIES[category]!!
        return HRClassificationResult(
            category = category,
            severity = categoryInfo.severity,
            label = categoryInfo.label
        )
    }
    
    // =========================================
    // Physiological Validation
    // =========================================
    
    /**
     * Validate a blood pressure reading for physiological plausibility.
     *
     * Validation rules (in order):
     * 1. Systolic must be between 60 and 300 mmHg
     * 2. Diastolic must be between 30 and 200 mmHg
     * 3. Systolic must be strictly greater than diastolic
     * 4. Pulse (if present) must be between 20 and 300 BPM
     * 5. Timestamp must not be in the future (allow ±clockDriftMinutes)
     */
    fun validateBPReading(
        systolic: Int,
        diastolic: Int,
        pulse: Int? = null,
        timestamp: String? = null,
        clockDriftMinutes: Int = 2
    ): ValidationResult {
        // Rule 1: Systolic range
        if (systolic !in 60..300) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Systolic BP out of physiologically plausible range (60-300 mmHg)"
            )
        }
        
        // Rule 2: Diastolic range
        if (diastolic !in 30..200) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Diastolic BP out of physiologically plausible range (30-200 mmHg)"
            )
        }
        
        // Rule 3: Systolic > Diastolic
        if (systolic <= diastolic) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Systolic must be greater than diastolic"
            )
        }
        
        // Rule 4: Pulse range (if provided)
        if (pulse != null && pulse !in 20..300) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Pulse out of physiologically plausible range (20-300 BPM)"
            )
        }
        
        // Rule 5: Timestamp not in future (if provided)
        if (timestamp != null) {
            try {
                val ts = OffsetDateTime.parse(timestamp)
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                val maxAllowed = now.plusMinutes(clockDriftMinutes.toLong())
                if (ts.isAfter(maxAllowed)) {
                    return ValidationResult(
                        isValid = false,
                        errorMessage = "Timestamp cannot be in the future"
                    )
                }
            } catch (e: DateTimeParseException) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "Invalid timestamp format (expected ISO 8601)"
                )
            }
        }
        
        return ValidationResult(isValid = true, errorMessage = null)
    }
    
    /**
     * Validate a heart rate reading for physiological plausibility.
     */
    fun validateHeartRateReading(
        bpm: Int,
        timestamp: String? = null,
        clockDriftMinutes: Int = 2
    ): ValidationResult {
        // BPM range
        if (bpm !in 20..300) {
            return ValidationResult(
                isValid = false,
                errorMessage = "BPM out of physiologically plausible range (20-300)"
            )
        }
        
        // Timestamp not in future (if provided)
        if (timestamp != null) {
            try {
                val ts = OffsetDateTime.parse(timestamp)
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                val maxAllowed = now.plusMinutes(clockDriftMinutes.toLong())
                if (ts.isAfter(maxAllowed)) {
                    return ValidationResult(
                        isValid = false,
                        errorMessage = "Timestamp cannot be in the future"
                    )
                }
            } catch (e: DateTimeParseException) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "Invalid timestamp format (expected ISO 8601)"
                )
            }
        }
        
        return ValidationResult(isValid = true, errorMessage = null)
    }
    
    // =========================================
    // Crisis Detection (Edge/Mobile)
    // =========================================
    
    /**
     * Detect immediate crisis conditions for edge/mobile processing.
     *
     * This function is designed for immediate alerting on the device,
     * running in parallel with API submission.
     *
     * Crisis conditions:
     * - Hypertensive Crisis: SBP > 180 OR DBP > 120
     * - Critical Tachycardia: Pulse > 150
     * - Critical Bradycardia: Pulse < 40
     *
     * @return CrisisResult if detected, null otherwise
     */
    fun detectCrisis(
        systolic: Int? = null,
        diastolic: Int? = null,
        pulse: Int? = null
    ): CrisisResult? {
        // Hypertensive Crisis
        if (systolic != null && diastolic != null) {
            if (systolic > 180 || diastolic > 120) {
                return CrisisResult(
                    type = "hypertensive_crisis",
                    severity = "urgent",
                    title = "Lectura de Presión Arterial Crítica",
                    body = "Tu presión arterial ($systolic/$diastolic mmHg) está a un nivel peligroso. " +
                           "Busca atención médica de emergencia inmediatamente.",
                    guidanceCategory = "urgent_help"
                )
            }
        }
        
        // Critical Tachycardia
        if (pulse != null && pulse > 150) {
            return CrisisResult(
                type = "critical_tachycardia",
                severity = "urgent",
                title = "Frecuencia Cardíaca Muy Alta Detectada",
                body = "Tu frecuencia cardíaca ($pulse BPM) está críticamente elevada. Descansa " +
                       "inmediatamente y contacta servicios de emergencia si sientes dolor en el pecho o mareos.",
                guidanceCategory = "urgent_help"
            )
        }
        
        // Critical Bradycardia
        if (pulse != null && pulse < 40) {
            return CrisisResult(
                type = "critical_bradycardia",
                severity = "urgent",
                title = "Frecuencia Cardíaca Muy Baja Detectada",
                body = "Tu frecuencia cardíaca ($pulse BPM) está críticamente baja. " +
                       "Busca atención médica inmediatamente.",
                guidanceCategory = "urgent_help"
            )
        }
        
        return null
    }
}
