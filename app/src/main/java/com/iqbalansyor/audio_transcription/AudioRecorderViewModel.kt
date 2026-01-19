package com.iqbalansyor.audio_transcription

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iqbalansyor.audio_transcription.media.Recorder
import com.iqbalansyor.audio_transcription.media.decodeWaveFile
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File

private const val LOG_TAG = "AudioRecorderViewModel"

data class RecordingState(
    val isRecording: Boolean = false,
    val amplitudes: List<Int> = emptyList(),
    val recordingFilePath: String? = null,
    val transcriptionText: String = "",
    val isTranscribing: Boolean = false,
    val isSavingRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val transcriptionDurationMs: Long = 0L,
    val isModelLoading: Boolean = false,
    val modelLoadError: String? = null
)

class AudioRecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var whisperContext: WhisperContext? = null
    private val recorder = Recorder()
    private var durationJob: Job? = null
    private var recordingStartTime: Long = 0L
    private var currentFilePath: String? = null

    private val maxAmplitudes = 50

    init {
        loadModel()
    }

    private fun loadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isModelLoading = true, modelLoadError = null)
            try {
                val context = getApplication<Application>()
                whisperContext = WhisperContext.createContextFromAsset(
                    context.assets,
                    "models/ggml-tiny.bin"
                )
                Log.d(LOG_TAG, "Whisper model loaded successfully")
                _state.value = _state.value.copy(isModelLoading = false)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to load Whisper model", e)
                _state.value = _state.value.copy(
                    isModelLoading = false,
                    modelLoadError = "Failed to load model: ${e.message}"
                )
            }
        }
    }

    fun startRecording() {
        val context = getApplication<Application>()
        val file = createAudioFile(context)
        currentFilePath = file.absolutePath

        recordingStartTime = System.currentTimeMillis()

        _state.value = _state.value.copy(
            isRecording = true,
            amplitudes = emptyList(),
            recordingFilePath = null,
            transcriptionText = "",
            recordingDurationMs = 0L
        )

        // Set up amplitude callback
        recorder.onAmplitudeUpdate = { amplitude ->
            val currentAmplitudes = _state.value.amplitudes.toMutableList()
            currentAmplitudes.add(amplitude)
            if (currentAmplitudes.size > maxAmplitudes) {
                currentAmplitudes.removeAt(0)
            }
            _state.value = _state.value.copy(amplitudes = currentAmplitudes)
        }

        // Start recording in background
        viewModelScope.launch(Dispatchers.IO) {
            recorder.startRecording()
        }

        // Track duration
        startDurationTracking()
    }

    fun stopRecording() {
        durationJob?.cancel()
        durationJob = null

        val duration = System.currentTimeMillis() - recordingStartTime
        val filePath = currentFilePath ?: return

        // Stop recording flag immediately (this allows the recording loop to exit)
        recorder.stopRecordingSync()

        _state.value = _state.value.copy(
            isRecording = false,
            isSavingRecording = true,
            recordingDurationMs = duration
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                recorder.stopRecording(File(filePath))
                Log.d(LOG_TAG, "Recording saved to: $filePath")
                _state.value = _state.value.copy(
                    recordingFilePath = filePath,
                    isSavingRecording = false
                )
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to save recording", e)
                _state.value = _state.value.copy(
                    isSavingRecording = false,
                    transcriptionText = "Failed to save recording: ${e.message}"
                )
            }
        }
    }

    private fun startDurationTracking() {
        durationJob = viewModelScope.launch {
            while (isActive) {
                val duration = System.currentTimeMillis() - recordingStartTime
                _state.value = _state.value.copy(recordingDurationMs = duration)
                delay(100)
            }
        }
    }

    private fun createAudioFile(context: android.content.Context): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "recording_$timestamp.wav"
        return File(context.cacheDir, fileName)
    }

    fun transcribe() {
        val filePath = _state.value.recordingFilePath ?: return
        val context = whisperContext ?: run {
            _state.value = _state.value.copy(
                transcriptionText = "Model not loaded. Please wait or restart the app."
            )
            return
        }

        _state.value = _state.value.copy(isTranscribing = true, transcriptionDurationMs = 0L)

        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val file = File(filePath)
                val audioData = decodeWaveFile(file)
                Log.d(LOG_TAG, "Decoded audio: ${audioData.size} samples")

                val result = context.transcribeData(audioData, printTimestamp = false)
                val duration = System.currentTimeMillis() - startTime
                Log.d(LOG_TAG, "Transcription completed in ${duration}ms: $result")

                _state.value = _state.value.copy(
                    transcriptionText = result.trim().ifEmpty { "(No speech detected)" },
                    isTranscribing = false,
                    transcriptionDurationMs = duration
                )
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(LOG_TAG, "Transcription failed after ${duration}ms", e)
                _state.value = _state.value.copy(
                    transcriptionText = "Transcription failed: ${e.message}",
                    isTranscribing = false,
                    transcriptionDurationMs = duration
                )
            }
        }
    }

    fun setTranscriptionText(text: String) {
        _state.value = _state.value.copy(transcriptionText = text)
    }

    override fun onCleared() {
        super.onCleared()
        durationJob?.cancel()
        viewModelScope.launch {
            whisperContext?.release()
        }
    }
}
