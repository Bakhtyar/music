package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MediaModel
import com.example.ui.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    playlistId: Long,
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlist = playlists.find { it.id == playlistId }
    
    // We remember the flow to prevent recompositions creating new flows
    val mediaFlow = remember(playlistId) { viewModel.getPlaylistMediaFiles(playlistId) }
    val playlistMedia by mediaFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showAddSongsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "قائمة التشغيل", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSongsDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Add Songs", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFF121212))) {
            if (playlistMedia.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("القائمة فارغة", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("اضغط على الزر أدناه لإضافة أغانٍ", color = Color.DarkGray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(playlistMedia, key = { it.id }) { media ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val index = playlistMedia.indexOf(media)
                                    val player = com.example.player.PlayerManager.exoPlayer
                                    player?.setMediaItems(playlistMedia.map { androidx.media3.common.MediaItem.fromUri(it.uri) })
                                    player?.seekTo(index, 0)
                                    player?.prepare()
                                    player?.play()
                                    onNavigateToPlayer()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFF2C2C2C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = "Music", tint = Color.Gray)
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(media.title, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1)
                                Text(if (media.artist.isNotBlank()) media.artist else "فنان غير معروف", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddSongsDialog) {
        AddSongsToPlaylistDialog(
            playlistId = playlistId,
            viewModel = viewModel,
            onDismiss = { showAddSongsDialog = false }
        )
    }
}

@Composable
fun AddSongsToPlaylistDialog(playlistId: Long, viewModel: MediaViewModel, onDismiss: () -> Unit) {
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val mediaFlow = remember(playlistId) { viewModel.getPlaylistMediaFiles(playlistId) }
    val alreadyInPlaylist by mediaFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val selectedPaths = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة أغانٍ للقائمة") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(audios) { audio ->
                    val isInPlaylist = alreadyInPlaylist.any { it.filePath == audio.filePath }
                    if (!isInPlaylist) {
                        val isSelected = selectedPaths.contains(audio.filePath)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedPaths.remove(audio.filePath)
                                    else selectedPaths.add(audio.filePath)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(audio.title, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                selectedPaths.forEach { path ->
                    viewModel.addMediaToPlaylist(playlistId, path)
                }
                onDismiss()
            }) {
                Text("إضافة (${selectedPaths.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
