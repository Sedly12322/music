package com.example.musicapp.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.musicapp.model.Album
import com.example.musicapp.model.Artist
import com.example.musicapp.model.Song
import java.io.File

// --- POMOCNÁ FUNKCE: Načtení coverů z MediaStore (Android 10+) ---
// Vrací mapu: ID složky (Bucket ID) -> URI prvního nalezeného coveru v té složce
private fun getCoversFromMediaStore(context: Context): Map<Long, Uri> {
    val covers = mutableMapOf<Long, Uri>()
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_ID
    )

    // Hledáme obrázky, které v názvu obsahují klíčová slova
    val selection = "(" +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE 'cover.%' OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE 'Cover.%' OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE 'folder.%' OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE 'Folder.%' OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE 'album.%' OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE 'front.%'" +
            ")"

    try {
        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val bucketId = cursor.getLong(bucketCol)

                // Pokud pro tuto složku ještě nemáme cover, uložíme ho
                if (!covers.containsKey(bucketId)) {
                    val artUri = ContentUris.withAppendedId(uri, id)
                    covers[bucketId] = artUri
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return covers
}

fun getAllSongs(context: Context): List<Song> {
    val songs = mutableListOf<Song>()
    val isAndroidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    // 1. Přednačteme covery (jen pokud máme práva na obrázky!)
    val coversMap = if (isAndroidQ) getCoversFromMediaStore(context) else emptyMap()

    // Cache pro starší Android
    val legacyFolderCache = mutableMapOf<String, Uri?>()

    val collection = if (isAndroidQ) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projectionList = mutableListOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DATA
    )

    if (isAndroidQ) {
        projectionList.add(MediaStore.Audio.Media.BUCKET_ID)
    }

    val projection = projectionList.toTypedArray()
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"

    try {
        context.contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val bucketIdCol = if (isAndroidQ) cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_ID) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)
                val artist = cursor.getString(artistCol) ?: "Neznámý umělec"
                val albumId = cursor.getLong(albumIdCol)
                val album = cursor.getString(albumCol) ?: "Neznámé album"
                val duration = cursor.getLong(durationCol)
                val track = cursor.getInt(trackCol) % 1000
                val extension = cursor.getString(nameCol).substringAfterLast('.', "").uppercase()

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                // Výchozí systémový cover
                var artUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)

                // VYLEPŠENÉ HLEDÁNÍ
                if (isAndroidQ) {
                    if (bucketIdCol != -1) {
                        val bucketId = cursor.getLong(bucketIdCol)
                        // Pokud máme v mapě obrázek ze stejné složky (Bucket), použijeme ho
                        if (coversMap.containsKey(bucketId)) {
                            artUri = coversMap[bucketId]!!
                        }
                    }
                } else {
                    // Stará metoda pro Android 9 a nižší
                    val path = cursor.getString(dataCol)
                    if (path != null) {
                        try {
                            val parentPath = File(path).parent
                            if (parentPath != null) {
                                if (legacyFolderCache.containsKey(parentPath)) {
                                    legacyFolderCache[parentPath]?.let { artUri = it }
                                } else {
                                    val parentFile = File(parentPath)
                                    val files = parentFile.listFiles()
                                    var foundUri: Uri? = null
                                    if (files != null) {
                                        val validNames = setOf("cover", "folder", "album", "front")
                                        val validExts = setOf("jpg", "jpeg", "png", "webp")
                                        val cover = files.firstOrNull { f ->
                                            val fname = f.nameWithoutExtension.lowercase()
                                            val fext = f.extension.lowercase()
                                            validNames.contains(fname) && validExts.contains(fext)
                                        }
                                        if (cover != null) foundUri = Uri.fromFile(cover)
                                    }
                                    legacyFolderCache[parentPath] = foundUri
                                    foundUri?.let { artUri = it }
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }

                songs.add(Song(id, contentUri, title, if (artist == "<unknown>") "Neznámý umělec" else artist, albumId, album, artUri, extension, track, duration))
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return songs
}

// Zbytek funkcí (getAllAlbums, generateSmartArtists...) zůstává stejný jako předtím
fun getAllAlbums(context: Context): List<Album> {
    val albums = mutableListOf<Album>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.NUMBER_OF_SONGS, MediaStore.Audio.Albums.FIRST_YEAR)
    try {
        context.contentResolver.query(collection, projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val songsCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id)
                albums.add(Album(id, cursor.getString(albumCol), cursor.getString(artistCol).let { if(it=="<unknown>") "Neznámý umělec" else it }, artUri, cursor.getInt(songsCol), cursor.getInt(yearCol)))
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return albums
}

val SPLIT_ARTIST_REGEX = Regex("(?i)\\s+(feat\\.|ft\\.|&|x|vs\\.)\\s+|[,;]\\s+")

fun generateSmartArtists(songs: List<Song>): List<Artist> {
    val artistMap = mutableMapOf<String, MutableSet<Long>>()
    val trackCounts = mutableMapOf<String, Int>()
    songs.forEach { song ->
        song.artist.split(SPLIT_ARTIST_REGEX).forEach { rawName ->
            val cleanName = rawName.trim()
            if (cleanName.isNotBlank()) {
                trackCounts[cleanName] = trackCounts.getOrDefault(cleanName, 0) + 1
                artistMap.getOrPut(cleanName) { mutableSetOf() }.add(song.albumId)
            }
        }
    }
    return artistMap.map { (name, albumIds) -> Artist(name.hashCode().toLong(), name, trackCounts[name] ?: 0, albumIds.size) }.sortedBy { it.name }
}

fun getAllArtists(context: Context): List<Artist> = emptyList()

fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return buildString(5) {
        if (min < 10) append('0')
        append(min)
        append(':')
        if (sec < 10) append('0')
        append(sec)
    }
}