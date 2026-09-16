package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel
import com.example.data.MediaModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioScreen(viewModel: MediaViewModel, onNavigateToPlayer: () -> Unit = {}) {
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()
    var mediaToEdit by remember { mutableStateOf<MediaModel?>(null) }
    
    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val exoPlayer = PlayerManager.exoPlayer
    
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) { isPlaying = isPlayingNow }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentlyPlayingUri = mediaItem?.localConfiguration?.uri?.toString()
            }
        }
        exoPlayer?.addListener(listener)
        onDispose { exoPlayer?.removeListener(listener) }
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(audios, key = { it.id }) { audio ->
            val isSelected = selectedItems.contains(audio.filePath)
            val isCurrentlyPlaying = audio.uri.toString() == currentlyPlayingUri
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (selectionMode) {
                                viewModel.toggleSelection(audio.filePath)
                            } else {
                                val index = audios.indexOf(audio)
                                exoPlayer?.setMediaItems(audios.map { MediaItem.fromUri(it.uri) })
                                exoPlayer?.seekTo(index, 0)
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                                onNavigateToPlayer()
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(audio.filePath) }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFF2C2C2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentlyPlaying && isPlaying) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = "Playing", tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Filled.MusicNote, contentDescription = "Music", tint = Color.Gray)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(audio.title, style = MaterialTheme.typography.bodyLarge, color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else Color.White, maxLines = 1)
                    Text("Download", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                
                if (!selectionMode) {
                    IconButton(onClick = { mediaToEdit = audio }) {
                        Icon(Icons.Filled.MoreVert, "More", tint = Color.Gray)
                    }
                }
            }
        }
    }
    
    mediaToEdit?.let { EditMetadataDialog(it, viewModel) { mediaToEdit = null } }
}

@Composable
fun EditMetadataDialog(media: MediaModel, viewModel: MediaViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(media.title) }
    var artist by remember { mutableStateOf(media.artist) }
    var album by remember { mutableStateOf(media.album) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل المعلومات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") })
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveMetadata(media.filePath, title, artist, album, media.coverUri)
                onDismiss()
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
