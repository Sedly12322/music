package com.example.musicapp.viewmodel

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.musicapp.data.FavoritesManager
import com.example.musicapp.data.MusicDatabase
import com.example.musicapp.data.MusicRepository
import com.example.musicapp.model.*
import com.example.musicapp.service.MusicService
import com.example.musicapp.utils.SPLIT_ARTIST_REGEX
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.pow

@Immutable
data class QueueItem(val index: Int, val song: Song, val isManual: Boolean)

@OptIn(UnstableApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val prefs = application.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val repository = MusicRepository(context, MusicDatabase.getDatabase(application))
    private val favoritesManager = FavoritesManager(application)

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // --- RYCHLÉ O(1) VYHLEDÁVACÍ MAPY PRO OPTIMALIZACI ---
    private var rawSongs: List<Song> = emptyList()
    private var songsByUri: Map<String, Song> = emptyMap()

    private val _albumArtByAlbumId = MutableStateFlow<Map<Long, Uri>>(emptyMap())
    val albumArtByAlbumId = _albumArtByAlbumId.asStateFlow()

    // --- STATES ---
    private val _libraryTab = MutableStateFlow(LibraryTab.SONGS)
    val libraryTab = _libraryTab.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs = _allSongs.asStateFlow()
    private val _allAlbums = MutableStateFlow<List<Album>>(emptyList())
    val allAlbums = _allAlbums.asStateFlow()
    private val _allArtists = MutableStateFlow<List<Artist>>(emptyList())
    val allArtists = _allArtists.asStateFlow()

    val allPlaylists = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _filteredSongs = MutableStateFlow<List<Song>?>(null)
    val filteredSongs = _filteredSongs.asStateFlow()
    private val _filterTitle = MutableStateFlow<String?>(null)
    val filterTitle = _filterTitle.asStateFlow()
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist = _selectedPlaylist.asStateFlow()

    private val _artistImages = MutableStateFlow<Map<String, String>>(emptyMap())
    val artistImages = _artistImages.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds = _favoriteIds.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled = _shuffleModeEnabled.asStateFlow()
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress = _progress.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _queueItems = MutableStateFlow<List<QueueItem>>(emptyList())
    val queueItems = _queueItems.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex = _currentQueueIndex.asStateFlow()

    private val _backgroundColors = MutableStateFlow(Pair(Color(0xFF2B2B2B), Color.Black))
    val backgroundColors = _backgroundColors.asStateFlow()
    private val _playerContentColor = MutableStateFlow(Color.White)
    val playerContentColor = _playerContentColor.asStateFlow()

    private val _outputDeviceName = MutableStateFlow("Telefon")
    val outputDeviceName = _outputDeviceName.asStateFlow()

    // --- VISUALIZER ---
    private val _visualizerType = MutableStateFlow(VisualizerType.BARS)
    val visualizerType = _visualizerType.asStateFlow()
    private val _isVisualizerMirrored = MutableStateFlow(true)
    val isVisualizerMirrored = _isVisualizerMirrored.asStateFlow()
    private val _visualizerData = MutableStateFlow<List<Float>>(emptyList())
    val visualizerData = _visualizerData.asStateFlow()
    private val _targetBounceScale = MutableStateFlow(1f)
    val targetBounceScale = _targetBounceScale.asStateFlow()

    private var visualizer: Visualizer? = null
    private var audioSessionId: Int = 0
    private val visualizerBarCount = 20
    private var previousMagnitudes = FloatArray(visualizerBarCount)
    private var fftMagnitudesBuffer = FloatArray(0)
    private val smoothingFactor = 0.6f

    private var progressJob: Job? = null
    private var playlistFilterJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var audioDeviceCallback: android.media.AudioDeviceCallback? = null

    // --- SETTINGS ---
    private val _isScreenAlwaysOn = MutableStateFlow(false)
    val isScreenAlwaysOn = _isScreenAlwaysOn.asStateFlow()
    private val _isAuroraEnabled = MutableStateFlow(true)
    val isAuroraEnabled = _isAuroraEnabled.asStateFlow()
    private val _isCoverBounceEnabled = MutableStateFlow(true)
    val isCoverBounceEnabled = _isCoverBounceEnabled.asStateFlow()
    private val _isParticlesEnabled = MutableStateFlow(true)
    val isParticlesEnabled = _isParticlesEnabled.asStateFlow()
    private val _isPulseEnabled = MutableStateFlow(false)
    val isPulseEnabled = _isPulseEnabled.asStateFlow()
    private val _visualizerSensitivity = MutableStateFlow(1.0f)
    val visualizerSensitivity = _visualizerSensitivity.asStateFlow()
    private val _bounceIntensity = MutableStateFlow(1.0f)
    val bounceIntensity = _bounceIntensity.asStateFlow()
    private val _auroraSpeed = MutableStateFlow(1.0f)
    val auroraSpeed = _auroraSpeed.asStateFlow()

    // --- NOVÁ NASTAVENÍ (ŘAZENÍ, RYCHLOST, PŘESKOČENÍ TICHA, ÚSPORNÝ REŽIM, ČASOVAČ) ---
    private val _sortOption = MutableStateFlow(SortOption.TITLE_ASC)
    val sortOption = _sortOption.asStateFlow()
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()
    private val _skipSilenceEnabled = MutableStateFlow(false)
    val skipSilenceEnabled = _skipSilenceEnabled.asStateFlow()
    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode = _isPowerSaveMode.asStateFlow()
    private val _sleepTimerRemainingSec = MutableStateFlow(0)
    val sleepTimerRemainingSec = _sleepTimerRemainingSec.asStateFlow()
    private val _sleepTimerFinishTrack = MutableStateFlow(false)
    val sleepTimerFinishTrack = _sleepTimerFinishTrack.asStateFlow()

    private val _lyricsLines = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyricsLines = _lyricsLines.asStateFlow()
    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex = _currentLyricIndex.asStateFlow()
    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics = _isLoadingLyrics.asStateFlow()

    init {
        loadSettings()
        loadLibrary()
        observeFavorites()
        initializeMediaController()
        registerAudioCallback()
        updateAudioOutput()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            val favPlaylist = repository.getOrCreateFavoritesPlaylist()
            val legacyFavs = favoritesManager.getFavorites()
            if (legacyFavs.isNotEmpty()) {
                legacyFavs.forEach { uriStr ->
                    repository.addSongToPlaylist(favPlaylist.playlistId, uriStr)
                }
            }
            repository.getFavoriteSongUris().collect { uris ->
                _favoriteIds.value = uris.toSet()
            }
        }
    }

    private fun registerAudioCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = updateAudioOutput()
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = updateAudioOutput()
            }
            audioDeviceCallback = callback
            audioManager.registerAudioDeviceCallback(callback, null)
        }
    }

    fun updateAudioOutput() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var name = "Telefon"
        var isBluetooth = false
        var isHeadset = false
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                isBluetooth = true
                name = device.productName.toString()
                break
            } else if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || device.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                isHeadset = true
            }
        }
        _outputDeviceName.value = if (isBluetooth) (if (name.isEmpty() || name == "null") "Bluetooth" else name) else if (isHeadset) "Sluchátka" else "Telefon"
    }

    fun loadArtistImage(name: String) {
        if (_artistImages.value.containsKey(name)) return
        viewModelScope.launch {
            if (!_artistImages.value.containsKey(name)) {
                val url = repository.fetchArtistImageUrl(name) ?: ""
                val currentMap = _artistImages.value.toMutableMap()
                currentMap[name] = url
                _artistImages.value = currentMap
            }
        }
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.setPlaybackSpeed(_playbackSpeed.value)
                applySkipSilenceToService(_skipSilenceEnabled.value)
                mediaController?.sessionExtras?.let { checkAndStartVisualizer(it) }
                setupPlayerListeners()
                syncPlayerState()
            } catch (e: Exception) { e.printStackTrace() }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListeners() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    mediaController?.sessionExtras?.let { checkAndStartVisualizer(it) }
                    startProgressUpdate()
                } else {
                    visualizer?.enabled = false
                    _targetBounceScale.value = 1f
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                _progress.value = newPosition.positionMs
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    viewModelScope.launch { delay(150); startVisualizer() }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (_sleepTimerFinishTrack.value && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    mediaController?.pause()
                    cancelSleepTimer()
                }
                updateQueue()
                viewModelScope.launch { delay(400); mediaController?.sessionExtras?.let { checkAndStartVisualizer(it) } }
                mediaItem?.let { item ->
                    val song = songsByUri[item.mediaId]
                    song?.let {
                        _currentSong.value = it
                        extractColors(it.albumArt)
                        _duration.value = (mediaController?.duration ?: 0L).coerceAtLeast(0L)
                        fetchLyrics(it.artist, it.title)
                    }
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = (mediaController?.duration ?: 0L).coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED && _sleepTimerFinishTrack.value) {
                    cancelSleepTimer()
                }
            }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { _shuffleModeEnabled.value = enabled }
            override fun onRepeatModeChanged(mode: Int) { _repeatMode.value = mode }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) { updateQueue() }
        })
    }

    private fun checkAndStartVisualizer(extras: Bundle) {
        val id = extras.getInt("AUDIO_SESSION_ID", 0)
        if (id != 0) audioSessionId = id
        if (_isPlaying.value) startVisualizer()
    }

    private fun syncPlayerState() {
        mediaController?.let { controller ->
            _isPlaying.value = controller.isPlaying
            _duration.value = controller.duration.coerceAtLeast(0L)
            _shuffleModeEnabled.value = controller.shuffleModeEnabled
            _repeatMode.value = controller.repeatMode
            updateQueue()
            controller.currentMediaItem?.let { item ->
                val song = songsByUri[item.mediaId]
                song?.let { _currentSong.value = it; extractColors(it.albumArt); fetchLyrics(it.artist, it.title) }
            }
            if (_isPlaying.value) {
                controller.sessionExtras?.let { checkAndStartVisualizer(it) }
                startProgressUpdate()
            }
        }
    }

    private fun startVisualizer() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        try {
            if (visualizer != null && visualizer?.enabled == false) {
                visualizer?.enabled = true
                return
            }
            visualizer?.release()
            val idToUse = if (audioSessionId > 0) audioSessionId else 0
            visualizer = Visualizer(idToUse).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, w: ByteArray?, s: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, s: Int) { fft?.let { processFftData(it) } }
                }, Visualizer.getMaxCaptureRate(), false, true)
                enabled = true
            }
        } catch (e: Exception) {
            if (audioSessionId != 0) { audioSessionId = 0; startVisualizer() }
        }
    }

    private fun stopVisualizer() {
        visualizer?.release()
        visualizer = null
        _visualizerData.value = emptyList()
        _targetBounceScale.value = 1f
    }

    private fun processFftData(fft: ByteArray) {
        val halfN = fft.size / 2
        if (fftMagnitudesBuffer.size != halfN) {
            fftMagnitudesBuffer = FloatArray(halfN)
        }
        val magnitudes = fftMagnitudesBuffer
        for (i in 0 until halfN) {
            magnitudes[i] = hypot(fft[2 * i].toFloat(), fft[2 * i + 1].toFloat())
        }
        val averaged = ArrayList<Float>(visualizerBarCount)
        val bucketSize = (magnitudes.size / visualizerBarCount).coerceAtLeast(1)
        val sensitivity = _visualizerSensitivity.value
        for (i in 0 until visualizerBarCount) {
            var sum = 0f
            for (j in 0 until bucketSize) {
                val idx = i * bucketSize + j
                if (idx < magnitudes.size) sum += magnitudes[idx]
            }
            val raw = ((sum / bucketSize * (1f + (i.toFloat() / visualizerBarCount) * 4.0f)) / 35f).pow(1.1f).coerceIn(0f, 1f) * sensitivity
            previousMagnitudes[i] = (previousMagnitudes[i] * smoothingFactor) + (raw * (1 - smoothingFactor))
            averaged.add(previousMagnitudes[i])
        }
        _visualizerData.value = averaged

        if (!_isPowerSaveMode.value && (_isCoverBounceEnabled.value || _isAuroraEnabled.value || _isParticlesEnabled.value || _isPulseEnabled.value)) {
            var bassSum = 0f
            for (i in 0 until 4) {
                if (i < magnitudes.size) bassSum += magnitudes[i]
            }
            _targetBounceScale.value = 1f + ((bassSum / 4 / 70f).coerceIn(0f, 1f).pow(1.5f) * 0.5f * _bounceIntensity.value)
        } else if (_targetBounceScale.value != 1f) {
            _targetBounceScale.value = 1f
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            val loadedSongs = repository.loadSongs()
            rawSongs = loadedSongs
            songsByUri = loadedSongs.associateBy { it.uri.toString() }

            val artMap = HashMap<Long, Uri>()
            for (s in loadedSongs) {
                if (s.albumArt != null && !artMap.containsKey(s.albumId)) {
                    artMap[s.albumId] = s.albumArt
                }
            }
            _albumArtByAlbumId.value = artMap

            _allSongs.value = applySort(loadedSongs, _sortOption.value)
            _allAlbums.value = repository.loadAlbums()
            _allArtists.value = repository.loadArtists(loadedSongs)
            if (_currentSong.value == null && mediaController?.currentMediaItem != null) {
                syncPlayerState()
            } else {
                updateQueue()
            }
        }
    }

    private fun applySort(songs: List<Song>, option: SortOption): List<Song> {
        return when (option) {
            SortOption.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST_ASC -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            SortOption.DURATION_DESC -> songs.sortedByDescending { it.duration }
        }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        prefs.edit().putString("sort_option", option.name).apply()
        _allSongs.value = applySort(rawSongs, option)
    }

    fun playSong(song: Song, sourceList: List<Song> = _allSongs.value) {
        val controller = mediaController ?: return
        val startIndex = sourceList.indexOfFirst { it.uri == song.uri }
        if (startIndex != -1) {
            val items = sourceList.map { s ->
                val meta = MediaMetadata.Builder()
                    .setTitle(s.title)
                    .setArtist(s.artist)
                    .apply { s.albumArt?.let { setArtworkUri(it) } }
                    .build()
                MediaItem.Builder()
                    .setUri(s.uri)
                    .setMediaId(s.uri.toString())
                    .setMediaMetadata(meta)
                    .build()
            }
            controller.setMediaItems(items, startIndex, androidx.media3.common.C.TIME_UNSET)
            controller.prepare()
            controller.play()
        }
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        val meta = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .apply { song.albumArt?.let { setArtworkUri(it) } }
            .setExtras(Bundle().apply { putBoolean("is_manual", true) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.uri.toString())
            .setMediaMetadata(meta)
            .build()

        val currentIndex = controller.currentMediaItemIndex
        var insertIndex = (currentIndex + 1).coerceAtMost(controller.mediaItemCount)

        while (insertIndex < controller.mediaItemCount) {
            val item = controller.getMediaItemAt(insertIndex)
            if (item.mediaMetadata.extras?.getBoolean("is_manual") == true) {
                insertIndex++
            } else {
                break
            }
        }

        controller.addMediaItem(insertIndex, mediaItem)
    }

    private fun updateQueue() {
        val controller = mediaController ?: return
        _currentQueueIndex.value = controller.currentMediaItemIndex
        val count = controller.mediaItemCount
        val items = ArrayList<QueueItem>(count)
        for (i in 0 until count) {
            val item = controller.getMediaItemAt(i)
            val song = songsByUri[item.mediaId] ?: Song(0, Uri.parse(item.mediaId), item.mediaMetadata.title.toString(), item.mediaMetadata.artist.toString(), 0, "", null, "", 0, 0)
            items.add(QueueItem(i, song, item.mediaMetadata.extras?.getBoolean("is_manual") == true))
        }
        _queueItems.value = items
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && mediaController?.isPlaying == true) {
                val pos = mediaController?.currentPosition ?: 0L
                if (pos != _progress.value) {
                    _progress.value = pos
                }
                val lines = _lyricsLines.value
                if (lines.isNotEmpty() && lines.first().isSynced) {
                    val idx = lines.indexOfLast { it.time <= pos }
                    if (idx != _currentLyricIndex.value) _currentLyricIndex.value = idx
                }
                delay(75)
            }
        }
    }

    private fun extractColors(artUri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (artUri == null) { resetColors(); return@launch }
            try {
                val loader = coil.Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artUri)
                    .allowHardware(false)
                    .size(100)
                    .build()
                val result = loader.execute(request)

                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    val palette = Palette.from(bitmap).generate()

                    val topSwatch = palette.darkVibrantSwatch ?: palette.vibrantSwatch ?: palette.dominantSwatch
                    val baseRgb = topSwatch?.rgb ?: android.graphics.Color.parseColor("#2B2B2B")

                    val hsl = FloatArray(3)
                    ColorUtils.colorToHSL(baseRgb, hsl)
                    hsl[1] = (hsl[1] * 1.5f).coerceAtMost(1f)
                    hsl[2] = hsl[2].coerceIn(0.15f, 0.4f)

                    val vibrantRgb = ColorUtils.HSLToColor(hsl)
                    val topColor = Color(vibrantRgb)

                    val isLight = topColor.luminance() > 0.5f
                    _playerContentColor.value = if (isLight) Color.Black else Color.White

                    val bottomColor = Color(
                        red = topColor.red * 0.3f,
                        green = topColor.green * 0.3f,
                        blue = topColor.blue * 0.3f
                    )

                    _backgroundColors.value = Pair(topColor, bottomColor)
                } else resetColors()
            } catch (e: Exception) { resetColors() }
        }
    }

    private fun resetColors() { _backgroundColors.value = Pair(Color(0xFF2B2B2B), Color.Black); _playerContentColor.value = Color.White }

    fun fetchLyrics(artist: String, title: String) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            _currentLyricIndex.value = -1
            _lyricsLines.value = repository.fetchLyrics(artist, title)
            _isLoadingLyrics.value = false
        }
    }

    fun togglePlayPause() { mediaController?.let { if (it.isPlaying) it.pause() else it.play() } }

    fun skipNext() {
        mediaController?.let {
            it.seekToNext()
            it.play()
        }
    }

    fun skipPrevious() {
        mediaController?.let {
            it.seekToPrevious()
            it.play()
        }
    }

    fun seekTo(p: Float) { mediaController?.let { it.seekTo(p.toLong()); _progress.value = p.toLong(); viewModelScope.launch { delay(150); startVisualizer() } } }
    fun toggleShuffle() { mediaController?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; _shuffleModeEnabled.value = it.shuffleModeEnabled } }
    fun toggleRepeat() { mediaController?.let { val n = when(it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE; Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL; else -> Player.REPEAT_MODE_OFF }; it.repeatMode = n; _repeatMode.value = n } }

    private fun loadSettings() {
        _isScreenAlwaysOn.value = prefs.getBoolean("always_on_display", false)
        _isAuroraEnabled.value = prefs.getBoolean("aurora_enabled", true)
        _isCoverBounceEnabled.value = prefs.getBoolean("cover_bounce_enabled", true)
        _isParticlesEnabled.value = prefs.getBoolean("particles_enabled", true)
        _isPulseEnabled.value = prefs.getBoolean("pulse_enabled", false)
        _visualizerSensitivity.value = prefs.getFloat("vis_sensitivity", 1.0f)
        _bounceIntensity.value = prefs.getFloat("bounce_intensity", 1.0f)
        _auroraSpeed.value = prefs.getFloat("aurora_speed", 1.0f)
        _isVisualizerMirrored.value = prefs.getBoolean("visualizer_mirrored", true)
        val typeName = prefs.getString("visualizer_type", VisualizerType.BARS.name) ?: VisualizerType.BARS.name
        _visualizerType.value = try { VisualizerType.valueOf(typeName) } catch (e: Exception) { VisualizerType.BARS }

        val sortName = prefs.getString("sort_option", SortOption.TITLE_ASC.name) ?: SortOption.TITLE_ASC.name
        _sortOption.value = try { SortOption.valueOf(sortName) } catch (e: Exception) { SortOption.TITLE_ASC }
        _playbackSpeed.value = prefs.getFloat("playback_speed", 1.0f).coerceIn(0.5f, 2.0f)
        _skipSilenceEnabled.value = prefs.getBoolean("skip_silence", false)
        _isPowerSaveMode.value = prefs.getBoolean("power_save_mode", false)
    }

    fun toggleAlwaysOn(v: Boolean) { _isScreenAlwaysOn.value = v; prefs.edit().putBoolean("always_on_display", v).apply() }
    fun toggleAurora(v: Boolean) { _isAuroraEnabled.value = v; prefs.edit().putBoolean("aurora_enabled", v).apply() }
    fun toggleCoverBounce(v: Boolean) { _isCoverBounceEnabled.value = v; prefs.edit().putBoolean("cover_bounce_enabled", v).apply() }
    fun setVisualizerType(t: VisualizerType) { _visualizerType.value = t; prefs.edit().putString("visualizer_type", t.name).apply() }
    fun toggleVisualizerMirroring(v: Boolean) { _isVisualizerMirrored.value = v; prefs.edit().putBoolean("visualizer_mirrored", v).apply() }
    fun setVisualizerSensitivity(v: Float) { _visualizerSensitivity.value = v; prefs.edit().putFloat("vis_sensitivity", v).apply() }
    fun setBounceIntensity(v: Float) { _bounceIntensity.value = v; prefs.edit().putFloat("bounce_intensity", v).apply() }
    fun setAuroraSpeed(v: Float) { _auroraSpeed.value = v; prefs.edit().putFloat("aurora_speed", v).apply() }
    fun toggleParticles(v: Boolean) { _isParticlesEnabled.value = v; prefs.edit().putBoolean("particles_enabled", v).apply() }
    fun togglePulse(v: Boolean) { _isPulseEnabled.value = v; prefs.edit().putBoolean("pulse_enabled", v).apply() }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = clamped
        prefs.edit().putFloat("playback_speed", clamped).apply()
        mediaController?.setPlaybackSpeed(clamped)
    }

    fun toggleSkipSilence(enabled: Boolean) {
        _skipSilenceEnabled.value = enabled
        prefs.edit().putBoolean("skip_silence", enabled).apply()
        applySkipSilenceToService(enabled)
    }

    private fun applySkipSilenceToService(enabled: Boolean) {
        try {
            val intent = Intent(context, MusicService::class.java).apply {
                action = "SET_SKIP_SILENCE"
                putExtra("enabled", enabled)
            }
            context.startService(intent)
        } catch (e: Exception) { }
    }

    fun togglePowerSaveMode(enabled: Boolean) {
        _isPowerSaveMode.value = enabled
        prefs.edit().putBoolean("power_save_mode", enabled).apply()
        if (enabled) {
            _targetBounceScale.value = 1f
        }
    }

    // --- ČASOVAČ VYPNUTÍ (SLEEP TIMER) ---
    fun setSleepTimer(minutes: Int, finishCurrentTrack: Boolean = false) {
        sleepTimerJob?.cancel()
        if (finishCurrentTrack) {
            _sleepTimerFinishTrack.value = true
            _sleepTimerRemainingSec.value = 0
            return
        }
        _sleepTimerFinishTrack.value = false
        if (minutes <= 0) {
            _sleepTimerRemainingSec.value = 0
            return
        }
        _sleepTimerRemainingSec.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (isActive && _sleepTimerRemainingSec.value > 0) {
                delay(1000L)
                val next = _sleepTimerRemainingSec.value - 1
                _sleepTimerRemainingSec.value = next
                if (next <= 0) {
                    mediaController?.pause()
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSec.value = 0
        _sleepTimerFinishTrack.value = false
    }

    fun clearAllCaches() {
        repository.clearCaches()
        _artistImages.value = emptyMap()
        try {
            coil.Coil.imageLoader(context).memoryCache?.clear()
        } catch (e: Exception) { }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val favPlaylist = repository.getOrCreateFavoritesPlaylist()
            val uriStr = song.uri.toString()
            if (_favoriteIds.value.contains(uriStr)) {
                repository.removeSongFromPlaylist(favPlaylist.playlistId, uriStr)
            } else {
                repository.addSongToPlaylist(favPlaylist.playlistId, uriStr)
            }
        }
    }

    fun isFavorite(uri: Uri) = _favoriteIds.value.contains(uri.toString())
    fun switchTab(t: LibraryTab) { _libraryTab.value = t }

    fun selectAlbum(a: Album) {
        playlistFilterJob?.cancel()
        _selectedPlaylist.value = null
        _filteredSongs.value = rawSongs
            .filter { it.albumId == a.id }
            .sortedWith(compareBy({ if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }, { it.title }))
        _filterTitle.value = a.title
    }

    fun selectArtist(a: Artist) {
        playlistFilterJob?.cancel()
        _selectedPlaylist.value = null
        _filteredSongs.value = rawSongs.filter { song ->
            song.artist.split(SPLIT_ARTIST_REGEX).any { it.trim().equals(a.name, ignoreCase = true) }
        }
        _filterTitle.value = a.name
    }

    fun selectPlaylist(p: Playlist) {
        playlistFilterJob?.cancel()
        _selectedPlaylist.value = p
        _filterTitle.value = p.name
        playlistFilterJob = viewModelScope.launch {
            repository.getSongUrisForPlaylist(p.playlistId).collect { uris ->
                _filteredSongs.value = uris.mapNotNull { songsByUri[it] }
            }
        }
    }

    fun clearFilter() {
        playlistFilterJob?.cancel()
        playlistFilterJob = null
        _selectedPlaylist.value = null
        _filteredSongs.value = null
        _filterTitle.value = null
    }

    fun createPlaylist(n: String) { viewModelScope.launch { repository.createPlaylist(n) } }
    fun deletePlaylist(p: Playlist) { viewModelScope.launch { repository.deletePlaylist(p) } }
    fun addSongToPlaylist(p: Playlist, s: Song) { viewModelScope.launch { repository.addSongToPlaylist(p.playlistId, s.uri.toString()) } }
    fun removeSongFromPlaylist(p: Playlist, s: Song) { viewModelScope.launch { repository.removeSongFromPlaylist(p.playlistId, s.uri.toString()) } }

    fun closePlayer() { mediaController?.stop(); _currentSong.value = null; _isPlaying.value = false; stopVisualizer() }
    fun playQueueItem(idx: Int) { mediaController?.seekToDefaultPosition(idx); mediaController?.play() }
    fun removeFromQueue(idx: Int) { mediaController?.removeMediaItem(idx) }
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
            controller.moveMediaItem(fromIndex, toIndex)
        }
    }
    fun clearManualQueue() {
        val controller = mediaController ?: return
        val currentIdx = controller.currentMediaItemIndex
        for (i in controller.mediaItemCount - 1 downTo (currentIdx + 1)) {
            val item = controller.getMediaItemAt(i)
            if (item.mediaMetadata.extras?.getBoolean("is_manual") == true) {
                controller.removeMediaItem(i)
            }
        }
    }
    fun clearQueue() { mediaController?.clearMediaItems() }

    fun resetSettingsToDefault() {
        toggleAlwaysOn(false)
        togglePowerSaveMode(false)
        toggleAurora(true)
        toggleParticles(true)
        togglePulse(false)
        toggleCoverBounce(true)
        setVisualizerType(VisualizerType.BARS)
        toggleVisualizerMirroring(true)
        setVisualizerSensitivity(1.0f)
        setBounceIntensity(1.0f)
        setAuroraSpeed(1.0f)
        setPlaybackSpeed(1.0f)
        toggleSkipSilence(false)
        setSortOption(SortOption.TITLE_ASC)
        cancelSleepTimer()
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        playlistFilterJob?.cancel()
        sleepTimerJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        }
        controllerFuture?.let { f -> MediaController.releaseFuture(f) }
        visualizer?.release()
    }
}