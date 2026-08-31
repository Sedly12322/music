package com.example.musicapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.material3.*
import com.example.musicapp.model.*
import com.example.musicapp.viewmodel.MusicViewModel

@Composable
fun AnimatedEqIndicator(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val bar1 by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "b1")
    val bar2 by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0.4f, animationSpec = infiniteRepeatable(animation = tween(300, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "b2")
    val bar3 by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 0.9f, animationSpec = infiniteRepeatable(animation = tween(500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "b3")

    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.2f

        drawLine(color = color, start = Offset(w * 0.2f, h), end = Offset(w * 0.2f, h - (h * 0.7f * bar1)), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.5f, h), end = Offset(w * 0.5f, h - (h * 0.9f * bar2)), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.8f, h), end = Offset(w * 0.8f, h - (h * 0.6f * bar3)), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun BreadcrumbsBar(breadcrumbs: List<Breadcrumb>, onBreadcrumbClick: (Breadcrumb) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(breadcrumbs.size) { if (breadcrumbs.isNotEmpty()) listState.animateScrollToItem(breadcrumbs.lastIndex) }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(breadcrumbs) { index, crumb ->
            val isLast = index == breadcrumbs.lastIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = crumb.name,
                    color = if (isLast) Color.White else Color.Gray,
                    fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Medium,
                    fontSize = if (isLast) 22.sp else 16.sp,
                    modifier = Modifier
                        .clickable(enabled = !isLast) { onBreadcrumbClick(crumb) }
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
                if (!isLast) Icon(Icons.Rounded.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun FolderRow(item: FolderItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C22))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Folder, contentDescription = "Folder", tint = Color(0xFFFFB300), modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
    }
}

@Composable
fun SongListItem(song: Song, isPlaying: Boolean, onMoreClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val bgColor = if (isPlaying) Color(0xFFBB86FC).copy(alpha = 0.1f) else Color.Transparent
    val titleColor = if (isPlaying) Color(0xFFBB86FC) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = song.albumArt,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C2C38)),
                contentScale = ContentScale.Crop
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqIndicator(color = Color(0xFFBB86FC))
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (song.fileType.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.fileType.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Více", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    val gradientColors = if (playlist.isSystem) {
        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
    } else {
        listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    val iconType = if (playlist.isSystem) Icons.Rounded.Favorite else Icons.AutoMirrored.Rounded.QueueMusic
                    Icon(iconType, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = playlist.name, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
                    if (playlist.isSystem) {
                        Text(text = "Automatický výběr", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
fun ArtistRow(artist: Artist, viewModel: MusicViewModel, onClick: () -> Unit) {
    val artistImages by viewModel.artistImages.collectAsState()
    val imageUrl = artistImages[artist.name]

    LaunchedEffect(artist.name) {
        if (imageUrl == null) viewModel.loadArtistImage(artist.name)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C38)),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Rounded.Person, null, tint = Color.Gray, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("${artist.numberOfTracks} skladeb • ${artist.numberOfAlbums} alb", color = Color.Gray, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White)
        }
    }
}

@Composable
fun AlbumGridItem(album: Album, coverArt: Any? = album.albumArt, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverArt != null) {
                AsyncImage(
                    model = coverArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C38)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Album, null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(64.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 150f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = album.title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = album.artist,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
        }
    }
}