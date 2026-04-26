package com.samsung.android.health.sdk.sample.healthdiary.voice

import com.samsung.android.health.sdk.sample.healthdiary.api.BloodPressureApiService
import com.samsung.android.health.sdk.sample.healthdiary.api.models.ParseBPVoiceRequest
import com.samsung.android.health.sdk.sample.healthdiary.api.models.VoiceParseResult

/**
 * LLM-based parser for extracting blood pressure values from voice transcriptions.
 *
 * Uses the backend endpoint which has the system prompt embedded for consistent
 * parsing of natural language BP readings like "ciento veinte sobre ochenta" or
 * "mi presión está en 135/88".
 */
class BloodPressureLLMParser(
    private val apiService: BloodPressureApiService,
    private val tokenProvider: suspend () -> String
) {
    
    /**
     * Parse a voice transcription to extract blood pressure values.
     *
     * @param transcription Raw text from speech recognition
     * @return Parsed BP values with confidence level
     * @throws Exception on network or parsing errors
     */
    suspend fun parseTranscription(transcription: String): VoiceParseResult {
        val token = tokenProvider()
        val request = ParseBPVoiceRequest(transcription = transcription)
        return apiService.parseBPVoice(token, request)
    }
    
    companion object {
        /**
         * Check if transcription likely contains BP values (for quick local check).
         *
         * This is a heuristic check before sending to LLM - saves API calls
         * for clearly non-BP transcriptions.
         */
        fun mightContainBPValues(transcription: String): Boolean {
            val lower = transcription.lowercase()
            
            // Common BP indicators in Spanish
            val bpIndicators = listOf(
                "presión", "presion",
                "sistólica", "sistolica", "diastólica", "diastolica",
                "sobre", "entre", "slash", "barra",
                "mmhg", "milímetros", "milimetros",
                "pulso", "latidos", "bpm",
                "ciento", "setenta", "ochenta", "noventa",  // Common BP number words
                "normal", "alta", "baja"
            )
            
            // Or contains digit patterns like "120/80", "120 80", "120 sobre 80"
            val hasDigits = lower.contains(Regex("""\d{2,3}"""))
            val hasIndicator = bpIndicators.any { lower.contains(it) }
            
            return hasDigits || hasIndicator
        }
    }
}
