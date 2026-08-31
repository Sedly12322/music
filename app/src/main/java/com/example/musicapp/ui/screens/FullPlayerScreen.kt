package com.example.musicapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.musicapp.model.LyricLine
import com.example.musicapp.model.Song
import com.example.musicapp.ui.components.AuroraBackground
import com.example.musicapp.ui.components.ParticleBackground
import com.example.musicapp.ui.components.PulseBackground
import com.example.musicapp.ui.components.RealAudioVisualizer
import com.example.musicapp.utils.formatTime
import com.example.musicapp.viewmodel.MusicViewModel
import com.example.musicapp.viewmodel.QueueItem
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PlayerOverlayType { QUEUE, LYRICS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    color1: Color,
    color2: Color,
    contentColor: Color,
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    isSheetOpen: Boolean,
    onSheetStateChange: (Boolean) -> Unit
) {
    val duration by viewModel.duration.collectAsState()
    val targetBounceScale by viewModel.targetBounceScale.collectAsState()
    val animatedBounceScale by animateFloatAsState(targetValue = targetBounceScale, animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f))
    val outputDeviceName by viewModel.outputDeviceName.collectAsState()

    val isAuroraEnabled by viewModel.isAuroraEnabled.collectAsState()
    val isCoverBounceEnabled by viewModel.isCoverBounceEnabled.collectAsState()
    val isParticlesEnabled by viewModel.isParticlesEnabled.collectAsState()
    val isPulseEnabled by viewModel.isPulseEnabled.collectAsState()

    val lyricsLines by viewModel.lyricsLines.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

    val queueItems by viewModel.queueItems.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()

    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    val coverOffsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var screenWidth by remember { mutableIntStateOf(0) }

    var activeOverlay by remember { mutableStateOf(PlayerOverlayType.QUEUE) }

    val subtleBounceScale = if (isCoverBounceEnabled) (1f + (animatedBounceScale - 1f) * 0.25f) else 1f

    Box(modifier = Modifier.fillMaxSize()) {
        if (isAuroraEnabled) {
            AuroraBackground(color1 = color1, color2 = color2, bounceScale = animatedBounceScale)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(color1, color2, Color.Black))))
        }

        if (isPulseEnabled) {
            PulseBackground(color = contentColor, bounceScale = animatedBounceScale, isPlaying = isPlaying, modifier = Modifier.alpha(0.5f))
        }

        if (isParticlesEnabled) {
            ParticleBackground(color = contentColor, bounceScale = animatedBounceScale, isPlaying = isPlaying, modifier = Modifier.alpha(0.6f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenWidth = it.width }
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activeOverlay = PlayerOverlayType.QUEUE; onSheetStateChange(true) }) { Icon(Icons.Rounded.QueueMusic, "Fronta", modifier = Modifier.size(28.dp), tint = contentColor) }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.7f)) {
                        val icon = when { outputDeviceName.contains("Bluetooth", true) -> Icons.Rounded.Bluetooth; outputDeviceName.contains("Sluchátka", true) -> Icons.Rounded.Headphones; else -> Icons.Rounded.Speaker }
                        Icon(icon, null, modifier = Modifier.size(12.dp), tint = contentColor); Spacer(modifier = Modifier.width(4.dp))
                        Text(text = outputDeviceName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onBackClick) { Icon(Icons.Rounded.KeyboardArrowDown, "Skrýt", modifier = Modifier.size(32.dp), tint = contentColor.copy(alpha = 0.7f)) }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.border(1.dp, contentColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(text = song.fileType, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.8f)) }
                }
                IconButton(onClick = { activeOverlay = PlayerOverlayType.LYRICS; onSheetStateChange(true) }) { Icon(Icons.Rounded.Description, "Text", modifier = Modifier.size(28.dp), tint = contentColor) }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .offset { IntOffset(coverOffsetX.value.roundToInt(), 0) }
                    .scale(subtleBounceScale)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint()
                            val frameworkPaint = paint.asFrameworkPaint()
                            frameworkPaint.color = android.graphics.Color.TRANSPARENT
                            frameworkPaint.setShadowLayer(
                                80f, 0f, 20f,
                                color1.copy(alpha = 0.5f).toArgb()
                            )
                            canvas.drawRoundRect(
                                0f, 0f, size.width, size.height,
                                24.dp.toPx(), 24.dp.toPx(), paint
                            )
                        }
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.DarkGray)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { val threshold = screenWidth * 0.25f; if (coverOffsetX.value > threshold) { scope.launch { coverOffsetX.animateTo(screenWidth.toFloat(), tween(200)); viewModel.skipPrevious(); coverOffsetX.snapTo(-screenWidth.toFloat()); coverOffsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) } } else if (coverOffsetX.value < -threshold) { scope.launch { coverOffsetX.animateTo(-screenWidth.toFloat(), tween(200)); viewModel.skipNext(); coverOffsetX.snapTo(screenWidth.toFloat()); coverOffsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) } } else { scope.launch { coverOffsetX.animateTo(0f, spring(dampingRatio = 0.6f)) } } },
                            onHorizontalDrag = { change, dragAmount -> scope.launch { coverOffsetX.snapTo(coverOffsetX.value + dragAmount) }; change.consume() }
                        )
                    }
            ) {
                AsyncImage(model = song.albumArt, contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha((1f - (abs(coverOffsetX.value) / (screenWidth / 2))).coerceIn(0f, 1f))) {
                Text(text = song.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = contentColor, style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 8f)), modifier = Modifier.basicMarquee())
                Text(text = song.artist, fontSize = 18.sp, color = contentColor.copy(alpha = 0.7f), maxLines = 1, style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 8f)), modifier = Modifier.basicMarquee())
            }
            Spacer(modifier = Modifier.weight(0.1f))

            RealAudioVisualizer(viewModel, contentColor, Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(20.dp))

            PlayerProgressBar(viewModel = viewModel, contentColor = contentColor, duration = duration)

            Spacer(modifier = Modifier.weight(0.1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.toggleShuffle() }) { Icon(imageVector = Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffleModeEnabled) Color(0xFF1DB954) else contentColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp)) }
                IconButton(onClick = { viewModel.skipPrevious() }) { Icon(Icons.Rounded.SkipPrevious, null, modifier = Modifier.size(48.dp), tint = contentColor) }
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(contentColor).clickable { viewModel.togglePlayPause() }, contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = if (contentColor.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { viewModel.skipNext() }) { Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(48.dp), tint = contentColor) }
                val repeatIcon = when (repeatMode) { Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne else -> Icons.Rounded.Repeat }; val repeatColor = if (repeatMode == Player.REPEAT_MODE_OFF) contentColor.copy(alpha = 0.5f) else Color(0xFF1DB954)
                IconButton(onClick = { viewModel.toggleRepeat() }) { Icon(imageVector = repeatIcon, contentDescription = "Repeat", tint = repeatColor, modifier = Modifier.size(28.dp)) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        AnimatedVisibility(
            visible = isSheetOpen,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            val isContentDark = contentColor.luminance() < 0.5f
            val overlayBgColor = if (isContentDark) Color(0xFFFAFAFA) else Color(0xFF121212)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBgColor.copy(alpha = 0.95f))
                    .systemBarsPadding()
            ) {
                if (activeOverlay == PlayerOverlayType.QUEUE) {
                    QueueOverlay(
                        queueItems = queueItems,
                        currentIndex = currentQueueIndex,
                        contentColor = contentColor,
                        onDismiss = { onSheetStateChange(false) },
                        onPlayItem = { viewModel.playQueueItem(it) },
                        onRemoveItem = { viewModel.removeFromQueue(it) },
                        onClearQueue = {
                            viewModel.clearQueue()
                            onSheetStateChange(false)
                        }
                    )
                } else {
                    LyricsOverlay(
                        lyricsLines = lyricsLines,
                        currentIndex = currentLyricIndex,
                        isLoading = isLoadingLyrics,
                        contentColor = contentColor,
                        onDismiss = { onSheetStateChange(false) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerProgressBar(viewModel: MusicViewModel, contentColor: Color, duration: Long) {
    val currentPos by viewModel.progress.collectAsState()
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentPos) {
        if (!isDraggingSlider) {
            sliderPosition = currentPos.toFloat()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { isDraggingSlider = true; sliderPosition = it },
            onValueChangeFinished = { viewModel.seekTo(sliderPosition); isDraggingSlider = false },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = contentColor,
                activeTrackColor = contentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.3f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatTime(if (isDraggingSlider) sliderPosition.toLong() else currentPos), fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
            Text(text = formatTime(duration), fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueOverlay(
    queueItems: List<QueueItem>,
    currentIndex: Int,
    contentColor: Color,
    onDismiss: () -> Unit,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearQueue: () -> Unit
) {
    val listState = rememberLazyListState()
    val removedIndices = remember { mutableStateListOf<Int>() }
    LaunchedEffect(queueItems) { removedIndices.clear() }

    val currentItem = queueItems.find { it.index == currentIndex }
    val manualItems = queueItems.filter { it.index > currentIndex && it.isManual }
    val autoItems = queueItems.filter { it.index > currentIndex && !it.isManual }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Zpět", tint = contentColor) }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Fronta", color = contentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (queueItems.isNotEmpty()) {
                TextButton(onClick = onClearQueue) { Text("Vymazat vše", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Bold) }
            }
        }

        HorizontalDivider(color = contentColor.copy(alpha = 0.1f))

        if (queueItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Fronta je prázdná", color = contentColor.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentItem != null) {
                    item { Text("Právě hraje", color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)) }
                    item { QueueSongItemRow(currentItem, true, contentColor, removedIndices, onPlayItem, onRemoveItem) }
                }

                if (manualItems.isNotEmpty()) {
                    item { Text("Ve frontě", color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp, top = 16.dp)) }
                    items(items = manualItems, key = { it.index }) { qItem -> QueueSongItemRow(qItem, false, contentColor, removedIndices, onPlayItem, onRemoveItem) }
                }

                if (autoItems.isNotEmpty()) {
                    item { Text("Následující", color = contentColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp, top = 16.dp)) }
                    items(items = autoItems, key = { it.index }) { qItem -> QueueSongItemRow(qItem, false, contentColor, removedIndices, onPlayItem, onRemoveItem) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSongItemRow(
    queueItem: QueueItem,
    isCurrent: Boolean,
    contentColor: Color,
    removedIndices: MutableList<Int>,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    if (removedIndices.contains(queueItem.index)) return

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { removedIndices.add(queueItem.index); onRemoveItem(queueItem.index); true } else false },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = Color.Red.copy(alpha = 0.8f)
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Rounded.Delete, null, tint = Color.White) }
        },
        content = {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(contentColor.copy(alpha = 0.1f)).border(1.dp, contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).clickable { onPlayItem(queueItem.index) }.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) Icon(Icons.Rounded.GraphicEq, null, tint = Color(0xFF1DB954), modifier = Modifier.size(24.dp))
                else Icon(Icons.Rounded.MusicNote, null, tint = contentColor.copy(alpha = 0.3f), modifier = Modifier.size(24.dp).padding(4.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(queueItem.song.title, color = if (isCurrent) Color(0xFF1DB954) else contentColor, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(queueItem.song.artist, color = contentColor.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.DragHandle, null, tint = contentColor.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            }
        }
    )
}

@Composable
fun LyricsOverlay(
    lyricsLines: List<LyricLine>,
    currentIndex: Int,
    isLoading: Boolean,
    contentColor: Color,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) { if (currentIndex >= 0 && !listState.isScrollInProgress) listState.animateScrollToItem(currentIndex, scrollOffset = -300) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Zpět", tint = contentColor) }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Text písně", color = contentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = contentColor) }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)) {
                itemsIndexed(lyricsLines) { index, line ->
                    val isActive = index == currentIndex
                    val alpha = if (isActive) 1f else 0.5f
                    val scale by animateFloatAsState(targetValue = if (isActive) 1.1f else 1f, animationSpec = tween(300))
                    Text(text = line.text, color = contentColor.copy(alpha = alpha), fontSize = 24.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 36.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).scale(scale))
                }
            }
        }
    }
}