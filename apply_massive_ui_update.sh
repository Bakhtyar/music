#!/bin/bash

# 1. Update MediaViewModel
sed -i '/^}$/i \
    val selectedAudios = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())\
    fun toggleSelection(filePath: String) {\
        val current = selectedAudios.value.toMutableSet()\
        if (current.contains(filePath)) current.remove(filePath) else current.add(filePath)\
        selectedAudios.value = current\
    }\
    fun clearSelection() {\
        selectedAudios.value = emptySet()\
    }\
    fun playNextPaths(paths: List<String>) {\
        val models = _audioFiles.value.filter { it.filePath in paths }\
        playNext(models)\
    }\
' app/src/main/java/com/example/ui/MediaViewModel.kt

# 2. AppNavigation
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/AppNavigation.kt
package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*

@Composable
fun AppNavigation(viewModel: MediaViewModel) {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(viewModel, onNavigateToPlayer = { navController.navigate("player") })
            }
            composable("player") {
                PlayerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenEqualizer = { navController.navigate("equalizer") },
                    onOpenQueue = { navController.navigate("queue") }
                )
            }
            composable("equalizer") {
                EqualizerScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("queue") {
                QueueScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
INNER_EOF

# 3. MainScreen
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/MainScreen.kt
package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MediaViewModel, onNavigateToPlayer: () -> Unit) {
    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()
    var selectedTab by remember { mutableStateOf(1) } // 0: Videos, 1: Songs, 2: Playlists
    
    val context = LocalContext.current
    var showPlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedItems.size} محددة", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.ArrowForward, "Cancel", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                )
            } else {
                Column(modifier = Modifier.background(Color(0xFF121212))) {
                    TopAppBar(
                        title = { Text("Lark Player", style = MaterialTheme.typography.titleLarge, color = Color.White) },
                        actions = {
                            IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Visibility, "Hidden", tint = Color.White) }
                            IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Sort, "Sort", tint = Color.White) }
                            IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Search, "Search", tint = Color.White) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                    )
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF121212),
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("الفيديوهات") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("الأغاني") })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("قوائم التشغيل") })
                    }
                }
            }
        },
        bottomBar = {
            if (selectionMode) {
                BottomAppBar(containerColor = Color(0xFF2C1E30), contentColor = Color.White) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ActionButton(Icons.Filled.Delete, "حذف") {
                            viewModel.deleteMediaFiles(selectedItems.toList())
                            viewModel.clearSelection()
                        }
                        ActionButton(Icons.Filled.VisibilityOff, "إخفاء") {
                            selectedItems.forEach { viewModel.hideMedia(it) }
                            viewModel.clearSelection()
                        }
                        ActionButton(Icons.Filled.Share, "مشاركة") {
                            val uris = ArrayList(selectedItems.map { android.net.Uri.parse(it) })
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "audio/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share"))
                            viewModel.clearSelection()
                        }
                        ActionButton(Icons.Filled.QueueMusic, "تشغيل تالياً") {
                            viewModel.playNextPaths(selectedItems.toList())
                            viewModel.clearSelection()
                        }
                        ActionButton(Icons.Filled.PlaylistAdd, "إضافة إلى") { showPlaylistDialog = true }
                    }
                }
            } else {
                MiniPlayer(viewModel, onClick = onNavigateToPlayer)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFF121212))) {
            when (selectedTab) {
                0 -> VideoScreen(viewModel)
                1 -> AudioScreen(viewModel)
                2 -> PlaylistsScreen(viewModel)
            }
        }
    }
    
    if (showPlaylistDialog) {
        com.example.ui.components.AddToPlaylistDialog(
            mediaPaths = selectedItems.toList(),
            viewModel = viewModel,
            onDismiss = { 
                showPlaylistDialog = false
                viewModel.clearSelection()
            }
        )
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(8.dp)) {
        Icon(icon, contentDescription = label, tint = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
INNER_EOF

# 4. AudioScreen
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/AudioScreen.kt
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioScreen(viewModel: MediaViewModel) {
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()
    
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
            val isCurrentlyPlaying = audio.uri == currentlyPlayingUri
            
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
            }
        }
    }
}
INNER_EOF

# 5. MiniPlayer
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/MiniPlayer.kt
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
    val media = audios.find { it.uri == currentUri } ?: return

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
INNER_EOF

# 6. AddToPlaylistDialog
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/components/AddToPlaylistDialog.kt
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    mediaPaths: List<String>,
    viewModel: MediaViewModel,
    onDismiss: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF2C2C2C)) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("إضافة الى قائمة تشغيل", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(16.dp))
            
            ListItem(
                headlineContent = { Text("قائمة تشغيل جديدة", color = Color.White) },
                supportingContent = { Text("٠ أغنية", color = Color.Gray) },
                trailingContent = {
                    Box(modifier = Modifier.size(48.dp).background(Color(0xFF404040), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                    }
                },
                modifier = Modifier.clickable { showCreateDialog = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { Text("أغاني أعجبتني", color = Color.White) },
                supportingContent = { Text("١٥ أغنية", color = Color.Gray) },
                trailingContent = {
                    Box(modifier = Modifier.size(48.dp).background(Color(0xFF404040), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = "Playlist", tint = Color.White)
                    }
                },
                modifier = Modifier.clickable { onDismiss() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            LazyColumn {
                items(playlists) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name, color = Color.White) },
                        supportingContent = { Text("٠ أغنية", color = Color.Gray) },
                        trailingContent = {
                            Box(modifier = Modifier.size(48.dp).background(Color(0xFF404040), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.QueueMusic, contentDescription = "Playlist", tint = Color.White)
                            }
                        },
                        modifier = Modifier.clickable {
                            mediaPaths.forEach { path -> viewModel.addMediaToPlaylist(playlist.id, path) }
                            onDismiss()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF2C2C2C),
            title = { Text("قائمة تشغيل جديدة", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم قائمة التشغيل") },
                    supportingText = { Text("${name.length}/200", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) viewModel.createPlaylist(name)
                    showCreateDialog = false
                }, enabled = name.isNotBlank()) { Text("إنشاء") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("إلغاء الأمر") }
            }
        )
    }
}
INNER_EOF

# 7. PlayerScreen
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/PlayerScreen.kt
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
    val media = audios.find { it.uri == currentUri }
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite = favorites.any { it.filePath == media?.filePath }

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
            IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }
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
            ElevatedButton(onClick = { /* Lyrics */ }, colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF403045), contentColor = Color.White)) {
                Icon(Icons.Filled.ChatBubbleOutline, "Lyrics", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("الكلمات")
            }
            IconButton(onClick = onOpenQueue) { Icon(Icons.Filled.QueueMusic, "Queue", tint = Color.White) }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
INNER_EOF

# 8. QueueScreen
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/QueueScreen.kt
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
INNER_EOF

# 9. EqualizerScreen
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/EqualizerScreen.kt
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.player.PlayerManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Mic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onNavigateBack: () -> Unit) {
    val equalizer = PlayerManager.equalizer ?: return
    var isEnabled by remember { mutableStateOf(equalizer.enabled) }
    
    val numBands = equalizer.numberOfBands.toInt()
    val bandLevels = remember { mutableStateListOf<Int>() }
    
    LaunchedEffect(Unit) {
        if (bandLevels.isEmpty()) {
            for (i in 0 until numBands) bandLevels.add(equalizer.getBandLevel(i.toShort()).toInt())
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2C1E30)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("المعدّل", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it; equalizer.enabled = it })
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            val minEQ = equalizer.bandLevelRange[0].toFloat()
            val maxEQ = equalizer.bandLevelRange[1].toFloat()
            
            for (i in 0 until numBands) {
                if (i < bandLevels.size) {
                    val freq = equalizer.getCenterFreq(i.toShort()) / 1000
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("+5", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Slider(
                                value = bandLevels[i].toFloat(),
                                onValueChange = { 
                                    bandLevels[i] = it.toInt()
                                    if(isEnabled) equalizer.setBandLevel(i.toShort(), it.toInt().toShort())
                                },
                                valueRange = minEQ..maxEQ,
                                modifier = Modifier.rotate(-90f).width(200.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.DarkGray)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (freq >= 1000) "${freq/1000}K" else "$freq", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PresetCard("معزز الباس", Icons.Filled.Speaker, modifier = Modifier.weight(1f))
            PresetCard("مخصص", Icons.Filled.GraphicEq, modifier = Modifier.weight(1f), isActive = true)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PresetCard("معزز الصوت المرتفع", Icons.Filled.Speaker, modifier = Modifier.weight(1f))
            PresetCard("معزز الصوت البشري", Icons.Filled.Mic, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PresetCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, isActive: Boolean = false) {
    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .background(if (isActive) Color.White else Color.Transparent, RoundedCornerShape(16.dp))
            .border(1.dp, if (isActive) Color.Transparent else Color.DarkGray, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = if (isActive) Color.Black else Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = if (isActive) Color.Black else Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
INNER_EOF

# 10. PlaylistsScreen
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MediaViewModel

@Composable
fun PlaylistsScreen(viewModel: MediaViewModel) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(2f).background(Color(0xFF2C2C2C), MaterialTheme.shapes.large).clickable { /* TODO */ },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Add, "Add", tint = Color.Gray, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("إضافة جديد", color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        item { SmartPlaylistCard("أغاني أعجبتني", "15", Icons.Filled.Favorite, Color.Red) }
        item { SmartPlaylistCard("المشغلة مؤخراً", "99", Icons.Filled.History, Color.White) }
        item { SmartPlaylistCard("الأكثر تشغيلاً", "99", Icons.Filled.Whatshot, Color.White) }
        
        items(playlists) { playlist ->
            SmartPlaylistCard(playlist.name, "0", Icons.Filled.QueueMusic, Color.White)
        }
    }
}

@Composable
fun SmartPlaylistCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color(0xFF2C2C2C), MaterialTheme.shapes.large).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Text("$count أغنية", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Box(modifier = Modifier.size(64.dp).background(Color(0xFF404040), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
        }
    }
}
INNER_EOF
