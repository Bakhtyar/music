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

    private fun copyDemoFilesIfNeeded() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("demos_copied", false)) {
            try {
                val audioFile = File(context.getExternalFilesDir(null), "Preview_Audio.mp3")
                val videoFile = File(context.getExternalFilesDir(null), "Preview_Video.mp4")
                if (!audioFile.exists()) {
                    context.resources.openRawResource(context.resources.getIdentifier("demo_audio", "raw", context.packageName)).use { input ->
                        audioFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (!videoFile.exists()) {
                    context.resources.openRawResource(context.resources.getIdentifier("demo_video", "raw", context.packageName)).use { input ->
                        videoFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                prefs.edit().putBoolean("demos_copied", true).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val favorites = dao.getAllFavorites()
    val playlists = dao.getAllPlaylists()
    val allPlaylistMedia = dao.getAllPlaylistMedia()
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
        copyDemoFilesIfNeeded()

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
        
        // Also check our local extracted mp3/m4a audio files if they are not in mediastore yet
        val localDirs = listOfNotNull(
            context.getExternalFilesDir(null),
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
        )
        val validAudioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg")
        
        localDirs.forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension.lowercase() in validAudioExtensions && !list.any { it.filePath == file.absolutePath }) {
                        var duration = 0L
                        val retriever = android.media.MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(file.absolutePath)
                            val dStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            duration = dStr?.toLongOrNull() ?: 0L
                        } catch (e: Exception) {
                            // ignore fallback
                        } finally {
                            try { retriever.release() } catch (e: Exception) {}
                        }
                        
                        list.add(
                            MediaModel(
                                id = file.hashCode().toLong(),
                                uri = Uri.fromFile(file),
                                filePath = file.absolutePath,
                                title = file.nameWithoutExtension.replace("_audio", ""),
                                duration = duration,
                                type = "AUDIO"
                            )
                        )
                    }
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

        val localDir = context.getExternalFilesDir(null)
        if (localDir != null && localDir.exists()) {
            localDir.listFiles()?.forEach { file ->
                if (file.extension.lowercase() == "mp4" && !list.any { it.filePath == file.absolutePath }) {
                    list.add(
                        MediaModel(
                            id = file.hashCode().toLong(),
                            uri = Uri.fromFile(file),
                            filePath = file.absolutePath,
                            title = file.nameWithoutExtension,
                            duration = 0L,
                            type = "VIDEO"
                        )
                    )
                }
            }
        }
        list
    }
}
