package com.example.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

data class MediaModel(
    val id: Long,
    val uri: Uri,
    val filePath: String,
    val title: String,
    val duration: Long,
    val type: String, // "AUDIO" or "VIDEO"
    val artist: String = "",
    val album: String = "",
    val coverUri: String = ""
)

class MediaRepository(private val context: Context, private val dao: MediaDao) {
    val favorites = dao.getAllFavorites()
    val playlists = dao.getAllPlaylists()
    val hiddenMedia = dao.getAllHiddenMedia()
    val mediaMetadata = dao.getAllMetadata()

    fun getShuffleMode(type: String): Flow<Boolean> {
        return dao.getSettingFlow("shuffle_$type").map { it?.value == "true" }
    }
    
    suspend fun setShuffleMode(type: String, isShuffle: Boolean) {
        dao.saveSetting(SettingEntity("shuffle_$type", isShuffle.toString()))
    }
    
    suspend fun hideMedia(filePath: String) {
        dao.addHiddenMedia(HiddenMediaEntity(filePath))
    }

    suspend fun saveMetadata(filePath: String, title: String, artist: String, album: String, coverUri: String) {
        dao.saveMetadata(MediaMetadataEntity(filePath, title, artist, album, coverUri))
    }

    suspend fun addFavorite(media: MediaModel) {
        dao.addFavorite(FavoriteEntity(media.filePath, media.type))
    }

    suspend fun removeFavorite(filePath: String) {
        dao.removeFavorite(filePath)
    }

    suspend fun createPlaylist(name: String) {
        dao.addPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(id: Long) {
        dao.deletePlaylist(id)
    }
    
    fun getMediaForPlaylist(playlistId: Long) = dao.getMediaForPlaylist(playlistId)

    suspend fun addMediaToPlaylist(playlistId: Long, filePath: String) {
        dao.addMediaToPlaylist(PlaylistMediaCrossRef(playlistId, filePath))
    }

    suspend fun removeMediaFromPlaylist(playlistId: Long, filePath: String) {
        dao.removeMediaFromPlaylist(playlistId, filePath)
    }

    suspend fun loadAudioFiles(): List<MediaModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaModel>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val filePath = cursor.getString(dataColumn)
                val title = cursor.getString(titleColumn) ?: File(filePath).nameWithoutExtension
                val duration = cursor.getLong(durationColumn)
                if (duration > 0 && File(filePath).exists()) {
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(MediaModel(id, uri, filePath, title, duration, "AUDIO"))
                }
            }
        }
        
        // Also check our local extracted mp3 files if they are not in mediastore yet
        val extractedDir = File(context.getExternalFilesDir(null), "ExtractedAudio")
        if (extractedDir.exists()) {
            extractedDir.listFiles()?.forEach { file ->
                if (file.extension == "mp3" && !list.any { it.filePath == file.absolutePath }) {
                    list.add(
                        MediaModel(
                            id = file.hashCode().toLong(),
                            uri = Uri.fromFile(file),
                            filePath = file.absolutePath,
                            title = file.nameWithoutExtension,
                            duration = 0L, // We don't have duration immediately without MediaMetadataRetriever
                            type = "AUDIO"
                        )
                    )
                }
            }
        }
        
        list
    }

    suspend fun loadVideoFiles(): List<MediaModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaModel>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val filePath = cursor.getString(dataColumn)
                val title = cursor.getString(titleColumn) ?: File(filePath).nameWithoutExtension
                val duration = cursor.getLong(durationColumn)
                
                if (File(filePath).exists()) {
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(MediaModel(id, uri, filePath, title, duration, "VIDEO"))
                }
            }
        }
        list
    }
}
