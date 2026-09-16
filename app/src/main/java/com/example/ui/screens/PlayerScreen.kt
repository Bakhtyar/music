package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel
import com.example.data.MediaModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val player = PlayerManager.exoPlayer ?: return
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableStateOf(player.currentPosition) }
    var duration by remember { mutableStateOf(player.duration.coerceAtLeast(0L)) }
    var currentUri by remember { mutableStateOf(player.currentMediaItem?.localConfiguration?.uri?.toString()) }
    
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val media = audios.find { it.uri.toString() == currentUri }
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite = favorites.any { it.filePath == media?.filePath }
    var showMetadataDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) { isPlaying = isPlayingNow }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0L)
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                currentUri = mediaItem?.localConfiguration?.uri?.toString()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF2C1E30)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down", modifier = Modifier.size(48.dp), tint = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier.weight(1f).aspectRatio(1f).clip(MaterialTheme.shapes.extraLarge).background(Color(0xFF403045)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(120.dp), tint = Color.Gray)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { showMetadataDialog = true }) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }
            Text(media?.title ?: "Unknown", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { media?.let { viewModel.toggleFavorite(it) } }) {
                Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorite", tint = if (isFavorite) Color.Red else Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f,
            onValueChange = { value ->
                val newPos = (value * duration).toLong()
                player.seekTo(newPos)
                currentPosition = newPos
            },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.DarkGray),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(currentPosition), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Text(formatTime(duration), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player.seekToPreviousMediaItem() }) { Icon(Icons.Filled.FastRewind, "Previous", modifier = Modifier.size(48.dp), tint = Color.White) }
            IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", modifier = Modifier.size(72.dp), tint = Color.White)
            }
            IconButton(onClick = { player.seekToNextMediaItem() }) { Icon(Icons.Filled.FastForward, "Next", modifier = Modifier.size(48.dp), tint = Color.White) }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onOpenEqualizer) { Icon(Icons.Filled.Tune, "Equalizer", tint = Color.White) }
            ElevatedButton(onClick = { showLyricsDialog = true }, colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF403045), contentColor = Color.White)) {
                Icon(Icons.Filled.ChatBubbleOutline, "Lyrics", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("الكلمات")
            }
            IconButton(onClick = onOpenQueue) { Icon(Icons.Filled.QueueMusic, "Queue", tint = Color.White) }
        }
    }
    
    if (showMetadataDialog && media != null) {
        EditMetadataDialog(media, viewModel) { showMetadataDialog = false }
    }
    
    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsDialog = false },
            title = { Text("الكلمات") },
            text = { Text("لم يتم العثور على كلمات مدمجة لهذه الأغنية.") },
            confirmButton = { TextButton(onClick = { showLyricsDialog = false }) { Text("حسناً") } }
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
