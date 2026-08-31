package com.example.musicapp.data

import android.content.Context
import android.net.Uri
import com.example.musicapp.model.*
import com.example.musicapp.utils.generateSmartArtists
import com.example.musicapp.utils.getAllAlbums
import com.example.musicapp.utils.getAllSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class MusicRepository(private val context: Context, private val db: MusicDatabase) {

    private val playlistDao = db.playlistDao()

    // --- CACHE (MEZIPAMĚŤ) PRO TEXTY ---
    // Ukládá se: "Interpret - Název" -> Seznam řádků
    // Díky tomu se text načte z internetu jen jednou a pak už se bere z paměti (bleskurychlé).
    private val lyricsCache = ConcurrentHashMap<String, List<LyricLine>>()

    // --- KNIHOVNA (Utils) ---

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        getAllSongs(context)
    }

    suspend fun loadAlbums(): List<Album> = withContext(Dispatchers.IO) {
        getAllAlbums(context)
    }

    suspend fun loadArtists(songs: List<Song>): List<Artist> = withContext(Dispatchers.Default) {
        generateSmartArtists(songs)
    }

    // --- DATABÁZE (Playlisty & Oblíbené) ---

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getSongUrisForPlaylist(playlistId: Long): Flow<List<String>> = playlistDao.getSongUrisForPlaylist(playlistId)

    suspend fun getFavoritesPlaylist(): Playlist? = playlistDao.getFavoritesPlaylist()

    suspend fun createPlaylist(name: String) = playlistDao.insertPlaylist(Playlist(name = name))

    suspend fun addSongToPlaylist(playlistId: Long, songUri: String) {
        playlistDao.insertPlaylistSong(PlaylistSongCrossRef(playlistId, songUri))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songUri: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songUri)
    }

    // --- SÍŤOVÉ VOLÁNÍ (API) ---

    // 1. Obrázky umělců (Deezer)
    suspend fun fetchArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = Uri.encode(artistName)
            val urlStr = "https://api.deezer.com/search/artist?q=$query&limit=1"
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode == 200) {
                val stream = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(stream)
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val artistObj = data.getJSONObject(0)
                    artistObj.optString("picture_xl", null) ?: artistObj.optString("picture_medium", null)
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 2. Texty písní (LrcLib + Fallback na Lyrics.ovh)
    suspend fun fetchLyrics(artist: String, title: String): List<LyricLine> = withContext(Dispatchers.IO) {
        val cacheKey = "$artist - $title"

        // 1. KONTROLA CACHE: Pokud už text máme, vrátíme ho hned
        if (lyricsCache.containsKey(cacheKey)) {
            return@withContext lyricsCache[cacheKey]!!
        }

        // 2. POKUS A: Vyčištěné názvy (největší šance na úspěch)
        val cleanArtist = cleanString(artist)
        val cleanTitle = cleanString(title)

        var lines = attemptFetchLyrics(cleanArtist, cleanTitle)

        // 3. POKUS B: Původní názvy (pokud čištění selhalo nebo smazalo důležitou část)
        // Zkusíme to jen tehdy, pokud se vyčištěné názvy liší od původních
        if (lines == null && (cleanArtist != artist || cleanTitle != title)) {
            lines = attemptFetchLyrics(artist, title)
        }

        val result = lines ?: listOf(LyricLine(0, "Text nebyl nalezen", false))

        // Uložíme výsledek do cache (i když se nenašel, abychom to nezkoušeli pořád dokola)
        if (lines != null) {
            lyricsCache[cacheKey] = result
        }

        result
    }

    // Pomocná metoda pro jeden pokus o stažení
    private fun attemptFetchLyrics(artist: String, title: String): List<LyricLine>? {
        // A. LrcLib (preferované - synchronizované)
        var result = fetchLyricsFromLrcLib(artist, title)

        // B. Lyrics.ovh (fallback - prostý text)
        if (result == null) {
            val plainText = fetchLyricsFromOvh(artist, title)
            if (plainText != null) result = parsePlainLyrics(plainText)
        }
        return result
    }

    private fun fetchLyricsFromLrcLib(artist: String, title: String): List<LyricLine>? {
        return try {
            val urlStr = "https://lrclib.net/api/get?artist_name=${Uri.encode(artist)}&track_name=${Uri.encode(title)}"
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000 // 5s timeout
            connection.readTimeout = 5000
            if (connection.responseCode == 200) {
                val stream = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(stream)

                val syncedLyrics = json.optString("syncedLyrics")
                if (syncedLyrics.isNotEmpty() && syncedLyrics != "null") {
                    parseLrcLyrics(syncedLyrics)
                } else {
                    val plain = json.optString("plainLyrics")
                    if (plain.isNotEmpty() && plain != "null") parsePlainLyrics(plain) else null
                }
            } else null
        } catch (e: Exception) { null }
    }

    private fun fetchLyricsFromOvh(artist: String, title: String): String? {
        return try {
            val urlStr = "https://api.lyrics.ovh/v1/${Uri.encode(artist)}/${Uri.encode(title)}"
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode == 200) {
                val stream = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(stream).optString("lyrics").takeIf { it.isNotEmpty() && it != "null" }
            } else null
        } catch (e: Exception) { null }
    }

    // --- PARSOVÁNÍ ---

    private fun parseLrcLyrics(lrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        // Regex pro časové značky: [mm:ss.xx] nebo [mm:ss:xxx]
        val regex = Regex("\\[(\\d+):(\\d+)[\\.:](\\d+)\\](.*)")

        lrc.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val (min, sec, centisec, text) = match.destructured
                // Ošetření délky milisekund (2 cifry = x10 ms, 3 cifry = x1 ms)
                val msMultiplier = if (centisec.length == 3) 1 else 10

                val timeMs = (min.toLong() * 60 * 1000) +
                        (sec.toLong() * 1000) +
                        (centisec.toLong() * msMultiplier)

                if (text.isNotBlank()) lines.add(LyricLine(timeMs, text.trim()))
            }
        }
        return lines.sortedBy { it.time }
    }

    private fun parsePlainLyrics(text: String) = text.lines()
        .filter { it.isNotBlank() }
        .map { LyricLine(0, it.trim(), false) }

    // --- CHYTRÉ ČIŠTĚNÍ NÁZVŮ ---
    private fun cleanString(input: String): String {
        var text = input

        // 1. Odstranit obsah v kulatých a hranatých závorkách (např. "(Remastered)", "[Official]")
        // Používáme non-greedy matching .*? aby to smazalo jen obsah závorky
        text = text.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")

        // 2. Odstranit "feat." nebo "ft." a vše za tím (často to API mate)
        text = text.replace(Regex("(?i)\\s+(feat\\.|ft\\.|&|x|vs\\.).*"), "")

        // 3. Odstranit " - " pokud je to oddělovač (např. "Artist - Title - Remaster")
        // Ale POZOR: Neodstraňovat pomlčky uvnitř slov (T-Pain, Jay-Z)
        if (text.contains(" - ")) {
            text = text.substringBefore(" - ")
        }

        return text.trim()
    }
}