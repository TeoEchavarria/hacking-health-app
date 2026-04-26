package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.api.models.AudioParseResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.BPClassificationResult
import com.samsung.android.health.sdk.sample.healthdiary.api.models.CrisisResult
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.BloodPressureReadingEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.SyncStatus
import com.samsung.android.health.sdk.sample.healthdiary.health.classification.BloodPressureClassification
import com.samsung.android.health.sdk.sample.healthdiary.utils.TokenManager
import com.samsung.android.health.sdk.sample.healthdiary.voice.AudioRecordingService
import com.samsung.android.health.sdk.sample.healthdiary.worker.BloodPressureSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * UI state for the blood pressure voice flow.
 */
data class BPVoiceUiState(
    /** Recording state */
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0,
    
    /** Uploading/parsing state */
    val isUploading: Boolean = false,
    
    /** Transcription result from Whisper */
    val transcription: String? = null,
    
    /** Parsed BP values */
    val parseResult: AudioParseResult? = null,
    
    /** Validation state */
    val validationError: String? = null,
    
    /** Local classification (edge) */
    val classification: BPClassificationResult? = null,
    
    /** Crisis detection */
    val crisisDetected: CrisisResult? = null,
    
    /** Submission state */
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean = false,
    
    /** Dialogs */
    val showLowConfidenceDialog: Boolean = false,
    val showCrisisDialog: Boolean = false,
    val showSuccessDialog: Boolean = false,
    
    /** Error state */
    val error: String? = null
) {
    /** Whether the flow is in progress (any active operation) */
    val isInProgress: Boolean
        get() = isRecording || isUploading || isSubmitting
    
    // Compatibility: isListening maps to isRecording
    val isListening: Boolean get() = isRecording
    val isParsing: Boolean get() = isUploading
    
    // No longer needed - online audio processing doesn't need language pack
    val needsLanguagePack: Boolean = false
    val partialTranscription: String? = null
}

/**
 * ViewModel for the blood pressure voice input flow.
 *
 * New audio-based flow:
 * 1. User taps record button → startRecording()
 * 2. Audio is recorded to temp file (max 30s)
 * 3. User taps stop → stopRecording()
 * 4. Audio uploaded to API for Whisper STT + LLM extraction
 * 5. Values validated and classified locally
 * 6. Crisis detection runs
 * 7. Show result for confirmation / edit
 */
class BloodPressureVoiceViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "BPVoiceViewModel"
    }
    
    private val database = AppDatabase.getDatabase(application)
    private val audioService = AudioRecordingService(application)
    private var recordedFile: File? = null
    
    private val _uiState = MutableStateFlow(BPVoiceUiState())
    val uiState: StateFlow<BPVoiceUiState> = _uiState.asStateFlow()
    
    /** Count of readings pending sync (for badge display) */
    val pendingSyncCount = database.bloodPressureReadingDao().getPendingSyncCount()
    
    init {
        // Collect recording duration updates
        viewModelScope.launch {
            audioService.recordingDurationMs.collect { duration ->
                _uiState.update { it.copy(recordingDurationMs = duration) }
            }
        }
        
        // Collect recording state
        viewModelScope.launch {
            audioService.isRecording.collect { isRecording ->
                _uiState.update { it.copy(isRecording = isRecording) }
            }
        }
    }
    
    /**
     * Check if user is authenticated before starting.
     */
    private fun isAuthenticated(): Boolean {
        return TokenManager.getToken() != null
    }
    
    /**
     * Start recording audio. User speaks their BP reading.
     */
    fun startRecording() {
        val currentState = _uiState.value
        if (currentState.isInProgress) {
            Log.w(TAG, "Already in progress, ignoring startRecording")
            return
        }
        
        // Check authentication first
        if (!isAuthenticated()) {
            Log.e(TAG, "❌ Not authenticated")
            _uiState.update { 
                it.copy(error = "No has iniciado sesión. Por favor inicia sesión primero.") 
            }
            return
        }
        
        Log.i(TAG, "🎤 Starting audio recording")
        
        // Reset to clean state but indicate we're starting
        _uiState.value = BPVoiceUiState(isRecording = true)
        
        val started = audioService.startRecording()
        if (!started) {
            Log.e(TAG, "❌ Failed to start recording")
            _uiState.update {
                it.copy(
                    isRecording = false,
                    error = "No se pudo iniciar la grabación. Verifica los permisos de micrófono."
                )
            }
        }
    }
    
    /**
     * Stop recording and process the audio.
     */
    fun stopRecording() {
        Log.i(TAG, "🛑 stopRecording() called, isRecording=${_uiState.value.isRecording}")
        
        if (!_uiState.value.isRecording) {
            Log.w(TAG, "⚠️ Not recording, ignoring stopRecording")
            return
        }
        
        Log.i(TAG, "🛑 Stopping audio recording")
        recordedFile = audioService.stopRecording()
        
        if (recordedFile == null || !recordedFile!!.exists() || recordedFile!!.length() == 0L) {
            Log.e(TAG, "❌ No audio recorded")
            _uiState.update {
                it.copy(
                    isRecording = false,
                    error = "No se grabó ningún audio. Intenta de nuevo."
                )
            }
            return
        }
        
        Log.i(TAG, "📤 Audio recorded: ${recordedFile?.length()} bytes")
        uploadAndParseAudio(recordedFile!!)
    }
    
    /**
     * Cancel recording without processing.
     */
    fun cancelRecording() {
        Log.i(TAG, "❌ Recording cancelled")
        audioService.cancelRecording()
        recordedFile = null
        _uiState.update { it.copy(isRecording = false) }
    }
    
    // Compatibility aliases for old API
    fun startListening() = startRecording()
    fun stopListening() = stopRecording()
    
    /**
     * Upload audio to API for transcription and parsing.
     */
    private fun uploadAndParseAudio(file: File) {
        Log.i(TAG, "📤 ====== STARTING AUDIO UPLOAD ======")
        Log.i(TAG, "📤 File: ${file.absolutePath}, size=${file.length()} bytes, exists=${file.exists()}")
        _uiState.update { it.copy(isUploading = true, isRecording = false) }
        
        viewModelScope.launch {
            try {
                val token = TokenManager.getToken()
                if (token == null) {
                    Log.e(TAG, "❌ No auth token available!")
                    throw IllegalStateException("No auth token")
                }
                Log.i(TAG, "🔑 Token available: ${token.take(10)}...")
                
                // Create multipart request - using audio/mp4 for M4A files (Whisper compatible)
                val requestBody = file.asRequestBody("audio/mp4".toMediaType())
                val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestBody)
                Log.i(TAG, "📦 Multipart request created (M4A format), sending to API...")
                
                // Upload and parse
                val result = RetrofitClient.bloodPressureApiService.parseBPAudio(
                    token = "Bearer $token",
                    audio = audioPart
                )
                
                Log.i(TAG, "✅ ====== API RESPONSE RECEIVED ======")
                Log.i(TAG, "📊 Parse result: S=${result.systolic} D=${result.diastolic} P=${result.pulse} conf=${result.confidence}")
                Log.i(TAG, "📝 Transcription: ${result.transcription}")
                
                // Clean up temp file
                audioService.deleteRecordedFile()
                
                _uiState.update { 
                    it.copy(
                        isUploading = false,
                        transcription = result.transcription,
                        parseResult = result
                    )
                }
                
                // Process the result
                processParseResult(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ ====== AUDIO PARSING FAILED ======")
                Log.e(TAG, "❌ Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "❌ Error message: ${e.message}")
                Log.e(TAG, "❌ Full stack trace:", e)
                
                val errorMessage = when (e) {
                    is java.net.UnknownHostException -> "Sin conexión a internet"
                    is java.net.SocketTimeoutException -> "La conexión tardó demasiado"
                    is retrofit2.HttpException -> "Error del servidor: ${e.code()}"
                    else -> "Error: ${e.message}"
                }
                
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        error = errorMessage
                    )
                }
            }
        }
    }
    
    /**
     * Process the parse result: validate, classify, detect crisis.
     */
    private fun processParseResult(parseResult: AudioParseResult) {
        // Check if we got values
        val systolic = parseResult.systolic
        val diastolic = parseResult.diastolic
        
        if (systolic == null || diastolic == null) {
            Log.w(TAG, "⚠️ Could not extract BP values")
            _uiState.update {
                it.copy(
                    validationError = "No se detectaron valores de presión arterial. Escuchamos: \"${parseResult.transcription}\"",
                    showLowConfidenceDialog = true
                )
            }
            return
        }
        
        // Validate physiological plausibility
        val validation = BloodPressureClassification.validateBPReading(
            systolic = systolic,
            diastolic = diastolic,
            pulse = parseResult.pulse
        )
        
        if (!validation.isValid) {
            Log.w(TAG, "⚠️ Validation failed: ${validation.errorMessage}")
            _uiState.update {
                it.copy(
                    validationError = validation.errorMessage,
                    showLowConfidenceDialog = true
                )
            }
            return
        }
        
        // Classify blood pressure
        val classification = BloodPressureClassification.classifyBloodPressure(systolic, diastolic)
        Log.i(TAG, "📊 Classification: ${classification.stage} (${classification.severity})")
        
        // Check for crisis
        val crisis = BloodPressureClassification.detectCrisis(
            systolic = systolic,
            diastolic = diastolic,
            pulse = parseResult.pulse
        )
        
        _uiState.update {
            it.copy(
                classification = classification,
                crisisDetected = crisis,
                showCrisisDialog = crisis != null
            )
        }
        
        // Handle low confidence
        if (parseResult.confidence == "low") {
            Log.w(TAG, "⚠️ Low confidence parse result")
            _uiState.update { it.copy(showLowConfidenceDialog = true) }
            return
        }
        
        // If no crisis and high confidence, show success for confirmation
        if (crisis == null) {
            _uiState.update { it.copy(showSuccessDialog = true) }
        }
        // If crisis detected, wait for user acknowledgment (UI will show dialog)
    }
    
    /**
     * User confirmed reading (after success, low confidence, or crisis dialog).
     */
    fun confirmAndSubmit() {
        val parseResult = _uiState.value.parseResult
        val classification = _uiState.value.classification
        val crisis = _uiState.value.crisisDetected
        
        if (parseResult?.systolic == null || parseResult.diastolic == null) {
            Log.e(TAG, "Cannot submit without systolic/diastolic values")
            return
        }
        
        Log.i(TAG, "💾 Submitting BP reading: ${parseResult.systolic}/${parseResult.diastolic}")
        _uiState.update {
            it.copy(
                isSubmitting = true,
                showLowConfidenceDialog = false,
                showCrisisDialog = false,
                showSuccessDialog = false
            )
        }
        
        viewModelScope.launch {
            try {
                val userId = TokenManager.getUserIdFromToken() 
                    ?: throw IllegalStateException("User ID not found")
                
                val timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                
                // Create local entity with PENDING sync status
                val entity = BloodPressureReadingEntity(
                    userId = userId,
                    systolic = parseResult.systolic,
                    diastolic = parseResult.diastolic,
                    pulse = parseResult.pulse,
                    timestamp = timestamp,
                    source = "voice",
                    crisisFlag = crisis != null,
                    lowConfidenceFlag = parseResult.confidence == "low",
                    syncStatus = SyncStatus.PENDING
                )
                
                // Save to local DB
                database.bloodPressureReadingDao().insert(entity)
                Log.i(TAG, "✅ Saved to local DB: ${entity.idempotencyKey}")
                
                // Trigger background sync
                BloodPressureSyncWorker.enqueue(getApplication())
                
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        showSuccessDialog = true
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save reading", e)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = "No se pudo guardar: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * User wants to try again (after error or low confidence).
     */
    fun retryMeasurement() {
        resetState()
    }
    
    /**
     * User wants to manually edit the values.
     */
    fun editValues(systolic: Int, diastolic: Int, pulse: Int?) {
        val current = _uiState.value.parseResult ?: return
        
        // Update parse result with manual values
        val edited = AudioParseResult(
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            deviceClassification = current.deviceClassification,
            confidence = "high", // Manual entry is high confidence
            transcription = current.transcription
        )
        
        _uiState.update {
            it.copy(
                parseResult = edited,
                showLowConfidenceDialog = false
            )
        }
        
        // Re-process with edited values
        processParseResult(edited)
    }
    
    /**
     * Dismiss a dialog without action.
     */
    fun dismissDialog() {
        _uiState.update {
            it.copy(
                showLowConfidenceDialog = false,
                showCrisisDialog = false,
                showSuccessDialog = false,
                error = null
            )
        }
    }
    
    // No longer needed - online processing
    fun openLanguageSettings() {}
    
    /**
     * Reset state for new measurement.
     */
    fun resetState() {
        // Cancel any active recording
        audioService.cancelRecording()
        recordedFile = null
        _uiState.value = BPVoiceUiState()
    }
    
    override fun onCleared() {
        super.onCleared()
        audioService.destroy()
    }
}
