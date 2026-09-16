package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.example.data.MediaModel
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToVideoPlayer: (String, Long?) -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val audioFiles by viewModel.audioFiles.collectAsStateWithLifecycle()
    val videoFiles by viewModel.videoFiles.collectAsStateWithLifecycle()

    val favoriteAudios = remember(favorites, audioFiles) {
        val favPaths = favorites.filter { it.mediaType == "AUDIO" }.map { it.filePath }.toSet()
        audioFiles.filter { it.filePath in favPaths }
    }

    val favoriteVideos = remember(favorites, videoFiles) {
        val favPaths = favorites.filter { it.mediaType == "VIDEO" }.map { it.filePath }.toSet()
        videoFiles.filter { it.filePath in favPaths }
    }

    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()

    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Songs, 1: Videos
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            kotlinx.coroutines.delay(2000)
            feedbackMessage = null
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF2A6D),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "المفضلة",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF101626))
            )
        },
        bottomBar = {
            MiniPlayer(viewModel = viewModel, onClick = onNavigateToPlayer)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF381232),
                                    Color(0xFF131B33),
                                    Color(0xFF0F2B48)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "مجموعتك المفضلة",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${favoriteAudios.size} أغانٍ • ${favoriteVideos.size} فيديوهات قصيرة",
                                color = Color(0xFF38BDF8),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF2A6D).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF2A6D),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Segmented Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF101626),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF38BDF8),
                                height = 3.dp
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الأغاني (${favoriteAudios.size})")
                            }
                        },
                        selectedContentColor = Color(0xFF38BDF8),
                        unselectedContentColor = Color.Gray
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فيديوهات تيك توك (${favoriteVideos.size})")
                            }
                        },
                        selectedContentColor = Color(0xFF38BDF8),
                        unselectedContentColor = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons: Play All & Shuffle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedTab == 0 && favoriteAudios.isNotEmpty()) {
                                PlayerManager.exoPlayer?.let { player ->
                                    player.stop()
                                    player.clearMediaItems()
                                    player.setMediaItems(favoriteAudios.map { MediaItem.fromUri(it.uri) })
                                    player.prepare()
                                    player.play()
                                }
                                onNavigateToPlayer()
                            } else if (selectedTab == 1 && favoriteVideos.isNotEmpty()) {
                                onNavigateToVideoPlayer(favoriteVideos.first().uri.toString(), -1L)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تشغيل الكل", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (selectedTab == 0 && favoriteAudios.isNotEmpty()) {
                                val shuffled = favoriteAudios.shuffled()
                                PlayerManager.exoPlayer?.let { player ->
                                    player.stop()
                                    player.clearMediaItems()
                                    player.setMediaItems(shuffled.map { MediaItem.fromUri(it.uri) })
                                    player.prepare()
                                    player.play()
                                }
                                onNavigateToPlayer()
                            } else if (selectedTab == 1 && favoriteVideos.isNotEmpty()) {
                                val randomVideo = favoriteVideos.random()
                                onNavigateToVideoPlayer(randomVideo.uri.toString(), -1L)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تشغيل عشوائي", color = Color.White)
                    }
                }

                // Content Lists
                if (selectedTab == 0) {
                    if (favoriteAudios.isEmpty()) {
                        EmptyFavoriteState(
                            icon = Icons.Filled.MusicNote,
                            title = "لا توجد أغانٍ مفضلة بعد",
                            subtitle = "اضغط على أيقونة القلب ❤️ في أي أغنية لإضافتها إلى قائمتك المفضلة."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(favoriteAudios, key = { it.filePath }) { song ->
                                val isSelected = selectedItems.contains(song.filePath)
                                FavoriteSongCard(
                                    song = song,
                                    isSelected = isSelected,
                                    selectionMode = selectionMode,
                                    onPlay = {
                                        if (selectionMode) {
                                            viewModel.toggleSelection(song.filePath)
                                        } else {
                                            PlayerManager.exoPlayer?.let { player ->
                                                val index = favoriteAudios.indexOf(song).coerceAtLeast(0)
                                                player.stop()
                                                player.clearMediaItems()
                                                player.setMediaItems(favoriteAudios.map { MediaItem.fromUri(it.uri) }, index, 0L)
                                                player.prepare()
                                                player.play()
                                            }
                                            onNavigateToPlayer()
                                        }
                                    },
                                    onRemove = {
                                        viewModel.toggleFavorite(song)
                                        feedbackMessage = "تمت إزالة ${song.title} من المفضلة"
                                    },
                                    onToggleSelection = {
                                        viewModel.toggleSelection(song.filePath)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (favoriteVideos.isEmpty()) {
                        EmptyFavoriteState(
                            icon = Icons.Filled.VideoLibrary,
                            title = "لا توجد فيديوهات مفضلة بعد",
                            subtitle = "أثناء مشاهدة الفيديوهات مثل التيك توك، اضغط مرتين أو على أيقونة القلب ❤️ للإضافة هنا."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favoriteVideos, key = { it.filePath }) { video ->
                                FavoriteVideoCard(
                                    video = video,
                                    onPlay = {
                                        onNavigateToVideoPlayer(video.uri.toString(), -1L)
                                    },
                                    onRemove = {
                                        viewModel.toggleFavorite(video)
                                        feedbackMessage = "تمت إزالة الفيديو من المفضلة"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating feedback toast
            AnimatedVisibility(
                visible = feedbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = feedbackMessage ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteSongCard(
    song: MediaModel,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onToggleSelection
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFF13182C)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (song.coverUri.isNotEmpty()) {
                    AsyncImage(
                        model = song.coverUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (song.artist.isNotBlank()) song.artist else "فنان غير معروف",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "إزالة من المفضلة",
                    tint = Color(0xFFFF2A6D),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FavoriteVideoCard(
    video: MediaModel,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13182C)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp, 50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = Color(0xFFFF2A6D),
                    modifier = Modifier.size(28.dp)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "فيديو تيك توك",
                            color = Color(0xFF38BDF8),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (video.duration > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatDuration(video.duration),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "إزالة من المفضلة",
                    tint = Color(0xFFFF2A6D),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyFavoriteState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161F30)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8).copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
