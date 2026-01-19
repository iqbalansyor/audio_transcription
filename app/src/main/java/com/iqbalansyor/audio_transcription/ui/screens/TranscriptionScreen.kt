package com.iqbalansyor.audio_transcription.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iqbalansyor.audio_transcription.AudioRecorderViewModel
import com.iqbalansyor.audio_transcription.ui.components.AnimatedAudioWaveform
import java.util.Locale

@Composable
fun TranscriptionScreen(
    viewModel: AudioRecorderViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Audio Transcription",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Show model loading state
        if (state.isModelLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading model...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        state.modelLoadError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedAudioWaveform(
            amplitudes = state.amplitudes,
            isRecording = state.isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formatDuration(state.recordingDurationMs),
            style = MaterialTheme.typography.titleLarge,
            color = if (state.isRecording) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        RecordButton(
            isRecording = state.isRecording,
            enabled = !state.isModelLoading,
            onClick = {
                if (state.isRecording) {
                    viewModel.stopRecording()
                } else {
                    viewModel.startRecording()
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                state.isRecording -> "Tap to stop"
                state.isSavingRecording -> "Saving..."
                else -> "Tap to record"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Transcribe button
        AnimatedVisibility(
            visible = (state.recordingFilePath != null || state.isSavingRecording) && !state.isRecording && !state.isTranscribing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.transcribe() },
                    enabled = !state.isModelLoading && !state.isSavingRecording && state.recordingFilePath != null
                ) {
                    if (state.isSavingRecording) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Transcribe")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = state.recordingFilePath != null || state.transcriptionText.isNotEmpty() || state.isTranscribing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TranscriptionResultCard(
                filePath = state.recordingFilePath,
                transcriptionText = state.transcriptionText,
                isTranscribing = state.isTranscribing,
                transcriptionDurationMs = state.transcriptionDurationMs,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(80.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isRecording) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 28.dp else 32.dp)
                .clip(if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun TranscriptionResultCard(
    filePath: String?,
    transcriptionText: String,
    isTranscribing: Boolean,
    transcriptionDurationMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filePath != null) {
                Text(
                    text = "Recording saved",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = filePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (isTranscribing) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Transcribing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (transcriptionText.isNotEmpty()) {
                Text(
                    text = if (transcriptionDurationMs > 0) {
                        "Transcription (${formatTranscriptionDuration(transcriptionDurationMs)})"
                    } else {
                        "Transcription"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = transcriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatTranscriptionDuration(durationMs: Long): String {
    return when {
        durationMs < 1000 -> "${durationMs}ms"
        else -> String.format(Locale.US, "%.1fs", durationMs / 1000.0)
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (durationMs % 1000) / 10
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millis)
}