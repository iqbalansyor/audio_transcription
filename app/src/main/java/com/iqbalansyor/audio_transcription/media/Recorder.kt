package com.iqbalansyor.audio_transcription.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private const val LOG_TAG = "Recorder"

class Recorder {
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private val recordedData = mutableListOf<Short>()

    // Callback for amplitude updates
    var onAmplitudeUpdate: ((Int) -> Unit)? = null

    @SuppressLint("MissingPermission")
    suspend fun startRecording() = withContext(Dispatchers.IO) {
        if (isRecording.get()) {
            Log.w(LOG_TAG, "Already recording")
            return@withContext
        }

        synchronized(recordedData) {
            recordedData.clear()
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording.set(true)
        Log.d(LOG_TAG, "Recording started")

        val buffer = ShortArray(bufferSize / 2)
        while (isRecording.get()) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (read > 0) {
                synchronized(recordedData) {
                    for (i in 0 until read) {
                        recordedData.add(buffer[i])
                    }
                }
                // Calculate amplitude for visualization
                val maxAmplitude = buffer.take(read).maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                val normalizedAmplitude = (maxAmplitude / 32767f * 100).toInt().coerceIn(0, 100)
                onAmplitudeUpdate?.invoke(normalizedAmplitude)
            }
        }
        Log.d(LOG_TAG, "Recording loop ended")
    }

    suspend fun stopRecording(outputFile: File) = withContext(Dispatchers.IO) {
        Log.d(LOG_TAG, "Stopping recording...")
        isRecording.set(false)

        // Give the recording loop time to exit
        Thread.sleep(100)

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d(LOG_TAG, "AudioRecord released")

        val data: ShortArray
        synchronized(recordedData) {
            data = recordedData.toShortArray()
            Log.d(LOG_TAG, "Recorded ${data.size} samples")
        }

        if (data.isNotEmpty()) {
            encodeWaveFile(outputFile, data)
            Log.d(LOG_TAG, "WAV file saved: ${outputFile.absolutePath}")
        } else {
            Log.w(LOG_TAG, "No audio data recorded!")
        }
    }

    fun stopRecordingSync() {
        isRecording.set(false)
    }

    fun isRecording(): Boolean = isRecording.get()
}
