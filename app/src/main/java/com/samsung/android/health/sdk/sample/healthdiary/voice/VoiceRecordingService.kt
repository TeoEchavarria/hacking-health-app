package com.samsung.android.health.sdk.sample.healthdiary.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Voice recording service using Android's built-in SpeechRecognizer.
 *
 * This service provides CONTINUOUS listening - it automatically restarts
 * after each result or recoverable error until explicitly stopped by the user.
 */
class VoiceRecordingService(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceRecordingService"
        private const val RESTART_DELAY_MS = 300L
        const val SPANISH_LOCALE = "es-ES"
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _partialResult = MutableStateFlow<String?>(null)
    val partialResult: StateFlow<String?> = _partialResult.asStateFlow()
    
    // Flag to control continuous listening
    @Volatile
    private var shouldKeepListening = false
    
    /**
     * Check if speech recognition is available on this device.
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Create an intent to download Spanish language pack for offline speech recognition.
     * The user should launch this intent to enable offline Spanish recognition.
     */
    fun getDownloadSpanishLanguageIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, SPANISH_LOCALE)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
        }
    }
    
    /**
     * Create an intent to open Google Voice settings where user can download languages.
     * This is the settings screen where offline languages can be managed.
     */
    fun getVoiceSettingsIntent(): Intent {
        return Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    /**
     * Create an intent to open Google app's offline speech settings directly.
     */
    fun getGoogleSpeechSettingsIntent(): Intent {
        return Intent().apply {
            action = "com.google.android.voicesearch.HAND_FREE"
            setPackage("com.google.android.googlequicksearchbox")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    /**
     * Start continuous listening for voice input.
     * 
     * The service will keep listening and auto-restart after each result
     * or recoverable error until stopListening() is called.
     *
     * @return Flow that emits transcription results (can emit multiple times)
     */
    fun startListening(): Flow<VoiceResult> = callbackFlow {
        if (!isAvailable()) {
            trySend(VoiceResult.Error("Speech recognition not available on this device"))
            close()
            return@callbackFlow
        }
        
        shouldKeepListening = true
        
        // Use lateinit to handle mutual recursion between functions
        lateinit var createRecognizer: () -> Unit
        lateinit var startRecognition: () -> Unit
        
        fun restartListening() {
            mainHandler.postDelayed({
                if (shouldKeepListening) {
                    createRecognizer()
                    startRecognition()
                }
            }, RESTART_DELAY_MS)
        }
        
        createRecognizer = {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "🎤 Ready for speech")
                    _isListening.value = true
                    trySend(VoiceResult.Started)
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "🗣️ User started speaking")
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - could use for waveform visualization
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "🔇 End of speech detected")
                    // Don't set isListening to false here - we'll restart
                }
                
                override fun onError(error: Int) {
                    Log.w(TAG, "⚠️ Speech error: $error")
                    _partialResult.value = null
                    
                    // Check if this is a fatal error that should stop listening
                    // 9 = ERROR_INSUFFICIENT_PERMISSIONS
                    // 5 = ERROR_CLIENT
                    // 3 = ERROR_AUDIO
                    // 12 = ERROR_LANGUAGE_NOT_SUPPORTED
                    // 13 = ERROR_LANGUAGE_UNAVAILABLE (offline pack missing)
                    val isFatalError = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> true
                        SpeechRecognizer.ERROR_CLIENT -> true
                        SpeechRecognizer.ERROR_AUDIO -> true
                        12 -> true  // ERROR_LANGUAGE_NOT_SUPPORTED
                        13 -> true  // ERROR_LANGUAGE_UNAVAILABLE
                        else -> false
                    }
                    
                    if (isFatalError) {
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente de voz"
                            SpeechRecognizer.ERROR_AUDIO -> "Error de audio - verifica el micrófono"
                            12 -> "Idioma español no soportado en este dispositivo"
                            13 -> "Paquete de idioma español no disponible. Requiere conexión a internet."
                            else -> "Error fatal ($error)"
                        }
                        Log.e(TAG, "❌ Fatal error $error - stopping completely: $errorMessage")
                        // Ensure we fully stop
                        shouldKeepListening = false
                        _isListening.value = false
                        mainHandler.removeCallbacksAndMessages(null)
                        speechRecognizer?.stopListening()
                        speechRecognizer?.cancel()
                        speechRecognizer?.destroy()
                        speechRecognizer = null
                        trySend(VoiceResult.Error(errorMessage))
                        close()
                    } else {
                        // Non-fatal error - auto restart if we should keep listening
                        if (shouldKeepListening) {
                            Log.d(TAG, "🔄 Restarting listener after error")
                            restartListening()
                        }
                    }
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcription = matches?.firstOrNull()
                    
                    if (!transcription.isNullOrBlank()) {
                        Log.i(TAG, "✅ Got transcription: $transcription")
                        _partialResult.value = null
                        trySend(VoiceResult.Success(transcription))
                        // Note: We don't close() here - allow collecting more results
                    }
                    
                    // Auto-restart for continuous listening
                    if (shouldKeepListening) {
                        Log.d(TAG, "🔄 Restarting listener for continuous mode")
                        restartListening()
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        _partialResult.value = partial
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            
            speechRecognizer?.setRecognitionListener(listener)
        }
        
        startRecognition = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Longer speech timeout (10 seconds of silence before auto-stop)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            }
            speechRecognizer?.startListening(intent)
        }
        
        // Initial start
        createRecognizer()
        startRecognition()
        
        awaitClose {
            Log.d(TAG, "🛑 Flow closed, stopping recognition")
            stopListening()
        }
    }
    
    /**
     * Stop listening and release resources.
     */
    fun stopListening() {
        Log.d(TAG, "🛑 stopListening() called")
        shouldKeepListening = false
        _isListening.value = false
        _partialResult.value = null
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
    }
    
    /**
     * Release all resources. Call when no longer needed.
     */
    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

/**
 * Result type for voice recognition.
 */
sealed class VoiceResult {
    /** Recognition has started, device is listening */
    object Started : VoiceResult()
    
    /** Recognition completed successfully with transcription */
    data class Success(val transcription: String) : VoiceResult()
    
    /** Recognition failed with error message */
    data class Error(val message: String) : VoiceResult()
}
