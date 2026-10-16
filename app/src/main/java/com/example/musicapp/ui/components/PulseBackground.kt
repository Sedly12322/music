package com.example.musicapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.isActive

private data class PulseWave(var radius: Float, var alpha: Float)

@Composable
fun PulseBackground(
    color: Color,
    bounceScale: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val waves = remember { mutableStateListOf<PulseWave>() }
    var timeSinceLastSpawn by remember { mutableLongStateOf(0L) }
    var tick by remember { mutableLongStateOf(0L) }
    val currentBounceScale by rememberUpdatedState(bounceScale)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    LaunchedEffect(isPlaying) {
        if (!isPlaying && waves.isEmpty()) return@LaunchedEffect
        var lastFrameTime = System.nanoTime()
        while (isActive) {
            withFrameNanos { currentTime ->
                val dt = ((currentTime - lastFrameTime) / 1_000_000f).coerceIn(0f, 100f)
                lastFrameTime = currentTime
                timeSinceLastSpawn += dt.toLong()

                val energy = (currentBounceScale - 1f).coerceAtLeast(0f)
                val spawnThreshold = if (currentIsPlaying) (1200f - (energy * 2000f)).coerceAtLeast(400f) else 3500f

                if (currentIsPlaying && timeSinceLastSpawn > spawnThreshold) {
                    waves.add(PulseWave(radius = 0f, alpha = 1f))
                    timeSinceLastSpawn = 0
                }

                val iterator = waves.iterator()
                while (iterator.hasNext()) {
                    val wave = iterator.next()
                    val expansionSpeed = 2f + (energy * 20f)
                    wave.radius += expansionSpeed
                    wave.alpha = (1f - (wave.radius / 1500f)).coerceIn(0f, 1f)
                    if (wave.alpha <= 0f) iterator.remove()
                }
                tick = currentTime
            }
            if (!currentIsPlaying && waves.isEmpty()) break
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val dummy = tick
        val center = Offset(size.width / 2, size.height / 2)
        val energy = (currentBounceScale - 1f).coerceAtLeast(0f)
        waves.forEach { wave ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, color.copy(alpha = wave.alpha * 0.4f), Color.Transparent),
                    center = center, radius = wave.radius + 40f
                ),
                radius = wave.radius, center = center,
                style = Stroke(width = 2f + (energy * 10f))
            )
        }
    }
}