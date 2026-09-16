package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.data.MediaModel
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel
import com.example.ui.theme.LayoutDensity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioScreen(viewModel: MediaViewModel, onNavigateToPlayer: () -> Unit = {}) {
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()
    var mediaToEdit by remember { mutableStateOf<MediaModel?>(null) }
    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    
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

    val onItemClick: (MediaModel) -> Unit = { audio ->
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
    }

    val onItemLongClick: (MediaModel) -> Unit = { audio ->
        viewModel.toggleSelection(audio.filePath)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (themeState.density) {
            LayoutDensity.LARGE -> {
                // 2-Column Grid with Large Cards (Screenshot 5)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(audios, key = { it.id }) { audio ->
                        val isSelected = selectedItems.contains(audio.filePath)
                        val isCurrentlyPlaying = audio.uri.toString() == currentlyPlayingUri

                        LargeAudioGridCard(
                            audio = audio,
                            isSelected = isSelected,
                            isCurrentlyPlaying = isCurrentlyPlaying && isPlaying,
                            selectionMode = selectionMode,
                            onClick = { onItemClick(audio) },
                            onLongClick = { onItemLongClick(audio) },
                            onMoreClick = { mediaToEdit = audio }
                        )
                    }
                }
            }
            LayoutDensity.MEDIUM -> {
                // Comfortable List Cards (Screenshot 1 & 2)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(audios, key = { it.id }) { audio ->
                        val isSelected = selectedItems.contains(audio.filePath)
                        val isCurrentlyPlaying = audio.uri.toString() == currentlyPlayingUri

                        MediumAudioListItem(
                            audio = audio,
                            isSelected = isSelected,
                            isCurrentlyPlaying = isCurrentlyPlaying && isPlaying,
                            selectionMode = selectionMode,
                            onClick = { onItemClick(audio) },
                            onLongClick = { onItemLongClick(audio) },
                            onMoreClick = { mediaToEdit = audio }
                        )
                    }
                }
            }
            LayoutDensity.SMALL -> {
                // Compact High-Density List (Screenshot 4)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(audios, key = { it.id }) { audio ->
                        val isSelected = selectedItems.contains(audio.filePath)
                        val isCurrentlyPlaying = audio.uri.toString() == currentlyPlayingUri

                        SmallAudioListItem(
                            audio = audio,
                            isSelected = isSelected,
                            isCurrentlyPlaying = isCurrentlyPlaying && isPlaying,
                            selectionMode = selectionMode,
                            onClick = { onItemClick(audio) },
                            onLongClick = { onItemLongClick(audio) },
                            onMoreClick = { mediaToEdit = audio }
                        )
                    }
                }
            }
        }
    }
    
    mediaToEdit?.let { EditMetadataDialog(it, viewModel) { mediaToEdit = null } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LargeAudioGridCard(
    audio: MediaModel,
    isSelected: Boolean,
    isCurrentlyPlaying: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (audio.coverUri != null) {
                    AsyncImage(
                        model = audio.coverUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        if (isCurrentlyPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
                if (selectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = audio.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = audio.artist.ifBlank { "فنان غير معروف" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (!selectionMode) {
                    IconButton(onClick = onMoreClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediumAudioListItem(
    audio: MediaModel,
    isSelected: Boolean,
    isCurrentlyPlaying: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (audio.coverUri != null) {
                AsyncImage(
                    model = audio.coverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    if (isCurrentlyPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = audio.artist.ifBlank { "فنان غير معروف" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (!selectionMode) {
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Filled.MoreVert, "خيارات", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmallAudioListItem(
    audio: MediaModel,
    isSelected: Boolean,
    isCurrentlyPlaying: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (audio.coverUri != null) {
                AsyncImage(
                    model = audio.coverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    if (isCurrentlyPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = audio.artist.ifBlank { "فنان غير معروف" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (!selectionMode) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.MoreVert, "خيارات", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
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
