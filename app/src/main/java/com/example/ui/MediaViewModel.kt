package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MediaModel
import com.example.data.MediaRepository
import com.example.util.AudioExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import androidx.media3.common.MediaItem
import com.example.player.PlayerManager

data class PlaylistStats(
    val audioCount: Int = 0,
    val videoCount: Int = 0
) {
    val totalCount: Int get() = audioCount + videoCount
}

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(
        application,
        AppDatabase.getDatabase(application).mediaDao()
    )

    private val themePreferences = com.example.ui.theme.ThemePreferences(application)
    val themeState: StateFlow<com.example.ui.theme.AppThemeState> = themePreferences.themeState
    val customArtState: StateFlow<com.example.ui.theme.CustomArtState> = themePreferences.customArtState

    fun saveCustomImage(type: String, uri: android.net.Uri): String? {
        return themePreferences.saveCustomImage(type, uri)
    }

    fun resetCustomImage(type: String) {
        themePreferences.resetCustomImage(type)
    }

    fun savePlaylistCover(playlistId: Long, uri: android.net.Uri): String? {
        return themePreferences.savePlaylistCover(playlistId, uri)
    }

    fun resetPlaylistCover(playlistId: Long) {
        themePreferences.resetPlaylistCover(playlistId)
    }

    fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun setThemePreset(preset: com.example.ui.theme.ThemePreset) {
        themePreferences.setThemePreset(preset)
    }

    fun setLayoutDensity(density: com.example.ui.theme.LayoutDensity) {
        themePreferences.setLayoutDensity(density)
    }

    fun setAppUIStyle(style: com.example.ui.theme.AppUIStyle) {
        themePreferences.setAppUIStyle(style)
    }

    private val _audioFiles = MutableStateFlow<List<MediaModel>>(emptyList())
    private val _videoFiles = MutableStateFlow<List<MediaModel>>(emptyList())
    private val _isExtracting = MutableStateFlow(false)

    val searchQuery = MutableStateFlow("")
    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
    val audioShuffleMode = repository.getShuffleMode("audio").stateIn(viewModelScope, SharingStarted.Lazily, false)
    val videoShuffleMode = repository.getShuffleMode("video").stateIn(viewModelScope, SharingStarted.Lazily, false)

    val audioFiles: StateFlow<List<MediaModel>> = combine(
        _audioFiles,
        repository.hiddenMedia,
        searchQuery,
        repository.mediaMetadata
    ) { audios, hidden, query, metadata ->
        audios.filter { a -> hidden.none { it.filePath == a.filePath } && a.title.contains(searchQuery.value, ignoreCase = true) }
            .map { a ->
                val meta = metadata.find { it.filePath == a.filePath }
                if (meta != null) {
                    a.copy(title = meta.title, artist = meta.artist, album = meta.album, coverUri = meta.coverUri)
                } else a
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val videoFiles: StateFlow<List<MediaModel>> = combine(
        _videoFiles,
        repository.hiddenMedia,
        searchQuery,
        repository.mediaMetadata
    ) { videos, hidden, query, metadata ->
        videos.filter { a -> hidden.none { it.filePath == a.filePath } && a.title.contains(searchQuery.value, ignoreCase = true) }
            .map { a ->
                val meta = metadata.find { it.filePath == a.filePath }
                if (meta != null) {
                    a.copy(title = meta.title, artist = meta.artist, album = meta.album, coverUri = meta.coverUri)
                } else a
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val rawAudioFiles: StateFlow<List<MediaModel>> = _audioFiles
    val rawVideoFiles: StateFlow<List<MediaModel>> = _videoFiles
    val favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val playlists = repository.playlists.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlistStats: StateFlow<Map<Long, PlaylistStats>> = combine(
        repository.allPlaylistMedia,
        _audioFiles,
        _videoFiles
    ) { crossRefs, audios, videos ->
        val audioPaths = audios.map { it.filePath }.toSet()
        val videoPaths = videos.map { it.filePath }.toSet()
        val map = mutableMapOf<Long, PlaylistStats>()
        crossRefs.forEach { ref ->
            val cur = map.getOrDefault(ref.playlistId, PlaylistStats())
            val isAudio = ref.filePath in audioPaths
            val isVideo = ref.filePath in videoPaths
            map[ref.playlistId] = cur.copy(
                audioCount = cur.audioCount + (if (isAudio) 1 else 0),
                videoCount = cur.videoCount + (if (isVideo) 1 else (if (!isAudio) 1 else 0))
            )
        }
        map
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val isExtracting: StateFlow<Boolean> = _isExtracting

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _audioFiles.value = repository.loadAudioFiles()
            _videoFiles.value = repository.loadVideoFiles()
        }
    }

    fun toggleFavorite(media: MediaModel) {
        viewModelScope.launch {
            val isFav = favorites.value.any { it.filePath == media.filePath }
            if (isFav) {
                repository.removeFavorite(media.filePath)
            } else {
                repository.addFavorite(media)
            }
        }
    }

    fun getPlaylistMediaFiles(playlistId: Long): kotlinx.coroutines.flow.Flow<List<MediaModel>> {
        return kotlinx.coroutines.flow.combine(
            repository.getMediaForPlaylist(playlistId),
            audioFiles,
            videoFiles
        ) { crossRefs, audios, videos ->
            val paths = crossRefs.map { it.filePath }
            val allMedia = audios + videos
            allMedia.filter { it.filePath in paths }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }
    
    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addMediaToPlaylist(playlistId: Long, filePath: String) {
        viewModelScope.launch {
            repository.addMediaToPlaylist(playlistId, filePath)
        }
    }

    fun removeMediaFromPlaylist(playlistId: Long, filePath: String) {
        viewModelScope.launch {
            repository.removeMediaFromPlaylist(playlistId, filePath)
        }
    }

    fun setShuffle(type: String, shuffle: Boolean) {
        viewModelScope.launch {
            repository.setShuffleMode(type, shuffle)
            if (type == "audio") {
                PlayerManager.exoPlayer?.shuffleModeEnabled = shuffle
            }
        }
    }
    
    fun hideMedia(filePath: String) {
        viewModelScope.launch {
            repository.hideMedia(filePath)
        }
    }
    
    fun deleteMediaFiles(filePaths: List<String>) {
        viewModelScope.launch {
            filePaths.forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadMedia()
        }
    }

    fun saveMetadata(filePath: String, title: String, artist: String, album: String, coverUri: String) {
        viewModelScope.launch {
            repository.saveMetadata(filePath, title, artist, album, coverUri)
        }
    }
    
    fun playNext(mediaModels: List<MediaModel>) {
        val player = PlayerManager.exoPlayer ?: return
        var insertIndex = player.currentMediaItemIndex + 1
        if (insertIndex > player.mediaItemCount) {
            insertIndex = player.mediaItemCount
        }
        val mediaItems = mediaModels.map { MediaItem.fromUri(it.uri) }
        player.addMediaItems(insertIndex, mediaItems)
    }

    fun extractAudioFromVideo(videoModel: MediaModel, onComplete: ((Boolean) -> Unit)? = null) {
        if (_isExtracting.value) return
        _isExtracting.value = true
        viewModelScope.launch {
            val context = getApplication<Application>()
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: context.getExternalFilesDir(null)!!
            if (!dir.exists()) dir.mkdirs()
            
            val sanitized = videoModel.title.replace(Regex("[^a-zA-Z0-9._\\-\\u0600-\\u06FF ]"), "_").trim()
            val cleanTitle = if (sanitized.isNotBlank()) sanitized else "audio_${System.currentTimeMillis()}"
            val outName = "${cleanTitle}_audio.mp3"
            val outFile = File(dir, outName)
            
            android.widget.Toast.makeText(context, "جاري تحويل الفيديو إلى أغنية MP3...", android.widget.Toast.LENGTH_SHORT).show()
            val success = AudioExtractor.extractAudio(context, videoModel.uri, videoModel.filePath, outFile.absolutePath)
            if (success && outFile.exists() && outFile.length() > 0) {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(outFile.absolutePath), null, null)
                android.widget.Toast.makeText(context, "تم تحويل وحفظ الأغنية بنجاح في مكتبة الأغاني!", android.widget.Toast.LENGTH_LONG).show()
                loadMedia() // reload to find the new audio track
                onComplete?.invoke(true)
            } else {
                android.widget.Toast.makeText(context, "فشل تحويل الفيديو. يرجى المحاولة مع ملف آخر.", android.widget.Toast.LENGTH_SHORT).show()
                onComplete?.invoke(false)
            }
            _isExtracting.value = false
        }
    }
    val selectedAudios = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    fun toggleSelection(filePath: String) {
        val current = selectedAudios.value.toMutableSet()
        if (current.contains(filePath)) current.remove(filePath) else current.add(filePath)
        selectedAudios.value = current
    }
    fun clearSelection() {
        selectedAudios.value = emptySet()
    }
    fun playNextPaths(paths: List<String>) {
        val models = _audioFiles.value.filter { it.filePath in paths }
        playNext(models)
    }

}
