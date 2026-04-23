package com.samsung.android.health.sdk.sample.healthdiary.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.*
import java.util.Locale
import java.util.UUID

/**
 * Helper class for Text-to-Speech functionality
 * 
 * Provides methods to speak numbers digit by digit and words
 * with configurable delays and repetitions.
 */
class TextToSpeechHelper(
    context: Context,
    private val onInitialized: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("es", "ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default Spanish
                    tts?.setLanguage(Locale("es"))
                }
                isInitialized = true
                onInitialized()
            } else {
                onError("Error inicializando TTS")
            }
        }
    }
    
    /**
     * Speaks a number digit by digit with pauses between digits
     * 
     * @param number The number to speak (e.g., 347 -> "tres", "cuatro", "siete")
     * @param delayBetweenDigits Delay in milliseconds between each digit
     * @param repetitions Number of times to repeat the entire sequence
     * @param delayBetweenRepetitions Delay in milliseconds between repetitions
     * @param onComplete Callback when speaking is complete
     */
    fun speakNumber(
        number: Int,
        delayBetweenDigits: Long = 800L,
        repetitions: Int = 2,
        delayBetweenRepetitions: Long = 1500L,
        onComplete: () -> Unit = {}
    ) {
        if (!isInitialized) {
            onError("TTS no inicializado")
            return
        }
        
        currentJob?.cancel()
        currentJob = scope.launch {
            val digits = number.toString().map { digitToSpanish(it) }
            
            repeat(repetitions) { rep ->
                digits.forEachIndexed { index, digit ->
                    speakAndWait(digit)
                    if (index < digits.lastIndex) {
                        delay(delayBetweenDigits)
                    }
                }
                if (rep < repetitions - 1) {
                    delay(delayBetweenRepetitions)
                }
            }
            onComplete()
        }
    }
    
    /**
     * Speaks a list of words with pauses between them
     * 
     * @param words List of words to speak
     * @param delayBetweenWords Delay in milliseconds between each word
     * @param repetitions Number of times to repeat the entire sequence
     * @param delayBetweenRepetitions Delay in milliseconds between repetitions
     * @param onComplete Callback when speaking is complete
     */
    fun speakWords(
        words: List<String>,
        delayBetweenWords: Long = 1000L,
        repetitions: Int = 2,
        delayBetweenRepetitions: Long = 2000L,
        onComplete: () -> Unit = {}
    ) {
        if (!isInitialized) {
            onError("TTS no inicializado")
            return
        }
        
        currentJob?.cancel()
        currentJob = scope.launch {
            repeat(repetitions) { rep ->
                words.forEachIndexed { index, word ->
                    speakAndWait(word)
                    if (index < words.lastIndex) {
                        delay(delayBetweenWords)
                    }
                }
                if (rep < repetitions - 1) {
                    delay(delayBetweenRepetitions)
                }
            }
            onComplete()
        }
    }
    
    /**
     * Speaks text and suspends until complete
     */
    private suspend fun speakAndWait(text: String) {
        suspendCancellableCoroutine { continuation ->
            val utteranceId = UUID.randomUUID().toString()
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                
                override fun onDone(utteranceId: String?) {
                    if (continuation.isActive) {
                        continuation.resume(Unit) {}
                    }
                }
                
                override fun onError(utteranceId: String?) {
                    if (continuation.isActive) {
                        continuation.resume(Unit) {}
                    }
                }
            })
            
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            
            continuation.invokeOnCancellation {
                tts?.stop()
            }
        }
    }
    
    /**
     * Converts a digit character to its Spanish word representation
     */
    private fun digitToSpanish(digit: Char): String {
        return when (digit) {
            '0' -> "cero"
            '1' -> "uno"
            '2' -> "dos"
            '3' -> "tres"
            '4' -> "cuatro"
            '5' -> "cinco"
            '6' -> "seis"
            '7' -> "siete"
            '8' -> "ocho"
            '9' -> "nueve"
            else -> digit.toString()
        }
    }
    
    /**
     * Stops any ongoing speech
     */
    fun stop() {
        currentJob?.cancel()
        tts?.stop()
    }
    
    /**
     * Releases TTS resources. Call this when done using the helper.
     */
    fun release() {
        stop()
        scope.cancel()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
    
    /**
     * Returns whether TTS is ready to use
     */
    fun isReady(): Boolean = isInitialized
}
