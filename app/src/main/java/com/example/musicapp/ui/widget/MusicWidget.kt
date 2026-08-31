package com.example.musicapp.ui.widget

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.musicapp.R
import com.example.musicapp.service.MusicServiceUtils

object MusicWidgetPrefs {
    val title = stringPreferencesKey("title")
    val artist = stringPreferencesKey("artist")
    val isPlaying = booleanPreferencesKey("is_playing")
    val artBase64 = stringPreferencesKey("art_base64")
}

class MusicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { MusicWidgetContent() } }
    }

    @Composable
    private fun MusicWidgetContent() {
        val prefs = currentState<Preferences>()
        val title = prefs[MusicWidgetPrefs.title] ?: "Nic nehraje"
        val artist = prefs[MusicWidgetPrefs.artist] ?: ""
        val isPlaying = prefs[MusicWidgetPrefs.isPlaying] ?: false
        val base64Art = prefs[MusicWidgetPrefs.artBase64] ?: ""

        val bitmap = if (base64Art.isNotEmpty()) {
            try {
                val decoded = Base64.decode(base64Art, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } catch (e: Exception) { null }
        } else null

        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color(0xFF181818))
                .cornerRadius(16.dp).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
                Box(modifier = GlanceModifier.size(52.dp).cornerRadius(10.dp).background(Color(0xFF2C2C38))) {
                    if (bitmap != null) {
                        Image(provider = ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.Crop, modifier = GlanceModifier.fillMaxSize())
                    } else {
                        Image(provider = ImageProvider(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = GlanceModifier.fillMaxSize())
                    }
                }

                Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 12.dp)) {
                    Text(title, style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    Text(artist, style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 13.sp), maxLines = 1)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(provider = ImageProvider(android.R.drawable.ic_media_previous), contentDescription = null, modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<PrevAction>()))
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Image(provider = ImageProvider(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play), contentDescription = null, modifier = GlanceModifier.size(40.dp).clickable(actionRunCallback<PlayPauseAction>()))
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Image(provider = ImageProvider(android.R.drawable.ic_media_next), contentDescription = null, modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<NextAction>()))
                }
            }
        }
    }
}

class MusicWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = MusicWidget() }

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, params: ActionParameters) =
        MusicServiceUtils.sendIntent(context, "PLAY_PAUSE")
}
class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, params: ActionParameters) =
        MusicServiceUtils.sendIntent(context, "NEXT")
}
class PrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, params: ActionParameters) =
        MusicServiceUtils.sendIntent(context, "PREV")
}