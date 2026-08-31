package com.example.musicapp.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

// --- ROZŠÍŘENÉ TYPY VIZUALIZÉRU ---
enum class VisualizerType(val title: String) {
    BARS("Sloupce"),
    CIRCLE("Kruh"),
    WAVE("Vlna"),
    DOTS("Body"),      // NOVÉ
    MIRROR("Zrcadlo"), // NOVÉ
    STAR("Hvězda")     // NOVÉ
}

enum class LibraryTab { SONGS, ARTISTS, ALBUMS, PLAYLISTS }

sealed interface LibraryItem
data class FolderItem(val name: String, val uri: Uri) : LibraryItem
data class SongItem(val song: Song) : LibraryItem

data class Breadcrumb(val name: String, val uri: Uri)

data class LyricLine(val time: Long, val text: String, val isSynced: Boolean = true)

enum class SortOption { TITLE_ASC, TITLE_DESC, TRACK_NUMBER }

// --- ENTITY PRO DATABÁZI (ROOM) ---

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String,
    val isSystem: Boolean = false
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songUri"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songUri: String,
    val addedAt: Long = System.currentTimeMillis()
)

// --- PŮVODNÍ MODELY ---

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val albumId: Long,
    val albumName: String,
    val albumArt: Uri? = null,
    val fileType: String,
    val trackNumber: Int,
    val duration: Long
)

data class Artist(
    val id: Long,
    val name: String,
    val numberOfTracks: Int,
    val numberOfAlbums: Int
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArt: Uri?,
    val numberOfSongs: Int,
    val year: Int
)