package com.example.musicapp.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.musicapp.model.LibraryTab
import com.example.musicapp.model.Song
import com.example.musicapp.ui.components.*
import com.example.musicapp.ui.screens.FullPlayerScreen
import com.example.musicapp.ui.screens.SettingsDialog
import com.example.musicapp.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@Composable
fun MusicAppScaffold(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val songsListState = rememberLazyListState()
    val artistsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val playlistsListState = rememberLazyListState()

    val expandProgress = remember { Animatable(0f) }
    val isFullscreen by remember { derivedStateOf { expandProgress.value > 0.01f } }

    var showBottomSheet by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx() }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val dynamicBackgroundColors by viewModel.backgroundColors.collectAsState()
    val playerContentColor by viewModel.playerContentColor.collectAsState()
    val isScreenAlwaysOn by viewModel.isScreenAlwaysOn.collectAsState()

    val currentTab by viewModel.libraryTab.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val allAlbums by viewModel.allAlbums.collectAsState()
    val allArtists by viewModel.allArtists.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val albumArtByAlbumId by viewModel.albumArtByAlbumId.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val filterTitle by viewModel.filterTitle.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val window = (context as? Activity)?.window
    LaunchedEffect(isScreenAlwaysOn) {
        if (isScreenAlwaysOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    val animatedSongTopColor by animateColorAsState(targetValue = dynamicBackgroundColors.first, animationSpec = tween(800), label = "TopColor")
    val animatedSongBottomColor by animateColorAsState(targetValue = dynamicBackgroundColors.second, animationSpec = tween(800), label = "BottomColor")
    val animatedContentColor by animateColorAsState(targetValue = playerContentColor, animationSpec = tween(800), label = "ContentColor")

    val ambientTopColor = if (currentSong != null) animatedSongTopColor.copy(alpha = 0.32f) else Color(0xFF261C42)
    val globalBrush = Brush.verticalGradient(
        colors = listOf(
            ambientTopColor,
            Color(0xFF0E0E14),
            Color(0xFF08080C)
        )
    )

    fun togglePlayer(expand: Boolean) {
        scope.launch {
            expandProgress.animateTo(
                if (expand) 1f else 0f,
                spring(dampingRatio = 0.82f, stiffness = 320f)
            )
        }
    }

    BackHandler(enabled = true) {
        when {
            showBottomSheet -> showBottomSheet = false
            expandProgress.value > 0.5f -> togglePlayer(false)
            isSearchActive -> { isSearchActive = false; searchQuery = "" }
            filteredSongs != null -> viewModel.clearFilter()
            else -> (context as? Activity)?.finish()
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val useDarkIcons = isFullscreen && playerContentColor.luminance() < 0.5f
        SideEffect {
            val w = (view.context as Activity).window
            w.statusBarColor = Color.Transparent.toArgb()
            w.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowInsetsControllerCompat(w, view)
            controller.isAppearanceLightStatusBars = useDarkIcons
        }

        val w = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(w, view)
        LaunchedEffect(isFullscreen) {
            if (expandProgress.targetValue == 1f) {
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08080C)).background(globalBrush)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // --- MODERNÍ HLAVIČKA ---
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = { fadeIn(tween(220)) + expandVertically() togetherWith fadeOut(tween(180)) + shrinkVertically() },
                label = "Header"
            ) { searching ->
                if (searching) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 10.dp)
                            .height(52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Hledat skladbu nebo umělce...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.07f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFFBB86FC)
                            ),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFFBB86FC)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Text("Zrušit", color = Color(0xFFBB86FC), fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (filteredSongs != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable { viewModel.clearFilter() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zpět", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Text(
                                    text = if (filteredSongs != null) (filterTitle ?: "Detail") else "Knihovna",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (filteredSongs != null) {
                                        "${filteredSongs?.size ?: 0} skladeb"
                                    } else {
                                        "${allSongs.size} skladeb • ${allAlbums.size} alb"
                                    },
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        if (filteredSongs == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.07f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                        .clickable { isSearchActive = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Search, "Hledat", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.07f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                        .clickable { showSettingsDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Tune, "Nastavení", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // --- MODERNÍ PILULKOVÉ ZÁLOŽKY (SEGMENTED PILLS) ---
            if (filteredSongs == null && !isSearchActive) {
                val tabs = remember {
                    listOf(
                        Triple(LibraryTab.SONGS, "Skladby", Icons.Rounded.MusicNote),
                        Triple(LibraryTab.ARTISTS, "Umělci", Icons.Rounded.Person),
                        Triple(LibraryTab.ALBUMS, "Alba", Icons.Rounded.Album),
                        Triple(LibraryTab.PLAYLISTS, "Playlisty", Icons.AutoMirrored.Rounded.QueueMusic)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.045f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEach { (tab, title, icon) ->
                        val isSelected = currentTab == tab
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFFBB86FC) else Color.Transparent,
                            animationSpec = tween(220),
                            label = "tabBg"
                        )
                        val pillText by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF120D1D) else Color.White.copy(alpha = 0.68f),
                            animationSpec = tween(220),
                            label = "tabText"
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(pillBg)
                                .clickable { viewModel.switchTab(tab) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = pillText,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = pillText,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- OBSAH KNIHOVNY ---
            Box(modifier = Modifier.weight(1f)) {
                val currentFilteredList = filteredSongs
                val playingUri = currentSong?.uri
                if (currentFilteredList != null) {
                    if (currentFilteredList.isEmpty()) {
                        EmptyStateBox(
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            title = "Žádné skladby",
                            subtitle = "Tento výběr je zatím prázdný."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = if (currentSong != null) 96.dp else 16.dp)
                        ) {
                            item {
                                QuickActionRow(
                                    onPlayAll = { viewModel.playSong(currentFilteredList.first(), currentFilteredList) },
                                    onShuffleAll = {
                                        val shuffled = currentFilteredList.shuffled()
                                        viewModel.playSong(shuffled.first(), shuffled)
                                    }
                                )
                            }
                            items(items = currentFilteredList, key = { it.id }) { song ->
                                SwipeableSongItem(
                                    song = song,
                                    isPlaying = (song.uri == playingUri),
                                    viewModel = viewModel,
                                    onMoreClick = { songToAddToPlaylist = song; showAddToPlaylistDialog = true },
                                    onQueueAdded = { scope.launch { snackbarHostState.showSnackbar("Přidáno do fronty") } },
                                    onClick = { viewModel.playSong(song, currentFilteredList) }
                                )
                            }
                        }
                    }
                } else if (isSearchActive) {
                    val filtered = remember(allSongs, searchQuery) {
                        if (searchQuery.isBlank()) allSongs
                        else allSongs.filter {
                            it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) || it.albumName.contains(searchQuery, true)
                        }
                    }
                    if (filtered.isEmpty()) {
                        EmptyStateBox(
                            icon = Icons.Rounded.SearchOff,
                            title = "Nic nenalezeno",
                            subtitle = "Zkus upravit hledaný výraz."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = if (currentSong != null) 96.dp else 16.dp)
                        ) {
                            items(items = filtered, key = { it.id }) { song ->
                                SwipeableSongItem(
                                    song = song,
                                    isPlaying = (song.uri == playingUri),
                                    viewModel = viewModel,
                                    onMoreClick = { songToAddToPlaylist = song; showAddToPlaylistDialog = true },
                                    onQueueAdded = { scope.launch { snackbarHostState.showSnackbar("Přidáno do fronty") } },
                                    onClick = { viewModel.playSong(song, filtered) }
                                )
                            }
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "TabTransition"
                    ) { targetTab ->
                        val bottomPad = if (currentSong != null) 96.dp else 16.dp
                        when (targetTab) {
                            LibraryTab.SONGS -> {
                                if (allSongs.isEmpty()) {
                                    EmptyStateBox(
                                        icon = Icons.Rounded.LibraryMusic,
                                        title = "Žádná hudba v zařízení",
                                        subtitle = "Přidej hudební soubory do telefonu nebo obnov knihovnu v nastavení."
                                    )
                                } else {
                                    LazyColumn(
                                        state = songsListState,
                                        contentPadding = PaddingValues(bottom = bottomPad)
                                    ) {
                                        item {
                                            QuickActionRow(
                                                onPlayAll = { viewModel.playSong(allSongs.first(), allSongs) },
                                                onShuffleAll = {
                                                    val shuffled = allSongs.shuffled()
                                                    viewModel.playSong(shuffled.first(), shuffled)
                                                }
                                            )
                                        }
                                        items(items = allSongs, key = { it.id }) { song ->
                                            SwipeableSongItem(
                                                song = song,
                                                isPlaying = (song.uri == playingUri),
                                                viewModel = viewModel,
                                                onMoreClick = { songToAddToPlaylist = song; showAddToPlaylistDialog = true },
                                                onQueueAdded = { scope.launch { snackbarHostState.showSnackbar("Přidáno do fronty") } },
                                                onClick = { viewModel.playSong(song, allSongs) }
                                            )
                                        }
                                    }
                                }
                            }
                            LibraryTab.ARTISTS -> LazyColumn(
                                state = artistsListState,
                                contentPadding = PaddingValues(bottom = bottomPad)
                            ) {
                                items(allArtists, key = { it.id }) { artist ->
                                    ArtistRow(artist = artist, viewModel = viewModel) { viewModel.selectArtist(artist) }
                                }
                            }
                            LibraryTab.ALBUMS -> LazyVerticalGrid(
                                state = albumsGridState,
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = bottomPad)
                            ) {
                                items(allAlbums, key = { it.id }) { album ->
                                    val safeCoverArt = albumArtByAlbumId[album.id] ?: album.albumArt
                                    AlbumGridItem(album = album, coverArt = safeCoverArt) { viewModel.selectAlbum(album) }
                                }
                            }
                            LibraryTab.PLAYLISTS -> Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = playlistsListState,
                                    contentPadding = PaddingValues(bottom = bottomPad + 72.dp)
                                ) {
                                    items(allPlaylists, key = { it.playlistId }) { playlist ->
                                        PlaylistRow(
                                            playlist = playlist,
                                            onDeleteClick = if (!playlist.isSystem) ({ viewModel.deletePlaylist(playlist) }) else null
                                        ) { viewModel.selectPlaylist(playlist) }
                                    }
                                }
                                ExtendedFloatingActionButton(
                                    onClick = { showCreatePlaylistDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = if (currentSong != null) 96.dp else 16.dp, end = 4.dp),
                                    containerColor = Color(0xFFBB86FC),
                                    contentColor = Color(0xFF120D1D),
                                    shape = RoundedCornerShape(18.dp),
                                    icon = { Icon(Icons.Rounded.Add, "Vytvořit playlist") },
                                    text = { Text("Nový playlist", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (currentSong != null) 94.dp else 16.dp)
        )

        if (currentSong != null) {
            val miniPlayerAlpha = (1f - expandProgress.value).coerceIn(0f, 1f)
            if (miniPlayerAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .graphicsLayer {
                            alpha = miniPlayerAlpha
                            translationY = (1f - miniPlayerAlpha) * 50.dp.toPx()
                        }
                ) {
                    MiniPlayerBar(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        viewModel = viewModel,
                        backgroundColor = animatedSongTopColor,
                        contentColor = animatedContentColor,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onClick = { togglePlayer(true) },
                        onDismiss = { viewModel.closePlayer() },
                        onNext = { viewModel.skipNext() },
                        onPrev = { viewModel.skipPrevious() },
                        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { if (expandProgress.value > 0.1f) togglePlayer(true) else togglePlayer(false) },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newProgress = (expandProgress.value - (dragAmount / screenHeightPx)).coerceIn(0f, 1f)
                                    scope.launch { expandProgress.snapTo(newProgress) }
                                }
                            )
                        }
                    )
                }
            }
        }

        if (currentSong != null && isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = screenHeightPx * (1f - expandProgress.value) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (showBottomSheet) return@detectVerticalDragGestures
                                if (expandProgress.value < 0.8f) togglePlayer(false) else togglePlayer(true)
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (showBottomSheet) return@detectVerticalDragGestures
                                change.consume()
                                val newProgress = (expandProgress.value - (dragAmount / screenHeightPx)).coerceIn(0f, 1f)
                                scope.launch { expandProgress.snapTo(newProgress) }
                            }
                        )
                    }
            ) {
                FullPlayerScreen(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    color1 = animatedSongTopColor,
                    color2 = animatedSongBottomColor,
                    contentColor = animatedContentColor,
                    viewModel = viewModel,
                    onBackClick = { togglePlayer(false) },
                    isSheetOpen = showBottomSheet,
                    onSheetStateChange = { showBottomSheet = it }
                )
            }
        }

        if (showSettingsDialog) SettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
        if (showCreatePlaylistDialog) {
            var newPlaylistName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text("Nový playlist", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Název playlistu") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFFBB86FC),
                            cursorColor = Color(0xFFBB86FC)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.createPlaylist(newPlaylistName); showCreatePlaylistDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Vytvořit", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Zrušit", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                containerColor = Color(0xFF181822)
            )
        }
        if (showAddToPlaylistDialog && songToAddToPlaylist != null) {
            val targetSong = songToAddToPlaylist!!
            val currentPlaylist = selectedPlaylist
            AlertDialog(
                onDismissRequest = { showAddToPlaylistDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text(targetSong.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        if (currentPlaylist != null) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.removeSongFromPlaylist(currentPlaylist, targetSong)
                                            showAddToPlaylistDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFFF5252))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Odebrat z playlistu ${currentPlaylist.name}", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAddToPlaylistDialog = false; showCreatePlaylistDialog = true }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = Color(0xFFBB86FC))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Nový playlist", color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                        items(allPlaylists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist, targetSong)
                                        showAddToPlaylistDialog = false
                                        scope.launch { snackbarHostState.showSnackbar("Přidáno do ${playlist.name}") }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (playlist.isSystem) Icons.Rounded.Favorite else Icons.AutoMirrored.Rounded.QueueMusic,
                                    null,
                                    tint = if (playlist.isSystem) Color(0xFFFF6B9D) else Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(playlist.name, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddToPlaylistDialog = false }) {
                        Text("Zavřít", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                containerColor = Color(0xFF181822)
            )
        }
    }
}

@Composable
fun QuickActionRow(onPlayAll: () -> Unit, onShuffleAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFBB86FC).copy(alpha = 0.16f))
                .border(1.dp, Color(0xFFBB86FC).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .clickable { onPlayAll() }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFFBB86FC), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Přehrát vše", color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .clickable { onShuffleAll() }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Náhodně", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun EmptyStateBox(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSongItem(
    song: Song,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    onMoreClick: () -> Unit,
    onQueueAdded: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                viewModel.addToQueue(song)
                onQueueAdded()
                return@rememberSwipeToDismissBoxState false
            }
            false
        }
    )

    val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            if (isSwiping) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1DB954).copy(alpha = 0.25f))
                        .border(1.dp, Color(0xFF1DB954).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Přidat do fronty", tint = Color(0xFF1DB954))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Přidat do fronty", color = Color(0xFF1DB954), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        content = {
            SongListItem(
                song = song,
                isPlaying = isPlaying,
                onMoreClick = onMoreClick,
                onClick = onClick
            )
        }
    )
}