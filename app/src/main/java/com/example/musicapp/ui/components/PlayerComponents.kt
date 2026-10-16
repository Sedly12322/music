package com.example.musicapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicapp.model.Song
import com.example.musicapp.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    backgroundColor: Color,
    contentColor: Color,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetY = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(22.dp)

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .onSizeChanged { width = it.width }
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                alpha = 1f - (offsetY.value / 150f).coerceIn(0f, 1f)
            }
            .height(68.dp)
            .shadow(elevation = 16.dp, shape = shape, ambientColor = backgroundColor, spotColor = backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.14f), shape)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY.value > 50f) {
                            onDismiss()
                        } else {
                            scope.launch { offsetY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        val newOffset = offsetY.value + dragAmount
                        if (dragAmount < 0 && offsetY.value <= 0) {
                            onClick()
                            return@detectVerticalDragGestures
                        }
                        scope.launch { offsetY.snapTo(newOffset.coerceAtLeast(0f)) }
                        change.consume()
                    }
                )
            },
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.95f),
                            Color(
                                red = backgroundColor.red * 0.55f,
                                green = backgroundColor.green * 0.55f,
                                blue = backgroundColor.blue * 0.55f,
                                alpha = 0.98f
                            )
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = width * 0.28f
                            if (offsetX.value > threshold) {
                                scope.launch {
                                    offsetX.animateTo(width.toFloat(), tween(180))
                                    onPrev()
                                    offsetX.snapTo(-width.toFloat())
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f))
                                }
                            } else if (offsetX.value < -threshold) {
                                scope.launch {
                                    offsetX.animateTo(-width.toFloat(), tween(180))
                                    onNext()
                                    offsetX.snapTo(width.toFloat())
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f))
                                }
                            } else {
                                scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                            change.consume()
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(contentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                    AsyncImage(
                        model = song.albumArt,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        color = contentColor,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        fontSize = 12.sp,
                        maxLines = 1,
                        color = contentColor.copy(alpha = 0.72f),
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(contentColor)
                        .clickable { onPlayPauseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Přehrát / Pozastavit",
                        tint = if (contentColor.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Další",
                        tint = contentColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            MiniPlayerProgressBar(
                viewModel = viewModel,
                contentColor = contentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    viewModel: MusicViewModel,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val currentPos by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val progressFraction = if (duration > 0L) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    LinearProgressIndicator(
        progress = { progressFraction },
        modifier = modifier,
        color = contentColor,
        trackColor = contentColor.copy(alpha = 0.15f),
        strokeCap = StrokeCap.Round
    )
}