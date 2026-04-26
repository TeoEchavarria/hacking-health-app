package com.samsung.android.health.sdk.sample.healthdiary.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Simple audio recording service using MediaRecorder.
 * Records to a temporary file that can be uploaded to the API for processing.
 * 
 * Flow:
 * 1. User taps "record" -> startRecording()
 * 2. Service records to temp file with timer
 * 3. User taps "stop" -> stopRecording() returns File
 * 4. File is uploaded to API for Whisper transcription + BP extraction
 */
class AudioRecordingService(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecordingService"
        const val MAX_DURATION_MS = 30_000L  // 30 seconds max
        private const val TIMER_UPDATE_MS = 100L
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()
    
    private var startTimeMs: Long = 0
    
    // Timer runnable for updating duration
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_isRecording.value) {
                _recordingDurationMs.value = System.currentTimeMillis() - startTimeMs
                
                // Auto-stop at max duration
                if (_recordingDurationMs.value >= MAX_DURATION_MS) {
                    Log.i(TAG, "⏱️ Max duration reached, auto-stopping")
                    stopRecording()
                } else {
                    mainHandler.postDelayed(this, TIMER_UPDATE_MS)
                }
            }
        }
    }
    
    /**
     * Start recording audio to a temporary file.
     * @return true if recording started successfully
     */
    fun startRecording(): Boolean {
        if (_isRecording.value) {
            Log.w(TAG, "Already recording, ignoring start")
            return false
        }
        
        try {
            // Clean up any existing file
            cleanup()
            
            // Create temp file for recording - using M4A format (Whisper compatible)
            outputFile = File.createTempFile("bp_recording_", ".m4a", context.cacheDir)
            Log.i(TAG, "📁 Recording to: ${outputFile?.absolutePath}")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // Use MPEG_4 container with AAC encoder - compatible with Whisper
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)  // Good for speech
                setAudioEncodingBitRate(64000)  // Better quality for AAC
                setOutputFile(outputFile?.absolutePath)
                setMaxDuration(MAX_DURATION_MS.toInt())
                
                setOnInfoListener { _, what, _ ->
                    when (what) {
                        MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED -> {
                            Log.i(TAG, "🛑 Max duration reached via MediaRecorder")
                            stopRecording()
                        }
                    }
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "❌ MediaRecorder error: what=$what extra=$extra")
                    cleanup()
                }
                
                prepare()
                start()
            }
            
            startTimeMs = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationMs.value = 0
            
            // Start timer
            mainHandler.post(timerRunnable)
            
            Log.i(TAG, "🎤 Recording started")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            return false
        }
    }
    
    /**
     * Stop recording and return the recorded file.
     * @return The recorded audio file, or null if recording failed
     */
    fun stopRecording(): File? {
        if (!_isRecording.value) {
            Log.w(TAG, "Not recording, ignoring stop")
            return null
        }
        
        // Stop timer
        mainHandler.removeCallbacks(timerRunnable)
        
        val duration = System.currentTimeMillis() - startTimeMs
        _recordingDurationMs.value = duration
        _isRecording.value = false
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            val file = outputFile
            if (file != null && file.exists() && file.length() > 0) {
                Log.i(TAG, "🛑 Recording stopped. Duration: ${duration}ms, Size: ${file.length()} bytes")
                return file
            } else {
                Log.w(TAG, "Recording file is empty or missing")
                return null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            cleanup()
            return null
        }
    }
    
    /**
     * Cancel recording and delete temp file.
     */
    fun cancelRecording() {
        Log.i(TAG, "❌ Recording cancelled")
        mainHandler.removeCallbacks(timerRunnable)
        cleanup()
    }
    
    private fun cleanup() {
        _isRecording.value = false
        _recordingDurationMs.value = 0
        
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        
        try {
            outputFile?.delete()
        } catch (_: Exception) {}
        outputFile = null
    }
    
    /**
     * Delete the recorded file after upload.
     */
    fun deleteRecordedFile() {
        try {
            outputFile?.delete()
            outputFile = null
        } catch (_: Exception) {}
    }
    
    /**
     * Release resources.
     */
    fun destroy() {
        cleanup()
        mainHandler.removeCallbacksAndMessages(null)
    }
}
