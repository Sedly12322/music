package com.example.musicapp.data

import android.content.Context
import androidx.room.*
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    // Získá všechny playlisty
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    // Vytvoří playlist
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    // Přidá skladbu do playlistu
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSong(crossRef: PlaylistSongCrossRef)

    // Odebere skladbu z playlistu
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songUri = :songUri")
    suspend fun removeSongFromPlaylist(playlistId: Long, songUri: String)

    // Získá URI všech skladeb v playlistu
    @Query("SELECT songUri FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getSongUrisForPlaylist(playlistId: Long): Flow<List<String>>

    // Speciální metoda pro Oblíbené (systémový playlist)
    @Query("SELECT * FROM playlists WHERE isSystem = 1 LIMIT 1")
    suspend fun getFavoritesPlaylist(): Playlist?
}

@Database(entities = [Playlist::class, PlaylistSongCrossRef::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_app_database"
                )
                    // Předvyplnění databáze (vytvoření playlistu Oblíbené)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // SQL příkaz pro vytvoření defaultního playlistu
                            db.execSQL("INSERT OR IGNORE INTO playlists (name, isSystem) VALUES ('Oblíbené', 1)")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}