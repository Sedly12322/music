package com.example.musicapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlin.random.Random

@Composable
fun ParticleBackground(
    color: Color,
    bounceScale: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Definice počtu částic
    val particleCount = 70

    // Vytvoření stabilních polí pro data částic (aby se nerekomponovalo při každém snímku)
    val xPos = remember { FloatArray(particleCount) { Random.nextFloat() } }
    val yPos = remember { FloatArray(particleCount) { Random.nextFloat() } }
    val speeds = remember { FloatArray(particleCount) { Random.nextFloat() * 0.5f + 0.2f } }
    val radii = remember { FloatArray(particleCount) { Random.nextFloat() * 5f + 2f } }
    val alphas = remember { FloatArray(particleCount) { Random.nextFloat() * 0.3f + 0.1f } }
    val angles = remember { FloatArray(particleCount) { Random.nextFloat() * 0.4f - 0.2f } } // Mírný rozptyl do stran

    // Stav pro vynucení překreslení (tick)
    var tick by remember { mutableLongStateOf(0L) }

    val currentBounceScale by rememberUpdatedState(bounceScale)

    // Hlavní animační smyčka
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (isActive) {
            withFrameNanos { time ->
                val dt = (time - lastTime) / 1_000_000_000f
                lastTime = time

                val musicEnergy = (currentBounceScale - 1f).coerceAtLeast(0f)
                // Základní rychlost + násobič podle hudby
                val speedMultiplier = 1.0f + (musicEnergy * 18f)

                for (i in 0 until particleCount) {
                    // Pohyb vzhůru
                    yPos[i] -= speeds[i] * speedMultiplier * dt * 0.3f
                    // Mírný pohyb do stran
                    xPos[i] += angles[i] * speedMultiplier * dt * 0.1f

                    // Pokud vyletí nahoře, objeví se dole
                    if (yPos[i] < -0.05f) {
                        yPos[i] = 1.05f
                        xPos[i] = Random.nextFloat()
                    }
                    // Horizontální wrap
                    if (xPos[i] < -0.05f) xPos[i] = 1.05f
                    if (xPos[i] > 1.05f) xPos[i] = -0.05f
                }
                // Změna stavu tick vyvolá překreslení Canvasu
                tick = time
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Použijeme tick v draw bloku, aby Canvas věděl, že se má překreslit
        val dummy = tick

        val w = size.width
        val h = size.height
        val musicEnergy = (currentBounceScale - 1f).coerceAtLeast(0f)

        for (i in 0 until particleCount) {
            val drawRadius = radii[i] * (1f + musicEnergy * 1.5f)

            drawCircle(
                color = color.copy(alpha = alphas[i]),
                radius = drawRadius,
                center = Offset(xPos[i] * w, yPos[i] * h)
            )
        }
    }
}