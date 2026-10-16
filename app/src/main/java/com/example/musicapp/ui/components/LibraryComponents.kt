package com.example.musicapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicapp.model.*
import com.example.musicapp.utils.formatTime
import com.example.musicapp.viewmodel.MusicViewModel

@Composable
fun AnimatedEqIndicator(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val bar1 by infiniteTransition.animateFloat(initialValue = 0.25f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(420, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "b1")
    val bar2 by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0.35f, animationSpec = infiniteRepeatable(animation = tween(310, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "b2")
    val bar3 by infiniteTransition.animateFloat(initialValue = 0.45f, targetValue = 0.95f, animationSpec = infiniteRepeatable(animation = tween(480, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "b3")

    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.2f

        drawLine(color = color, start = Offset(w * 0.22f, h * 0.85f), end = Offset(w * 0.22f, h * 0.85f - (h * 0.65f * bar1)), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.85f), end = Offset(w * 0.5f, h * 0.85f - (h * 0.75f * bar2)), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.78f, h * 0.85f), end = Offset(w * 0.78f, h * 0.85f - (h * 0.55f * bar3)), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    accentColor: Color = Color(0xFFBB86FC),
    onMoreClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val bgBrush = if (isPlaying) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF281D3A),
                Color(0xFF171522)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF161620),
                Color(0xFF12121A)
            )
        )
    }
    val borderColor = if (isPlaying) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f)
    val titleColor = if (isPlaying) accentColor else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderColor, shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF22222C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(24.dp)
            )
            AsyncImage(
                model = song.albumArt,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqIndicator(color = accentColor)
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (song.duration > 0L) {
                    Text(
                        text = " • ${formatTime(song.duration)}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        maxLines = 1
                    )
                }
                if (song.fileType.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.fileType.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        if (onMoreClick != null) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Více",
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistRow(playlist: Playlist, onDeleteClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val gradientColors = if (playlist.isSystem) {
        listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
    } else {
        listOf(Color(0xFF2A2D3E), Color(0xFF1F1C2C))
    }
    val shape = RoundedCornerShape(22.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.dp,
                color = if (playlist.isSystem) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                shape = shape
            )
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    val iconType = if (playlist.isSystem) Icons.Rounded.Favorite else Icons.AutoMirrored.Rounded.QueueMusic
                    val iconTint = if (playlist.isSystem) Color(0xFFFF6B9D) else Color(0xFFBB86FC)
                    Icon(iconType, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (playlist.isSystem) "Automatický systémový playlist" else "Vlastní kolekce skladeb",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }
                if (!playlist.isSystem && onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Smazat playlist",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun ArtistRow(artist: Artist, viewModel: MusicViewModel, onClick: () -> Unit) {
    val artistImages by viewModel.artistImages.collectAsState()
    val imageUrl = artistImages[artist.name]
    val shape = RoundedCornerShape(18.dp)

    LaunchedEffect(artist.name) {
        if (imageUrl == null) viewModel.loadArtistImage(artist.name)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF252532))
                .border(1.5.dp, Color(0xFFBB86FC).copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(30.dp))
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFBB86FC).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${artist.numberOfTracks} skladeb", color = Color(0xFFBB86FC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("${artist.numberOfAlbums} alb", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun AlbumGridItem(album: Album, coverArt: Any? = album.albumArt, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable { onClick() },
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF22222C))) {
            Icon(
                Icons.Rounded.Album,
                null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.size(64.dp).align(Alignment.Center)
            )
            if (coverArt != null) {
                AsyncImage(
                    model = coverArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            if (album.year > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = album.year.toString(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = album.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = album.artist,
                        color = Color.White.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}