package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(viewModel: MediaViewModel, onNavigateBack: () -> Unit) {
    val player = PlayerManager.exoPlayer ?: return
    var repeatMode by remember { mutableStateOf(player.repeatMode) }
    var shuffleMode by remember { mutableStateOf(player.shuffleModeEnabled) }
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    
    val queueItems = audios.take(10) // UI mock

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffleMode = enabled }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2C1E30))) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFF403045), MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(queueItems.firstOrNull()?.title ?: "", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White)
        }
        
        HorizontalDivider(color = Color.DarkGray)
        
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("في الانتظار", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Row {
                Icon(Icons.Filled.Crop, "Cover", tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Filled.ColorLens, "Theme", tint = Color.Gray)
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(queueItems) { audio ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFF403045), MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MusicNote, null, tint = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(audio.title, color = Color.White, maxLines = 1)
                        Text("Download", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Filled.DragHandle, "Drag", tint = Color.Gray)
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { player.shuffleModeEnabled = !shuffleMode }) {
                Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (shuffleMode) Color.White else Color.Gray)
            }
            IconButton(onClick = { player.repeatMode = Player.REPEAT_MODE_ALL }) {
                Icon(Icons.Filled.Repeat, "Repeat All", tint = if (repeatMode == Player.REPEAT_MODE_ALL) Color.White else Color.Gray)
            }
            IconButton(onClick = { player.repeatMode = Player.REPEAT_MODE_ONE }) {
                Icon(Icons.Filled.RepeatOne, "Repeat One", tint = if (repeatMode == Player.REPEAT_MODE_ONE) Color.White else Color.Gray)
            }
        }
    }
}
