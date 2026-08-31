package com.example.musicapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicapp.model.Song
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    progressFraction: Float,
    backgroundColor: Color,
    contentColor: Color,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetY = remember { Animatable(0f) }; val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope(); var width by remember { mutableIntStateOf(0) }
    val alpha = 1f - (offsetY.value / 150f).coerceIn(0f, 1f)

    Surface(modifier = modifier.onSizeChanged { width = it.width }.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }.alpha(alpha).height(70.dp)
        .pointerInput(Unit) { detectVerticalDragGestures(onDragEnd = { if (offsetY.value > 50f) { onDismiss() } else { scope.launch { offsetY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) } } }, onVerticalDrag = { change, dragAmount -> val newOffset = offsetY.value + dragAmount; if (dragAmount < 0 && offsetY.value <= 0) { onClick(); return@detectVerticalDragGestures }; scope.launch { offsetY.snapTo(newOffset.coerceAtLeast(0f)) }; change.consume() }) },
        color = backgroundColor, tonalElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)).pointerInput(Unit) { detectHorizontalDragGestures(onDragEnd = { val threshold = width * 0.3f; if (offsetX.value > threshold) { scope.launch { offsetX.animateTo(width.toFloat(), tween(200)); onPrev(); offsetX.snapTo(-width.toFloat()); offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f)) } } else if (offsetX.value < -threshold) { scope.launch { offsetX.animateTo(-width.toFloat(), tween(200)); onNext(); offsetX.snapTo(width.toFloat()); offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f)) } } else { scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.6f)) } } }, onHorizontalDrag = { change, dragAmount -> scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }; change.consume() }) }) {

            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = song.albumArt, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = song.title, fontWeight = FontWeight.Bold, maxLines = 1, color = contentColor, modifier = Modifier.basicMarquee())
                    Text(text = song.artist, fontSize = 12.sp, maxLines = 1, color = contentColor.copy(alpha = 0.7f), modifier = Modifier.basicMarquee())
                }
                IconButton(onClick = onPlayPauseClick) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = contentColor) }
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter),
                color = contentColor,
                trackColor = contentColor.copy(alpha = 0.1f)
            )
        }
    }
}