package com.example.musicapp.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicapp.MainActivity
import com.example.musicapp.ui.widget.MusicWidget
import com.example.musicapp.ui.widget.MusicWidgetPrefs
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

// --- VLASTNÍ NAČÍTAČ PRO SYSTÉMOVÉ NOTIFIKACE (STEJNÝ PRINCIP JAKO V KNIHOVNĚ) ---
@OptIn(UnstableApi::class)
class CustomBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = true

    // Zpracování ID3 tagů přímo ze souboru
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                val scaled = scaleBitmap(bitmap)
                if (scaled != null) {
                    future.set(scaled)
                } else {
                    future.setException(IllegalArgumentException("Nelze dekódovat bitmapu"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    // Zpracování URI (použijeme sdílený Coil ImageLoader přesně jako UI aplikace)
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val loader = coil.Coil.imageLoader(context)
                val request = coil.request.ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false)
                    .size(350) // Rovnou ušetříme paměť
                    .build()

                val result = loader.execute(request)
                if (result is coil.request.SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    val scaled = scaleBitmap(bitmap)
                    if (scaled != null) {
                        future.set(scaled)
                    } else {
                        future.setException(Exception("Scaled bitmap is null"))
                    }
                } else {
                    future.setException(Exception("Coil failed to load Image"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    // Bezpečná komprese pro Samsung roletku (prevence proti příliš velkým datům)
    private fun scaleBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        val maxDim = 350f
        val scale = minOf(maxDim / bitmap.width, maxDim / bitmap.height)
        if (scale >= 1f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }
}


class MusicService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetUpdateJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA).build(), true)
            setHandleAudioBecomingNoisy(true)
        }
        player = exoPlayer

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = updateWidget()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateWidget()
            }
            override fun onMediaMetadataChanged(metadata: androidx.media3.common.MediaMetadata) = updateWidget()
            override fun onPlaybackStateChanged(state: Int) = updateWidget()
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                val extras = Bundle().apply { putInt("AUDIO_SESSION_ID", audioSessionId) }
                mediaSession?.setSessionExtras(extras)
            }
        })

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val sessionExtras = Bundle().apply {
            putInt("AUDIO_SESSION_ID", exoPlayer.audioSessionId)
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setSessionExtras(sessionExtras)
            .setBitmapLoader(CustomBitmapLoader(applicationContext, serviceScope))
            .build()
    }


    private fun updateWidget() {
        val item = player?.currentMediaItem ?: return
        val title = item.mediaMetadata.title?.toString() ?: "Neznámá skladba"
        val artist = item.mediaMetadata.artist?.toString() ?: "Neznámý umělec"
        val isPlaying = player?.isPlaying ?: false

        val artworkUri = item.mediaMetadata.artworkUri
        val artworkData = item.mediaMetadata.artworkData

        widgetUpdateJob?.cancel()
        widgetUpdateJob = serviceScope.launch {
            delay(200)
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(MusicWidget::class.java)
            if (glanceIds.isEmpty()) return@launch

            var bitmap: Bitmap? = null

            // Coil načítání pro Glance Widget (Plocha)
            if (artworkUri != null) {
                try {
                    val loader = coil.Coil.imageLoader(applicationContext)
                    val request = coil.request.ImageRequest.Builder(applicationContext)
                        .data(artworkUri)
                        .allowHardware(false)
                        .size(200)
                        .build()
                    val result = loader.execute(request)
                    if (result is coil.request.SuccessResult) {
                        bitmap = result.drawable.toBitmap()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // Fallback na vložená data tagu, pokud Coil selže nebo URI neexistuje
            if (bitmap == null && artworkData != null) {
                try {
                    bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
                } catch (e: Exception) { e.printStackTrace() }
            }

            var base64Art = ""
            if (bitmap != null) {
                try {
                    val scaled = Bitmap.createScaledBitmap(bitmap!!, 150, 150, true)
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
                    base64Art = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
                } catch (e: Exception) { e.printStackTrace() }
            }

            glanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetPrefs.title] = title
                    prefs[MusicWidgetPrefs.artist] = artist
                    prefs[MusicWidgetPrefs.isPlaying] = isPlaying
                    prefs[MusicWidgetPrefs.artBase64] = base64Art
                    prefs[longPreferencesKey("force_update")] = System.currentTimeMillis()
                }
                MusicWidget().update(applicationContext, id)
            }
        }
    }

    override fun onGetSession(info: MediaSession.ControllerInfo) = mediaSession

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY_PAUSE" -> if (player?.isPlaying == true) player?.pause() else player?.play()
            "NEXT" -> {
                player?.seekToNext()
                player?.play()
            }
            "PREV" -> {
                player?.seekToPrevious()
                player?.play()
            }
            "SET_SKIP_SILENCE" -> {
                val enabled = intent.getBooleanExtra("enabled", false)
                player?.skipSilenceEnabled = enabled
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        widgetUpdateJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}

object MusicServiceUtils {
    fun sendIntent(context: Context, action: String) {
        val intent = Intent(context, MusicService::class.java).apply { this.action = action }
        context.startService(intent)
    }
}