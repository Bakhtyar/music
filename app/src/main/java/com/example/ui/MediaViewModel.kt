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

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(
        application,
        AppDatabase.getDatabase(application).mediaDao()
    )

    private val _audioFiles = MutableStateFlow<List<MediaModel>>(emptyList())
    private val _videoFiles = MutableStateFlow<List<MediaModel>>(emptyList())
    private val _isExtracting = MutableStateFlow(false)

    val audioShuffleMode = repository.getShuffleMode("audio").stateIn(viewModelScope, SharingStarted.Lazily, false)
    val videoShuffleMode = repository.getShuffleMode("video").stateIn(viewModelScope, SharingStarted.Lazily, false)

    val audioFiles: StateFlow<List<MediaModel>> = combine(
        _audioFiles,
        repository.hiddenMedia,
        repository.mediaMetadata
    ) { audios, hidden, metadata ->
        audios.filter { a -> hidden.none { it.filePath == a.filePath } }
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
        repository.mediaMetadata
    ) { videos, hidden, metadata ->
        videos.filter { a -> hidden.none { it.filePath == a.filePath } }
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

    fun extractAudioFromVideo(videoModel: MediaModel) {
        if (_isExtracting.value) return
        _isExtracting.value = true
        viewModelScope.launch {
            val context = getApplication<Application>()
            val dir = File(context.getExternalFilesDir(null), "ExtractedAudio")
            if (!dir.exists()) dir.mkdirs()
            
            val outName = "${videoModel.title}_audio.mp3"
            val outFile = File(dir, outName)
            
            val success = AudioExtractor.extractAudio(videoModel.filePath, outFile.absolutePath)
            if (success) {
                loadMedia() // reload to find the new mp3
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
