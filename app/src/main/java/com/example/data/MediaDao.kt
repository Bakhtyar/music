package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE filePath = :filePath")
    suspend fun removeFavorite(filePath: String)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT * FROM playlist_media ORDER BY addedAt ASC")
    fun getAllPlaylistMedia(): Flow<List<PlaylistMediaCrossRef>>

    @Query("SELECT * FROM playlist_media WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getMediaForPlaylist(playlistId: Long): Flow<List<PlaylistMediaCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMediaToPlaylist(crossRef: PlaylistMediaCrossRef)

    @Query("DELETE FROM playlist_media WHERE playlistId = :playlistId AND filePath = :filePath")
    suspend fun removeMediaFromPlaylist(playlistId: Long, filePath: String)
    
    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): SettingEntity?
    
    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<SettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: SettingEntity)

    // Hidden Media
    @Query("SELECT * FROM hidden_media")
    fun getAllHiddenMedia(): Flow<List<HiddenMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHiddenMedia(hiddenMedia: HiddenMediaEntity)

    @Query("DELETE FROM hidden_media WHERE filePath = :filePath")
    suspend fun removeHiddenMedia(filePath: String)

    // Custom Metadata
    @Query("SELECT * FROM media_metadata")
    fun getAllMetadata(): Flow<List<MediaMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: MediaMetadataEntity)
}
