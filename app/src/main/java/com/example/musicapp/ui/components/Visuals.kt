package com.example.musicapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.musicapp.model.VisualizerType
import com.example.musicapp.viewmodel.MusicViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AuroraBackground(color1: Color, color2: Color, bounceScale: Float, speedMultiplier: Float = 1f, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween((20000 / speedMultiplier).roundToInt().coerceAtLeast(1), easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val breathe by infiniteTransition.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse))

    Canvas(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val w = size.width; val h = size.height; val center = Offset(w/2, h/2)
        val radius = w * 0.9f * bounceScale * breathe
        val offset1 = Offset(center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * w * 0.25f, center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * h * 0.15f)
        drawCircle(brush = Brush.radialGradient(colors = listOf(color1.copy(alpha = 0.85f), Color.Transparent), center = offset1, radius = radius), center = offset1, radius = radius)
        val offset2 = Offset(center.x - cos(Math.toRadians(angle.toDouble())).toFloat() * w * 0.25f, center.y - sin(Math.toRadians(angle.toDouble())).toFloat() * h * 0.15f + h*0.2f)
        drawCircle(brush = Brush.radialGradient(colors = listOf(color2.copy(alpha = 0.75f), Color.Transparent), center = offset2, radius = radius * 0.9f), center = offset2, radius = radius * 0.9f)
        val offset3 = Offset(center.x, center.y + h * 0.2f)
        drawCircle(brush = Brush.radialGradient(colors = listOf(color1.copy(alpha = 0.5f), color2.copy(alpha = 0.5f), Color.Transparent), center = offset3, radius = radius * 1.1f), center = offset3, radius = radius * 1.1f)
        drawRect(color = Color.Black.copy(alpha = 0.2f))
    }
}

@Composable
fun RealAudioVisualizer(viewModel: MusicViewModel, barColor: Color, modifier: Modifier = Modifier) {
    val frequencies by viewModel.visualizerData.collectAsState()
    val visualizerType by viewModel.visualizerType.collectAsState()
    val isMirrored by viewModel.isVisualizerMirrored.collectAsState() // Stav zrcadlení

    val brush = remember(barColor) { Brush.verticalGradient(0.0f to barColor.copy(alpha = 0.2f), 0.5f to barColor, 1.0f to barColor.copy(alpha = 0.2f)) }
    val reusablePath = remember { Path() }

    Canvas(modifier = modifier) {
        if (frequencies.isEmpty()) return@Canvas

        val barCount = frequencies.size
        val centerY = size.height / 2f
        val centerX = size.width / 2f

        when (visualizerType) {
            VisualizerType.BARS -> {
                val halfWidth = size.width / 2f
                val totalSlotWidth = if (isMirrored) halfWidth / barCount else size.width / barCount
                val barWidth = totalSlotWidth * 0.65f
                val gap = totalSlotWidth * 0.35f

                frequencies.forEachIndexed { index, magnitude ->
                    val totalBarHeight = (magnitude * size.height * 2.5f).coerceAtMost(size.height * 0.95f).coerceAtLeast(8f)
                    val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                    if (isMirrored) {
                        val offsetFromCenter = (index * totalSlotWidth) + (gap / 2)
                        drawRoundRect(brush = brush, topLeft = Offset(x = centerX + offsetFromCenter, y = centerY - (totalBarHeight / 2f)), size = Size(width = barWidth, height = totalBarHeight), cornerRadius = cornerRadius)
                        drawRoundRect(brush = brush, topLeft = Offset(x = centerX - offsetFromCenter - barWidth, y = centerY - (totalBarHeight / 2f)), size = Size(width = barWidth, height = totalBarHeight), cornerRadius = cornerRadius)
                    } else {
                        val offsetX = (index * totalSlotWidth) + (gap / 2)
                        drawRoundRect(brush = brush, topLeft = Offset(x = offsetX, y = centerY - (totalBarHeight / 2f)), size = Size(width = barWidth, height = totalBarHeight), cornerRadius = cornerRadius)
                    }
                }
            }

            VisualizerType.CIRCLE -> {
                val radius = size.height / 4.5f
                val maxBarHeight = (size.height / 2f) - radius

                drawCircle(color = barColor.copy(alpha = 0.15f), radius = radius * 1.1f, center = Offset(centerX, centerY))
                drawCircle(color = barColor.copy(alpha = 0.4f), radius = radius, center = Offset(centerX, centerY), style = Stroke(width = 4f))

                for (i in 0 until barCount) {
                    val magnitude = frequencies[i]
                    val barHeight = (magnitude * maxBarHeight * 2.5f).coerceAtLeast(2f)

                    if (isMirrored) {
                        val angleRight = Math.toRadians((i * 180.0 / barCount) - 90.0)
                        val startX1 = centerX + (cos(angleRight) * radius).toFloat()
                        val startY1 = centerY + (sin(angleRight) * radius).toFloat()
                        val endX1 = centerX + (cos(angleRight) * (radius + barHeight)).toFloat()
                        val endY1 = centerY + (sin(angleRight) * (radius + barHeight)).toFloat()
                        drawLine(color = barColor, start = Offset(startX1, startY1), end = Offset(endX1, endY1), strokeWidth = 10f, cap = StrokeCap.Round)

                        if (i != 0) {
                            val angleLeft = Math.toRadians((-i * 180.0 / barCount) - 90.0)
                            val startX2 = centerX + (cos(angleLeft) * radius).toFloat()
                            val startY2 = centerY + (sin(angleLeft) * radius).toFloat()
                            val endX2 = centerX + (cos(angleLeft) * (radius + barHeight)).toFloat()
                            val endY2 = centerY + (sin(angleLeft) * (radius + barHeight)).toFloat()
                            drawLine(color = barColor, start = Offset(startX2, startY2), end = Offset(endX2, endY2), strokeWidth = 10f, cap = StrokeCap.Round)
                        }
                    } else {
                        val angle = Math.toRadians((i * 360.0 / barCount) - 90.0)
                        val startX = centerX + (cos(angle) * radius).toFloat()
                        val startY = centerY + (sin(angle) * radius).toFloat()
                        val endX = centerX + (cos(angle) * (radius + barHeight)).toFloat()
                        val endY = centerY + (sin(angle) * (radius + barHeight)).toFloat()
                        drawLine(color = barColor, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 10f, cap = StrokeCap.Round)
                    }
                }
            }

            VisualizerType.WAVE -> {
                reusablePath.reset()

                val fullWave = if (isMirrored) {
                    val arr = FloatArray(barCount * 2)
                    for (i in 0 until barCount) arr[i] = frequencies[barCount - 1 - i]
                    for (i in 0 until barCount) arr[barCount + i] = frequencies[i]
                    arr
                } else {
                    frequencies.toFloatArray()
                }

                val stepX = size.width / (fullWave.size - 1).coerceAtLeast(1)
                val firstYOffset = (fullWave[0] * size.height * 1.5f).coerceAtMost(size.height / 2.2f)
                reusablePath.moveTo(0f, centerY - firstYOffset)

                for (i in 1 until fullWave.size) {
                    val x = i * stepX
                    val y = centerY - (fullWave[i] * size.height * 1.5f).coerceAtMost(size.height / 2.2f)

                    val prevX = (i - 1) * stepX
                    val prevY = centerY - (fullWave[i - 1] * size.height * 1.5f).coerceAtMost(size.height / 2.2f)

                    val midX = (prevX + x) / 2
                    val midY = (prevY + y) / 2

                    reusablePath.quadraticBezierTo(prevX, prevY, midX, midY)

                    if (i == fullWave.size - 1) reusablePath.lineTo(x, y)
                }

                drawPath(reusablePath, color = barColor.copy(alpha = 0.3f), style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(reusablePath, color = barColor, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            VisualizerType.DOTS -> {
                val totalSlotWidth = if (isMirrored) (size.width / 2f) / barCount else size.width / barCount
                val dotRadius = totalSlotWidth * 0.4f
                val gap = totalSlotWidth * 0.2f

                drawLine(color = barColor.copy(alpha = 0.2f), start = Offset(0f, centerY), end = Offset(size.width, centerY), strokeWidth = 2f)

                frequencies.forEachIndexed { index, magnitude ->
                    val heightFromCenter = (magnitude * size.height * 2.5f).coerceAtMost(size.height / 2.2f).coerceAtLeast(dotRadius * 2)

                    if (isMirrored) {
                        val offsetFromCenter = (index * totalSlotWidth) + (gap / 2) + dotRadius
                        drawCircle(color = barColor, radius = dotRadius, center = Offset(centerX + offsetFromCenter, centerY - heightFromCenter))
                        drawCircle(color = barColor, radius = dotRadius, center = Offset(centerX - offsetFromCenter, centerY - heightFromCenter))
                        drawCircle(color = barColor.copy(alpha = 0.5f), radius = dotRadius, center = Offset(centerX + offsetFromCenter, centerY + heightFromCenter))
                        drawCircle(color = barColor.copy(alpha = 0.5f), radius = dotRadius, center = Offset(centerX - offsetFromCenter, centerY + heightFromCenter))
                    } else {
                        val offsetX = (index * totalSlotWidth) + (gap / 2) + dotRadius
                        drawCircle(color = barColor, radius = dotRadius, center = Offset(offsetX, centerY - heightFromCenter))
                        drawCircle(color = barColor.copy(alpha = 0.5f), radius = dotRadius, center = Offset(offsetX, centerY + heightFromCenter))
                    }
                }
            }

            VisualizerType.MIRROR -> {
                val totalHeight = size.height * 0.8f
                val slotHeight = totalHeight / barCount
                val barHeight = slotHeight * 0.6f
                val gap = slotHeight * 0.4f
                val startY = centerY - totalHeight / 2f

                if (isMirrored) {
                    drawLine(color = barColor.copy(alpha = 0.3f), start = Offset(centerX, startY), end = Offset(centerX, startY + totalHeight), strokeWidth = 4f)
                }

                frequencies.forEachIndexed { index, magnitude ->
                    val y = startY + (index * slotHeight) + (gap / 2)
                    val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

                    if (isMirrored) {
                        val w = (magnitude * size.width / 2f).coerceAtLeast(4f)
                        drawRoundRect(color = barColor, topLeft = Offset(centerX, y), size = Size(w, barHeight), cornerRadius = cornerRadius)
                        drawRoundRect(color = barColor, topLeft = Offset(centerX - w, y), size = Size(w, barHeight), cornerRadius = cornerRadius)
                    } else {
                        val w = (magnitude * size.width * 0.9f).coerceAtLeast(4f)
                        drawRoundRect(color = barColor, topLeft = Offset(0f, y), size = Size(w, barHeight), cornerRadius = cornerRadius)
                    }
                }
            }

            VisualizerType.STAR -> {
                reusablePath.reset()
                val baseRadius = size.height / 5f
                val maxSpike = (size.height / 2.2f) - baseRadius
                val points = if (isMirrored) barCount * 2 else barCount
                val angleStep = Math.PI * 2 / points

                drawCircle(color = barColor.copy(alpha = 0.1f), radius = baseRadius, center = Offset(centerX, centerY))

                for (i in 0 until points) {
                    val freqIndex = if (isMirrored) {
                        if (i < barCount) barCount - 1 - i else i - barCount
                    } else i

                    val mag = frequencies[freqIndex]
                    val r = baseRadius + (mag * maxSpike * 2.5f).coerceAtLeast(0f)
                    val angle = i * angleStep - Math.PI / 2

                    val x = centerX + (cos(angle) * r).toFloat()
                    val y = centerY + (sin(angle) * r).toFloat()

                    if (i == 0) reusablePath.moveTo(x, y)
                    else reusablePath.lineTo(x, y)
                }
                reusablePath.close()

                drawPath(reusablePath, color = barColor.copy(alpha = 0.2f))
                drawPath(reusablePath, color = barColor, style = Stroke(width = 6f, join = StrokeJoin.Round))
            }
        }
    }
}