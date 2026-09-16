package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.MediaModel
import com.example.ui.MediaViewModel
import com.example.ui.components.AddToPlaylistDialog

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideosListScreen(
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVideoPlayer: (String) -> Unit
) {
    val videos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()

    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
    }

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val favoritePaths = remember(favorites) {
        favorites.filter { it.mediaType == "VIDEO" }.map { it.filePath }.toSet()
    }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) videos
        else videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F19),
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            "${selectedItems.size} فيديو محدد",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, "إلغاء التحديد", tint = Color.White)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val allPaths = filteredVideos.map { it.filePath }
                                viewModel.selectAll(allPaths)
                            }
                        ) {
                            Text("تحديد الكل", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13182C))
                )
            } else if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث في الفيديوهات...", color = Color.Gray) },
                            singleLine = true,
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
                            Icon(Icons.Filled.ArrowBack, "رجوع", tint = Color.White)
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, "مسح", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13182C))
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "الفيديوهات المرتبة",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "${filteredVideos.size}",
                                    color = Color(0xFF38BDF8),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, "رجوع", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, "بحث", tint = Color.White)
                        }
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "تغيير طريقة العرض",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13182C))
                )
            }
        },
        bottomBar = {
            if (selectionMode) {
                BottomAppBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = Color(0xFF161F30),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionButton(Icons.Filled.Favorite, "المفضلة") {
                            viewModel.addSelectedToFavorites()
                        }
                        ActionButton(Icons.Filled.PlaylistAdd, "إضافة لقائمة") {
                            showPlaylistDialog = true
                        }
                        ActionButton(Icons.Filled.PlayCircle, "عرض كتيك توك") {
                            viewModel.playSelectedVideos { uri ->
                                onNavigateToVideoPlayer(uri)
                            }
                        }
                        ActionButton(Icons.Filled.MusicVideo, "تحويل لـ MP3") {
                            val vids = viewModel.rawVideoFiles.value
                            selectedItems.forEach { path ->
                                vids.find { it.filePath == path }?.let { video ->
                                    viewModel.extractAudioFromVideo(video)
                                }
                            }
                            viewModel.clearSelection()
                        }
                        ActionButton(Icons.Filled.Share, "مشاركة") {
                            val uris = ArrayList(selectedItems.map { android.net.Uri.parse(it) })
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "video/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة الفيديوهات"))
                            viewModel.clearSelection()
                        }
                        ActionButton(Icons.Filled.VisibilityOff, "إخفاء") {
                            selectedItems.forEach { viewModel.hideMedia(it) }
                            viewModel.clearSelection()
                        }
                        ActionButton(Icons.Filled.Delete, "حذف") {
                            viewModel.deleteMediaFiles(selectedItems.toList())
                            viewModel.clearSelection()
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
        ) {
            if (filteredVideos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.VideoLibrary,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotEmpty()) "لا توجد نتائج بحث" else "لا توجد مقاطع فيديو في جهازك",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Quick Action Banner
                    if (!selectionMode) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val first = filteredVideos.firstOrNull()
                                    if (first != null) onNavigateToVideoPlayer(first.uri.toString())
                                },
                            color = Color(0xFF161F38)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF2979FF)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("مشغل تيك توك الرأسي", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("اضغط مطولاً على أي فيديو لتحديده ونقله للمفضلة أو للقوائم", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Color(0xFF38BDF8))
                            }
                        }
                    }

                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredVideos, key = { it.id }) { video ->
                                val isSelected = selectedItems.contains(video.filePath)
                                val isFavorite = favoritePaths.contains(video.filePath)

                                VideoGridCard(
                                    video = video,
                                    isSelected = isSelected,
                                    selectionMode = selectionMode,
                                    isFavorite = isFavorite,
                                    onClick = {
                                        if (selectionMode) {
                                            viewModel.toggleSelection(video.filePath)
                                        } else {
                                            onNavigateToVideoPlayer(video.uri.toString())
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(video.filePath)
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(video)
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredVideos, key = { it.id }) { video ->
                                val isSelected = selectedItems.contains(video.filePath)
                                val isFavorite = favoritePaths.contains(video.filePath)

                                VideoListRowItem(
                                    video = video,
                                    isSelected = isSelected,
                                    selectionMode = selectionMode,
                                    isFavorite = isFavorite,
                                    onClick = {
                                        if (selectionMode) {
                                            viewModel.toggleSelection(video.filePath)
                                        } else {
                                            onNavigateToVideoPlayer(video.uri.toString())
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(video.filePath)
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(video)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            mediaPaths = selectedItems.toList(),
            viewModel = viewModel,
            onDismiss = {
                showPlaylistDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGridCard(
    video: MediaModel,
    isSelected: Boolean,
    selectionMode: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13182C))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF1E293B))
            ) {
                AsyncImage(
                    model = video.uri,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (selectionMode || isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF38BDF8) else Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isSelected) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (video.duration > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (isFavorite) Color(0xFFFF2A6D) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "فيديو MP4",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListRowItem(
    video: MediaModel,
    isSelected: Boolean,
    selectionMode: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF13182C))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode || isSelected) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(76.dp, 54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = video.uri,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (video.duration > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (video.duration > 0) formatDuration(video.duration) else "فيديو",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "المفضلة",
                tint = if (isFavorite) Color(0xFFFF2A6D) else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
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
