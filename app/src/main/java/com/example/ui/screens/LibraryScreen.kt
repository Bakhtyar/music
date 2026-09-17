package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.example.data.MediaModel
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MediaViewModel,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToVideos: () -> Unit = {}
) {
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val videos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()

    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val recentAudios = audios

    var showPlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!selectionMode) {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث في مكتبتك...", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Filled.ArrowBack, "إغلاق البحث", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                "مكتبتي",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Filled.Search, "بحث", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Filled.Settings, "الإعدادات والمظهر", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 2x2 Feature Cards Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LibraryFeatureCard(
                            title = "المفضلة",
                            subtitle = "${favorites.size} عنصر",
                            icon = Icons.Filled.Favorite,
                            iconColor = Color(0xFFFF2A6D),
                            bgGradient = listOf(Color(0xFF2C103D), Color(0xFF13182C)),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToFavorites
                        )
                        LibraryFeatureCard(
                            title = "فيديوهات تيك توك",
                            subtitle = "${videos.size} مقطع",
                            icon = Icons.Filled.VideoLibrary,
                            iconColor = Color(0xFF00E5FF),
                            bgGradient = listOf(Color(0xFF0A2B47), Color(0xFF13182C)),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToVideos
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LibraryFeatureCard(
                            title = "قوائم التشغيل",
                            subtitle = "${playlists.size} قائمة",
                            icon = Icons.Filled.QueueMusic,
                            iconColor = Color(0xFF00B0FF),
                            bgGradient = listOf(Color(0xFF0C2B4D), Color(0xFF13182C)),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToPlaylists
                        )
                        LibraryFeatureCard(
                            title = "الملفات الصوتية",
                            subtitle = "${audios.size} أغنية",
                            icon = Icons.Filled.Folder,
                            iconColor = Color(0xFFFF9100),
                            bgGradient = listOf(Color(0xFF33200D), Color(0xFF13182C)),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (audios.isNotEmpty()) {
                                    val player = PlayerManager.initPlayer(viewModel.getApplication())
                                    player.stop()
                                    player.clearMediaItems()
                                    player.addMediaItems(audios.map { MediaItem.fromUri(it.uri) })
                                    player.prepare()
                                    player.play()
                                    onNavigateToPlayer()
                                }
                            }
                        )
                    }
                }
            }

            // Section Header: أحدث الإضافات
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "أحدث الإضافات",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Recent songs list
            val displayList = if (searchQuery.isNotEmpty()) {
                recentAudios.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
            } else {
                recentAudios
            }

            items(displayList, key = { it.filePath }) { song ->
                val isSelected = selectedItems.contains(song.filePath)
                SongRowItem(
                    song = song,
                    isSelected = isSelected,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) {
                            viewModel.toggleSelection(song.filePath)
                        } else {
                            val player = PlayerManager.initPlayer(viewModel.getApplication())
                            player.stop()
                            player.clearMediaItems()
                            player.setMediaItem(MediaItem.fromUri(song.uri))
                            player.prepare()
                            player.play()
                            onNavigateToPlayer()
                        }
                    },
                    onLongClick = {
                        viewModel.toggleSelection(song.filePath)
                    }
                )
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
fun LibraryFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    bgGradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(105.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(bgGradient))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRowItem(
    song: MediaModel,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color.Transparent)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUri.isNotEmpty()) {
                AsyncImage(
                    model = song.coverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                song.artist.ifEmpty { "فنان غير معروف" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Text(
            formatTime(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (!selectionMode) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "تشغيل",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
