package com.example.musicapp.ui

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val filterTitle by viewModel.filterTitle.collectAsState()

    // Výpočet progresu pro MiniPlayer
    val currentPos by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val progressFraction = if (duration > 0L) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val window = (context as? Activity)?.window
    LaunchedEffect(isScreenAlwaysOn) { if (isScreenAlwaysOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

    val animatedSongTopColor by animateColorAsState(targetValue = dynamicBackgroundColors.first, animationSpec = tween(800), label = "TopColor")
    val animatedSongBottomColor by animateColorAsState(targetValue = dynamicBackgroundColors.second, animationSpec = tween(800), label = "BottomColor")
    val animatedContentColor by animateColorAsState(targetValue = playerContentColor, animationSpec = tween(800), label = "ContentColor")
    val globalBrush = Brush.verticalGradient(colors = listOf(Color(0xFF121212), Color.Black))

    fun togglePlayer(expand: Boolean) {
        scope.launch {
            expandProgress.animateTo(
                if (expand) 1f else 0f,
                spring(dampingRatio = 0.8f, stiffness = 300f)
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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION; context.contentResolver.takePersistableUriPermission(it, flags); viewModel.setRootFolder(it) } }

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

    Box(modifier = Modifier.fillMaxSize().background(globalBrush)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            AnimatedContent(targetState = isSearchActive, transitionSpec = { fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically() }, label = "Header") { searching ->
                if (searching) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Hledat...", color = Color.Gray) },
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF2B2B2B), unfocusedContainerColor = Color(0xFF2B2B2B), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = Color.White),
                            singleLine = true, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Rounded.Close, null, tint = Color.Gray) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                        TextButton(onClick = { isSearchActive = false; searchQuery = "" }) { Text("Zrušit", color = Color.White) }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp).height(56.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (filteredSongs != null) IconButton(onClick = { viewModel.clearFilter() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zpět", tint = Color.White) }
                            Column { Text(if (filteredSongs != null) filterTitle ?: "Detail" else "Knihovna", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1) }
                        }
                        Row {
                            if (filteredSongs == null) {
                                IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Rounded.Search, "Hledat", tint = Color.White) }
                                IconButton(onClick = { viewModel.updateSystemVolume(); showSettingsDialog = true }) { Icon(Icons.Rounded.Settings, "Nastavení", tint = Color.White) }
                            }
                        }
                    }
                }
            }

            if (filteredSongs == null && !isSearchActive) {
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {},
                    indicator = { tabPositions ->
                        if (currentTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[currentTab.ordinal])
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                                color = Color(0xFFBB86FC),
                                height = 4.dp
                            )
                        }
                    }
                ) {
                    val tabs = listOf(
                        LibraryTab.SONGS to "Skladby",
                        LibraryTab.ARTISTS to "Umělci",
                        LibraryTab.ALBUMS to "Alba",
                        LibraryTab.PLAYLISTS to "Playlisty"
                    )

                    tabs.forEach { (tab, title) ->
                        val isSelected = currentTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.switchTab(tab) },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = if (isSelected) Color(0xFFBB86FC) else Color.LightGray
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                val currentFilteredList = filteredSongs
                if (currentFilteredList != null) {
                    LazyColumn {
                        items(items = currentFilteredList, key = { it.id }) { song ->
                            SwipeableSongItem(
                                song = song,
                                currentSong = currentSong,
                                viewModel = viewModel,
                                onMoreClick = { songToAddToPlaylist = song; showAddToPlaylistDialog = true },
                                onQueueAdded = { scope.launch { snackbarHostState.showSnackbar("Přidáno do fronty") } },
                                onClick = { viewModel.playSong(song, currentFilteredList) }
                            )
                        }
                    }
                } else if (isSearchActive) {
                    val filtered = allSongs.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
                    LazyColumn {
                        items(items = filtered, key = { it.id }) { song ->
                            SwipeableSongItem(
                                song = song,
                                currentSong = currentSong,
                                viewModel = viewModel,
                                onMoreClick = { songToAddToPlaylist = song; showAddToPlaylistDialog = true },
                                onQueueAdded = { scope.launch { snackbarHostState.showSnackbar("Přidáno do fronty") } },
                                onClick = { viewModel.playSong(song, filtered) }
                            )
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                        },
                        label = "TabTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            LibraryTab.SONGS -> LazyColumn(state = songsListState) {
                                items(items = allSongs, key = { it.id }) { song ->
                                    SwipeableSongItem(
                                        song = song,
                                        currentSong = currentSong,
                                        viewModel = viewModel,
                                        onMoreClick = { songToAddToPlaylist = song; showAddToPlaylistDialog = true },
                                        onQueueAdded = { scope.launch { snackbarHostState.showSnackbar("Přidáno do fronty") } },
                                        onClick = { viewModel.playSong(song, allSongs) }
                                    )
                                }
                            }
                            LibraryTab.ARTISTS -> LazyColumn(state = artistsListState) { items(allArtists) { artist -> ArtistRow(artist = artist, viewModel = viewModel) { viewModel.selectArtist(artist) } } }
                            LibraryTab.ALBUMS -> LazyVerticalGrid(
                                state = albumsGridState,
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(allAlbums) { album ->
                                    val safeCoverArt = allSongs.find { it.albumId == album.id }?.albumArt ?: album.albumArt
                                    AlbumGridItem(album = album, coverArt = safeCoverArt) { viewModel.selectAlbum(album) }
                                }
                            }
                            LibraryTab.PLAYLISTS -> Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(state = playlistsListState) { items(allPlaylists) { playlist -> PlaylistRow(playlist = playlist) { viewModel.selectPlaylist(playlist) } } }
                                FloatingActionButton(onClick = { showCreatePlaylistDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), containerColor = Color(0xFFBB86FC)) { Icon(Icons.Rounded.Add, "Vytvořit playlist", tint = Color.Black) }
                            }
                        }
                    }
                }
            }
            if (currentSong != null) Spacer(modifier = Modifier.height(80.dp))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (currentSong != null) 90.dp else 16.dp))

        if (currentSong != null) {
            val miniPlayerAlpha = (1f - expandProgress.value).coerceIn(0f, 1f)
            if (miniPlayerAlpha > 0f) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().graphicsLayer { alpha = miniPlayerAlpha; translationY = (1f - miniPlayerAlpha) * 50.dp.toPx() }) {
                    MiniPlayerBar(
                        song = currentSong!!, isPlaying = isPlaying,
                        progressFraction = progressFraction, // PŘEDÁVÁME ZBÝVAJÍCÍ PROGRESS
                        backgroundColor = animatedSongTopColor, contentColor = animatedContentColor,
                        onPlayPauseClick = { viewModel.togglePlayPause() }, onClick = { togglePlayer(true) }, onDismiss = { viewModel.closePlayer() },
                        onNext = { viewModel.skipNext() }, onPrev = { viewModel.skipPrevious() },
                        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { if (expandProgress.value > 0.1f) togglePlayer(true) else togglePlayer(false) },
                                onVerticalDrag = { change, dragAmount -> change.consume(); val newProgress = (expandProgress.value - (dragAmount / screenHeightPx)).coerceIn(0f, 1f); scope.launch { expandProgress.snapTo(newProgress) } }
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
                    song = currentSong!!, isPlaying = isPlaying, color1 = animatedSongTopColor, color2 = animatedSongBottomColor,
                    contentColor = animatedContentColor, viewModel = viewModel, onBackClick = { togglePlayer(false) },
                    isSheetOpen = showBottomSheet, onSheetStateChange = { showBottomSheet = it }
                )
            }
        }

        if (showSettingsDialog) SettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false }, onSelectFolder = { launcher.launch(null) })
        if (showCreatePlaylistDialog) {
            var newPlaylistName by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { showCreatePlaylistDialog = false }, title = { Text("Nový Playlist", color = Color.White) }, text = { OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, label = { Text("Název playlistu") }, singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { TextButton(onClick = { viewModel.createPlaylist(newPlaylistName); showCreatePlaylistDialog = false }) { Text("Vytvořit", color = Color(0xFFBB86FC)) } }, dismissButton = { TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Zrušit", color = Color.Gray) } }, containerColor = Color(0xFF1E1E1E))
        }
        if (showAddToPlaylistDialog && songToAddToPlaylist != null) {
            AlertDialog(onDismissRequest = { showAddToPlaylistDialog = false }, title = { Text("Přidat do playlistu", color = Color.White) }, text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    item { Row(modifier = Modifier.fillMaxWidth().clickable { showAddToPlaylistDialog = false; showCreatePlaylistDialog = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Add, null, tint = Color(0xFFBB86FC)); Spacer(modifier = Modifier.width(16.dp)); Text("Nový playlist", color = Color.White, fontWeight = FontWeight.Bold) }; HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f)) }
                    items(allPlaylists) { playlist -> Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.addSongToPlaylist(playlist, songToAddToPlaylist!!); showAddToPlaylistDialog = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color.Gray); Spacer(modifier = Modifier.width(16.dp)); Text(playlist.name, color = Color.White); if (playlist.isSystem) { Spacer(modifier = Modifier.width(8.dp)); Text("(Systémový)", color = Color.Gray, fontSize = 10.sp) } } }
                }
            }, confirmButton = {}, dismissButton = { TextButton(onClick = { showAddToPlaylistDialog = false }) { Text("Zavřít", color = Color.Gray) } }, containerColor = Color(0xFF1E1E1E))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSongItem(
    song: Song,
    currentSong: Song?,
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

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val color = Color(0xFF333333)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Přidat do fronty", tint = Color.White)
            }
        },
        content = {
            SongListItem(
                song = song,
                isPlaying = (song.uri == currentSong?.uri),
                onMoreClick = onMoreClick,
                onClick = onClick
            )
        }
    )
}