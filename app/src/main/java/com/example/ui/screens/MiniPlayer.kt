package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FastForward
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
import kotlinx.coroutines.delay

@Composable
fun MiniPlayer(viewModel: MediaViewModel, onClick: () -> Unit) {
    val exoPlayer = PlayerManager.exoPlayer ?: return
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentUri by remember { mutableStateOf(exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()) }
    var currentPosition by remember { mutableStateOf(exoPlayer.currentPosition) }
    var duration by remember { mutableStateOf(exoPlayer.duration.coerceAtLeast(0L)) }
    
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val media = audios.find { it.uri.toString() == currentUri } ?: return

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) { isPlaying = isPlayingNow }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentUri = mediaItem?.localConfiguration?.uri?.toString()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) duration = exoPlayer.duration.coerceAtLeast(0L)
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF2C1E30)).clickable(onClick = onClick)
    ) {
        LinearProgressIndicator(
            progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.DarkGray
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { exoPlayer.seekToNextMediaItem() }) {
                Icon(Icons.Filled.FastForward, contentDescription = "Next", tint = Color.White)
            }
            
            Text(media.title, color = Color.White, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1)
            
            Box(
                modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(Color(0xFF403045)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}
