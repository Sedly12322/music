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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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
import androidx.compose.ui.graphics.graphicsLayer
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
    val isPowerSaveMode by viewModel.isPowerSaveMode.collectAsState()
    val auroraSpeed by viewModel.auroraSpeed.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isFavorite = favoriteIds.contains(song.uri.toString())

    val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsState()
    val sleepTimerFinishTrack by viewModel.sleepTimerFinishTrack.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

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
    val shadowPaint = remember { Paint() }

    var activeOverlay by remember { mutableStateOf(PlayerOverlayType.QUEUE) }

    val playPauseCoverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.91f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 260f),
        label = "playPauseScale"
    )
    val playButtonCorner by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 38.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "playCorner"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isAuroraEnabled && !isPowerSaveMode) {
            AuroraBackground(color1 = color1, color2 = color2, bounceScale = animatedBounceScale, speedMultiplier = auroraSpeed)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(color1, color2, Color.Black))))
        }

        if (isPulseEnabled && !isPowerSaveMode) {
            PulseBackground(color = contentColor, bounceScale = animatedBounceScale, isPlaying = isPlaying, modifier = Modifier.alpha(0.5f))
        }

        if (isParticlesEnabled && !isPowerSaveMode) {
            ParticleBackground(color = contentColor, bounceScale = animatedBounceScale, isPlaying = isPlaying, modifier = Modifier.alpha(0.6f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenWidth = it.width }
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HORNÍ LIŠTA ---
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.28f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.1f))
                        .border(1.dp, contentColor.copy(alpha = 0.14f), CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        "Skrýt",
                        modifier = Modifier.size(26.dp),
                        tint = contentColor
                    )
                }

                // Středová pilulka se zařízením, formátem, případně rychlostí a časovačem
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(contentColor.copy(alpha = 0.1f))
                        .border(1.dp, contentColor.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    val icon = when {
                        outputDeviceName.contains("Bluetooth", true) -> Icons.Rounded.Bluetooth
                        outputDeviceName.contains("Sluchátka", true) -> Icons.Rounded.Headphones
                        else -> Icons.Rounded.Speaker
                    }
                    Icon(icon, null, modifier = Modifier.size(14.dp), tint = contentColor.copy(alpha = 0.85f))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = outputDeviceName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (song.fileType.isNotEmpty()) {
                        Text(
                            text = "  •  ${song.fileType}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                    if (playbackSpeed != 1.0f) {
                        Text(
                            text = "  •  ${playbackSpeed}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFBB86FC)
                        )
                    }
                    if (sleepTimerRemainingSec > 0 || sleepTimerFinishTrack) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Rounded.Bedtime, null, modifier = Modifier.size(13.dp), tint = Color(0xFF1DB954))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background( if (isFavorite) Color(0xFFFF4081).copy(alpha = 0.18f) else contentColor.copy(alpha = 0.1f) )
                        .border(1.dp, if (isFavorite) Color(0xFFFF4081).copy(alpha = 0.45f) else contentColor.copy(alpha = 0.14f), CircleShape)
                        .clickable { viewModel.toggleFavorite(song) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Oblíbené",
                        tint = if (isFavorite) Color(0xFFFF4081) else contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.08f))

            // --- OBAL ALBA (S GPU graphicsLayer pro plynulé animace bez rekompozice) ---
            val coverShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val bounce = if (isCoverBounceEnabled && !isPowerSaveMode) (1f + (animatedBounceScale - 1f) * 0.25f) else 1f
                        val totalScale = bounce * playPauseCoverScale
                        translationX = coverOffsetX.value
                        scaleX = totalScale
                        scaleY = totalScale
                    }
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val frameworkPaint = shadowPaint.asFrameworkPaint()
                            frameworkPaint.color = android.graphics.Color.TRANSPARENT
                            frameworkPaint.setShadowLayer(
                                90f, 0f, 24f,
                                color1.copy(alpha = 0.65f).toArgb()
                            )
                            canvas.drawRoundRect(
                                0f, 0f, size.width, size.height,
                                28.dp.toPx(), 28.dp.toPx(), shadowPaint
                            )
                        }
                    }
                    .clip(coverShape)
                    .background(Color(0xFF22222C))
                    .border(1.5.dp, contentColor.copy(alpha = 0.16f), coverShape)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val threshold = screenWidth * 0.25f
                                if (coverOffsetX.value > threshold) {
                                    scope.launch {
                                        coverOffsetX.animateTo(screenWidth.toFloat(), tween(200))
                                        viewModel.skipPrevious()
                                        coverOffsetX.snapTo(-screenWidth.toFloat())
                                        coverOffsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f))
                                    }
                                } else if (coverOffsetX.value < -threshold) {
                                    scope.launch {
                                        coverOffsetX.animateTo(-screenWidth.toFloat(), tween(200))
                                        viewModel.skipNext()
                                        coverOffsetX.snapTo(screenWidth.toFloat())
                                        coverOffsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f))
                                    }
                                } else {
                                    scope.launch { coverOffsetX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                scope.launch { coverOffsetX.snapTo(coverOffsetX.value + dragAmount) }
                                change.consume()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Album,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(84.dp)
                )
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- NÁZEV SKLADBY A INTERPRET ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = (1f - (abs(coverOffsetX.value) / (screenWidth / 2f).coerceAtLeast(1f))).coerceIn(0f, 1f)
                    }
            ) {
                Text(
                    text = song.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    color = contentColor,
                    style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), blurRadius = 10f)),
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.4f), blurRadius = 8f)),
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.weight(0.08f))

            RealAudioVisualizer(
                viewModel,
                contentColor,
                Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            PlayerProgressBar(viewModel = viewModel, contentColor = contentColor, duration = duration)

            Spacer(modifier = Modifier.height(14.dp))

            // --- OVLÁDACÍ TLAČÍTKA PŘEHRÁVÁNÍ ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (shuffleModeEnabled) contentColor.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { viewModel.toggleShuffle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) contentColor else contentColor.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.1f))
                        .clickable { viewModel.skipPrevious() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SkipPrevious, null, modifier = Modifier.size(32.dp), tint = contentColor)
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(playButtonCorner))
                        .background(contentColor)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = if (contentColor.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.1f))
                        .clickable { viewModel.skipNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(32.dp), tint = contentColor)
                }

                val repeatIcon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                    else -> Icons.Rounded.Repeat
                }
                val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isRepeatActive) contentColor.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { viewModel.toggleRepeat() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (isRepeatActive) contentColor else contentColor.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.08f))

            // --- SPODNÍ DOCK (FRONTA & TEXT PÍSNĚ) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(contentColor.copy(alpha = 0.1f))
                        .border(1.dp, contentColor.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                        .clickable { activeOverlay = PlayerOverlayType.QUEUE; onSheetStateChange(true) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Fronta", modifier = Modifier.size(20.dp), tint = contentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fronta", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = contentColor)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(contentColor.copy(alpha = 0.1f))
                        .border(1.dp, contentColor.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                        .clickable { activeOverlay = PlayerOverlayType.LYRICS; onSheetStateChange(true) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lyrics, "Text písně", modifier = Modifier.size(20.dp), tint = contentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Text písně", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = contentColor)
                }
            }
        }

        AnimatedVisibility(
            visible = isSheetOpen,
            enter = slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(260)) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(220)) + fadeOut(animationSpec = tween(180))
        ) {
            if (activeOverlay == PlayerOverlayType.QUEUE) {
                QueueOverlay(
                    viewModel = viewModel,
                    isPlaying = isPlaying,
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
                    accentColor = color1,
                    queueItems = queueItems,
                    currentIndex = currentQueueIndex,
                    onDismiss = { onSheetStateChange(false) },
                    onPlayItem = { viewModel.playQueueItem(it) },
                    onRemoveItem = { viewModel.removeFromQueue(it) },
                    onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                    onClearManualQueue = { viewModel.clearManualQueue() },
                    onClearQueue = {
                        viewModel.clearQueue()
                        onSheetStateChange(false)
                    },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.skipNext() },
                    onPrev = { viewModel.skipPrevious() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() }
                )
            } else {
                LyricsOverlay(
                    song = song,
                    isPlaying = isPlaying,
                    duration = duration,
                    color1 = color1,
                    color2 = color2,
                    lyricsLines = lyricsLines,
                    currentIndex = currentLyricIndex,
                    isLoading = isLoadingLyrics,
                    viewModel = viewModel,
                    onSeekTo = { timeMs -> viewModel.seekTo(timeMs.toFloat()) },
                    onRetry = { viewModel.fetchLyrics(song.artist, song.title) },
                    onDismiss = { onSheetStateChange(false) }
                )
            }
        }
    }
}

@Composable
fun PlayerProgressBar(viewModel: MusicViewModel, contentColor: Color, duration: Long) {
    val currentPos by viewModel.progress.collectAsState()
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val safeDuration = duration.toFloat().coerceAtLeast(1f)

    LaunchedEffect(currentPos) {
        if (!isDraggingSlider) {
            sliderPosition = currentPos.toFloat().coerceIn(0f, safeDuration)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition.coerceIn(0f, safeDuration),
            onValueChange = { isDraggingSlider = true; sliderPosition = it },
            onValueChangeFinished = { viewModel.seekTo(sliderPosition); isDraggingSlider = false },
            valueRange = 0f..safeDuration,
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
    viewModel: MusicViewModel,
    isPlaying: Boolean,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    accentColor: Color,
    queueItems: List<QueueItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onClearManualQueue: () -> Unit,
    onClearQueue: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    val listState = rememberLazyListState()
    val spotifyGreen = Color(0xFF1DB954)

    val currentItem = remember(queueItems, currentIndex) { queueItems.find { it.index == currentIndex } }
    val manualItems = remember(queueItems, currentIndex) { queueItems.filter { it.index > currentIndex && it.isManual } }
    val autoItems = remember(queueItems, currentIndex) { queueItems.filter { it.index > currentIndex && !it.isManual } }

    val bgBrush = remember(accentColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color(
                    red = accentColor.red * 0.35f,
                    green = accentColor.green * 0.35f,
                    blue = accentColor.blue * 0.35f,
                    alpha = 1f
                ),
                Color(0xFF121212),
                Color(0xFF0A0A0E)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .background(bgBrush)
            .systemBarsPadding()
    ) {
        // --- SPOTIFY-STYLE HLAVIČKA FRONTY ---
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 4.dp)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Zavřít", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Fronta", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "${manualItems.size + autoItems.size} následujících skladeb",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }
            if (queueItems.isNotEmpty()) {
                TextButton(onClick = onClearQueue) {
                    Text("Stop", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (queueItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Fronta je prázdná", color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 20.dp, start = 16.dp, end = 16.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. PRÁVĚ HRAJE (Now Playing)
                if (currentItem != null) {
                    item {
                        Text(
                            text = "Právě hraje",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                        )
                    }
                    item(key = "current_${currentItem.index}_${currentItem.song.uri}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(1.dp, spotifyGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF252530)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(22.dp))
                                AsyncImage(
                                    model = currentItem.song.albumArt,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentItem.song.title,
                                    color = spotifyGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentItem.song.artist,
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            com.example.musicapp.ui.components.AnimatedEqIndicator(color = spotifyGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }

                // 2. DALŠÍ VE FRONTĚ (Next in Queue - ručně přidané)
                if (manualItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Další ve frontě",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Vymazat frontu",
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { onClearManualQueue() }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                    itemsIndexed(items = manualItems, key = { _, it -> "manual_${it.index}_${it.song.uri}" }) { pos, qItem ->
                        SpotifyQueueRow(
                            queueItem = qItem,
                            isManual = true,
                            canMoveUp = pos > 0,
                            canMoveDown = pos < manualItems.lastIndex,
                            onPlayItem = onPlayItem,
                            onRemoveItem = onRemoveItem,
                            onMoveUp = { onMoveItem(qItem.index, qItem.index - 1) },
                            onMoveDown = { onMoveItem(qItem.index, qItem.index + 1) }
                        )
                    }
                }

                // 3. DALŠÍ Z KNIHOVNY (Next From)
                if (autoItems.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) {
                            Text(
                                text = "Následující",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (shuffleModeEnabled) "Náhodné přehrávání zapnuto" else "Automaticky z aktuálního výběru",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    itemsIndexed(items = autoItems, key = { _, it -> "auto_${it.index}_${it.song.uri}" }) { pos, qItem ->
                        SpotifyQueueRow(
                            queueItem = qItem,
                            isManual = false,
                            canMoveUp = pos > 0,
                            canMoveDown = pos < autoItems.lastIndex,
                            onPlayItem = onPlayItem,
                            onRemoveItem = onRemoveItem,
                            onMoveUp = { onMoveItem(qItem.index, qItem.index - 1) },
                            onMoveDown = { onMoveItem(qItem.index, qItem.index + 1) }
                        )
                    }
                }
            }
        }

        // --- SPODNÍ OVLÁDACÍ PANEL PO VZORU SPOTIFY FRONTY ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF181820))
        ) {
            QueueLinearProgress(viewModel = viewModel, color = spotifyGreen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) spotifyGreen else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Předchozí", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Další", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                val repeatIcon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                    else -> Icons.Rounded.Repeat
                }
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) spotifyGreen else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueLinearProgress(viewModel: MusicViewModel, color: Color) {
    val currentPos by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val fraction = if (duration > 0L) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth().height(2.5.dp),
        color = color,
        trackColor = Color.White.copy(alpha = 0.12f)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyQueueRow(
    queueItem: QueueItem,
    isManual: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onRemoveItem(queueItem.index)
                true
            } else false
        },
        positionalThreshold = { it * 0.35f }
    )
    val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (isSwiping) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.85f))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Odebrat", tint = Color.White)
                }
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isManual) Color(0xFF1E2229) else Color(0xFF17171E))
                    .clickable { onPlayItem(queueItem.index) }
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262632)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(20.dp))
                    AsyncImage(
                        model = queueItem.song.albumArt,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = queueItem.song.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isManual) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1DB954))
                            )
                        }
                        Text(
                            text = queueItem.song.artist,
                            color = Color.White.copy(alpha = 0.58f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Posunout výše",
                            tint = if (canMoveUp) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Posunout níže",
                            tint = if (canMoveDown) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onRemoveItem(queueItem.index) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Odebrat z fronty",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun LyricsOverlay(
    song: Song,
    isPlaying: Boolean,
    duration: Long,
    color1: Color,
    color2: Color,
    lyricsLines: List<LyricLine>,
    currentIndex: Int,
    isLoading: Boolean,
    viewModel: MusicViewModel,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val isSynced = lyricsLines.firstOrNull()?.isSynced == true
    val isNotFound = lyricsLines.size == 1 && lyricsLines.first().text == "Text nebyl nalezen"

    LaunchedEffect(currentIndex) {
        if (isSynced && currentIndex >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(currentIndex, scrollOffset = -260)
        }
    }

    val lyricsBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(
                red = (color1.red * 0.65f).coerceIn(0.08f, 0.7f),
                green = (color1.green * 0.65f).coerceIn(0.08f, 0.7f),
                blue = (color1.blue * 0.65f).coerceIn(0.08f, 0.7f),
                alpha = 1f
            ),
            Color(
                red = (color2.red * 0.45f).coerceIn(0.05f, 0.45f),
                green = (color2.green * 0.45f).coerceIn(0.05f, 0.45f),
                blue = (color2.blue * 0.45f).coerceIn(0.05f, 0.45f),
                alpha = 1f
            ),
            Color(0xFF0B0B10)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101016))
            .background(lyricsBgBrush)
            .systemBarsPadding()
    ) {
        // --- HLAVIČKA TEXTU PÍSNĚ ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Zavřít", tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
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
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isLoading && !isNotFound && lyricsLines.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSynced) "SYNC" else "TEXT",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // --- OBSAH TEXTU PÍSNĚ ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Hledám text písně...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }
                isNotFound -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.Lyrics, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Text písně nebyl nalezen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Pro tuto skladbu se nepodařilo najít text v databázi LrcLib ani Lyrics.ovh.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Zkusit znovu", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 28.dp, bottom = 120.dp, start = 20.dp, end = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(lyricsLines) { index, line ->
                            val isActive = isSynced && index == currentIndex
                            val isPassed = isSynced && currentIndex >= 0 && index < currentIndex

                            val targetAlpha = when {
                                !isSynced -> 0.92f
                                isActive -> 1f
                                isPassed -> 0.32f
                                else -> 0.55f
                            }
                            val animatedAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(250), label = "lyricAlpha")
                            val scale by animateFloatAsState(targetValue = if (isActive) 1.03f else 1f, animationSpec = tween(250), label = "lyricScale")

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isActive) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable(enabled = line.isSynced) { onSeekTo(line.time) }
                                    .padding(vertical = 10.dp, horizontal = 14.dp)
                            ) {
                                Text(
                                    text = line.text,
                                    color = Color.White.copy(alpha = animatedAlpha),
                                    fontSize = if (isActive) 24.sp else 21.sp,
                                    fontWeight = if (isActive || !isSynced) FontWeight.ExtraBold else FontWeight.Bold,
                                    textAlign = TextAlign.Start,
                                    lineHeight = 31.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SPODNÍ OVLÁDÁNÍ PŘEHRÁVÁNÍ V TEXTU PÍSNĚ ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            PlayerProgressBar(viewModel = viewModel, contentColor = Color.White, duration = duration)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.skipPrevious() }) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Předchozí", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(onClick = { viewModel.skipNext() }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Další", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}