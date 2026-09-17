package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.data.MediaModel
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(viewModel: MediaViewModel, onNavigateBack: () -> Unit) {
    val player = PlayerManager.exoPlayer ?: return
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentUri by remember { mutableStateOf(player.currentMediaItem?.localConfiguration?.uri?.toString()) }
    var repeatMode by remember { mutableStateOf(player.repeatMode) }
    var shuffleMode by remember { mutableStateOf(player.shuffleModeEnabled) }
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    val queueItems = remember { mutableStateListOf<MediaModel>() }
    var selectedMediaForPlaylist by remember { mutableStateOf<String?>(null) }

    // Synchronize queue items with player / audios
    LaunchedEffect(audios) {
        if (queueItems.isEmpty() && audios.isNotEmpty()) {
            if (player.mediaItemCount > 0) {
                val currentList = mutableListOf<MediaModel>()
                for (i in 0 until player.mediaItemCount) {
                    val itemUri = player.getMediaItemAt(i).localConfiguration?.uri?.toString()
                    val found = audios.find { it.uri.toString() == itemUri }
                    if (found != null) currentList.add(found)
                }
                if (currentList.isNotEmpty()) {
                    queueItems.clear()
                    queueItems.addAll(currentList)
                } else {
                    queueItems.addAll(audios)
                }
            } else {
                queueItems.addAll(audios)
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentUri = mediaItem?.localConfiguration?.uri?.toString()
            }
            override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffleMode = enabled }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val currentPlayingMedia = audios.find { it.uri.toString() == currentUri }
        ?: queueItems.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1422))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Text(
                text = "في الانتظار (${queueItems.size})",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            IconButton(onClick = {
                queueItems.shuffle()
                player.setMediaItems(queueItems.map { MediaItem.fromUri(it.uri) })
                player.prepare()
                player.play()
            }) {
                Icon(Icons.Filled.Shuffle, contentDescription = "خلط", tint = Color.White)
            }
        }

        // Currently Playing Card / Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF38233E),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF553260)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = "Playing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = "Paused",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentPlayingMedia?.title ?: "لا يوجد تشغيل حالي",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Text(
                        text = if (isPlaying) "جاري التشغيل الآن" else "متوقف مؤقتاً",
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = { player.seekToPreviousMediaItem() }) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "السابق",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (isPlaying) {
                            player.pause()
                        } else {
                            if (player.playbackState == Player.STATE_ENDED) {
                                player.seekTo(0, 0L)
                            }
                            player.play()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = { player.seekToNextMediaItem() }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "التالي",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Subheader instructions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الأغاني التالية (استخدم أزرار الترتيب للتحريك)",
                color = Color(0xFFC7B1D0),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "اضغط للتشغيل",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        HorizontalDivider(color = Color(0xFF33203A), thickness = 1.dp)

        // LazyColumn for Queue
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            itemsIndexed(queueItems, key = { _, item -> item.filePath }) { index, audio ->
                val isCurrentlyPlaying = audio.uri.toString() == currentUri
                val isFav = favorites.any { it.filePath == audio.filePath }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCurrentlyPlaying) Color(0xFF3A2341) else Color(0xFF28192D))
                        .clickable {
                            val targetIndex = queueItems.indexOf(audio)
                            if (targetIndex >= 0) {
                                if (player.mediaItemCount != queueItems.size) {
                                    player.setMediaItems(queueItems.map { MediaItem.fromUri(it.uri) })
                                    player.prepare()
                                }
                                player.seekTo(targetIndex, 0L)
                                player.play()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else Color(0xFF3D2644)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentlyPlaying && isPlaying) {
                            Icon(
                                Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audio.title,
                            color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Text(
                            text = if (audio.artist.isNotBlank()) audio.artist else "فنان غير معروف",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }

                    // Favorite button
                    IconButton(
                        onClick = { viewModel.toggleFavorite(audio) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "المفضلة",
                            tint = if (isFav) Color(0xFFFF2A6D) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add to Playlist button
                    IconButton(
                        onClick = { selectedMediaForPlaylist = audio.filePath },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlaylistAdd,
                            contentDescription = "إضافة لقائمة",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Move Up / Move Down buttons for robust, lag-free reordering
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val item = queueItems.removeAt(index)
                                    queueItems.add(index - 1, item)
                                    try {
                                        player.moveMediaItem(index, index - 1)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = "تحريك لأعلى",
                                tint = if (index > 0) Color.White else Color.Gray
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index < queueItems.size - 1) {
                                    val item = queueItems.removeAt(index)
                                    queueItems.add(index + 1, item)
                                    try {
                                        player.moveMediaItem(index, index + 1)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            enabled = index < queueItems.size - 1,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = "تحريك لأسفل",
                                tint = if (index < queueItems.size - 1) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls Bar (Shuffle & Repeat)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF27172C),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newShuffle = !shuffleMode
                    player.shuffleModeEnabled = newShuffle
                    shuffleMode = newShuffle
                }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        "خلط",
                        tint = if (shuffleMode) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = {
                    val nextRepeat = when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    player.repeatMode = nextRepeat
                    repeatMode = nextRepeat
                }) {
                    val icon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                        Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat
                        else -> Icons.Filled.Repeat
                    }
                    Icon(
                        icon,
                        "تكرار",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                TextButton(onClick = {
                    queueItems.clear()
                    player.clearMediaItems()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مسح القائمة", color = Color.LightGray)
                }
            }
        }
    }

    if (selectedMediaForPlaylist != null) {
        com.example.ui.components.AddToPlaylistDialog(
            mediaPaths = listOf(selectedMediaForPlaylist!!),
            viewModel = viewModel,
            onDismiss = { selectedMediaForPlaylist = null }
        )
    }
}
