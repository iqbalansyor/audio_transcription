package com.iqbalansyor.audio_transcription.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveform(
    amplitudes: List<Int>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    maxBars: Int = 50,
    minBarHeight: Float = 0.05f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = canvasWidth / maxBars
        val barSpacing = barWidth * 0.2f
        val actualBarWidth = barWidth - barSpacing

        val paddedAmplitudes = if (amplitudes.size < maxBars) {
            List(maxBars - amplitudes.size) { 0 } + amplitudes
        } else {
            amplitudes.takeLast(maxBars)
        }

        paddedAmplitudes.forEachIndexed { index, amplitude ->
            val normalizedHeight = (amplitude / 100f).coerceIn(minBarHeight, 1f)
            val barHeight = canvasHeight * normalizedHeight
            val x = index * barWidth + barSpacing / 2
            val y = (canvasHeight - barHeight) / 2

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(actualBarWidth, barHeight),
                cornerRadius = CornerRadius(actualBarWidth / 2, actualBarWidth / 2)
            )
        }
    }
}

@Composable
fun AnimatedAudioWaveform(
    amplitudes: List<Int>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val barColor = if (isRecording) activeColor else inactiveColor

    AudioWaveform(
        amplitudes = amplitudes,
        modifier = modifier,
        barColor = barColor,
        backgroundColor = backgroundColor
    )
}