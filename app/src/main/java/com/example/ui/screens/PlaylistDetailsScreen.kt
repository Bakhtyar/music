package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.example.data.MediaModel
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    playlistId: Long,
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToVideoPlayer: (String, Long?) -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlist = playlists.find { it.id == playlistId }
    
    val mediaFlow = remember(playlistId) { viewModel.getPlaylistMediaFiles(playlistId) }
    val playlistMedia by mediaFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showCoverDialog by remember { mutableStateOf(false) }

    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    val customArt by viewModel.customArtState.collectAsStateWithLifecycle()
    val customCoverPath = customArt.playlistCovers[playlistId]
    val isNightVibes = themeState.style == com.example.ui.theme.AppUIStyle.NIGHT_VIBES

    val playlistCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.savePlaylistCover(playlistId, uri)
        }
    }

    val audioCount = playlistMedia.count { it.type == "AUDIO" }
    val videoCount = playlistMedia.count { it.type == "VIDEO" }

    if (showCoverDialog) {
        AlertDialog(
            onDismissRequest = { showCoverDialog = false },
            icon = {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("تخصيص غلاف قائمة التشغيل", style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Text(
                    "يمكنك اختيار صورة من معرض جهازك لتعيينها كغلاف مخصص لقائمة التشغيل هذه، أو استعادة الغلاف الافتراضي.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCoverDialog = false
                        playlistCoverPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختيار من المعرض")
                }
            },
            dismissButton = {
                Row {
                    if (customCoverPath != null) {
                        TextButton(
                            onClick = {
                                showCoverDialog = false
                                viewModel.resetPlaylistCover(playlistId)
                            }
                        ) {
                            Text("استعادة الافتراضي", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showCoverDialog = false }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(playlist?.name ?: "قائمة التشغيل", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = buildString {
                                if (playlistMedia.isEmpty()) append("فارغة")
                                else {
                                    val parts = mutableListOf<String>()
                                    if (audioCount > 0) parts.add("$audioCount أغانٍ")
                                    if (videoCount > 0) parts.add("$videoCount فيديو")
                                    append(parts.joinToString(" • "))
                                }
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSongsDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Filled.Add, "إضافة وسائط", tint = Color.White)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            if (playlistMedia.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.QueueMusic, null, tint = Color.Gray, modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("قائمة التشغيل فارغة", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("اضغط على زر الإضافة لاختيار الأغاني أو الفيديوهات", color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showAddSongsDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة أغانٍ وفيديوهات")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (isNightVibes) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                        .pointerInput(playlistId) {
                                            detectTapGestures(
                                                onLongPress = { showCoverDialog = true },
                                                onTap = { showCoverDialog = true }
                                            )
                                        }
                                ) {
                                    if (customCoverPath != null) {
                                        AsyncImage(
                                            model = File(customCoverPath),
                                            contentDescription = "غلاف قائمة التشغيل",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(
                                                id = if (playlist?.name?.contains("Sad", ignoreCase = true) == true) com.example.R.drawable.img_moonlight_cover
                                                else com.example.R.drawable.img_night_vibes_cover
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    playlist?.name ?: "Night Vibes",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "قائمة تشغيل • ${playlistMedia.size} أغنية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "أغاني تناسب أجواء الليل والهدوء والتركيز...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Quick Action Buttons (Play all & Shuffle)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val audiosInPlaylist = playlistMedia.filter { it.type == "AUDIO" }
                                    if (audiosInPlaylist.isNotEmpty()) {
                                        PlayerManager.exoPlayer?.let { player ->
                                            player.setMediaItems(audiosInPlaylist.map { MediaItem.fromUri(it.uri) })
                                            player.prepare()
                                            player.play()
                                        }
                                        onNavigateToPlayer()
                                    } else if (playlistMedia.isNotEmpty()) {
                                        val firstVideo = playlistMedia.firstOrNull { it.type == "VIDEO" }
                                        if (firstVideo != null) {
                                            onNavigateToVideoPlayer(firstVideo.filePath, playlistId)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تشغيل الكل")
                            }

                            OutlinedButton(
                                onClick = {
                                    val audiosInPlaylist = playlistMedia.filter { it.type == "AUDIO" }.shuffled()
                                    if (audiosInPlaylist.isNotEmpty()) {
                                        PlayerManager.exoPlayer?.let { player ->
                                            player.setMediaItems(audiosInPlaylist.map { MediaItem.fromUri(it.uri) })
                                            player.prepare()
                                            player.play()
                                        }
                                        onNavigateToPlayer()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("خلط")
                            }
                        }
                    }

                    items(playlistMedia, key = { it.filePath }) { media ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (media.type == "AUDIO") {
                                        val audiosInPlaylist = playlistMedia.filter { it.type == "AUDIO" }
                                        val index = audiosInPlaylist.indexOf(media).coerceAtLeast(0)
                                        PlayerManager.exoPlayer?.let { player ->
                                            player.setMediaItems(audiosInPlaylist.map { MediaItem.fromUri(it.uri) })
                                            player.seekTo(index, 0L)
                                            player.prepare()
                                            player.play()
                                        }
                                        onNavigateToPlayer()
                                    } else {
                                        onNavigateToVideoPlayer(media.filePath, playlistId)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2C1E30)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (media.type == "VIDEO") {
                                        AsyncImage(
                                            model = media.uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
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

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        media.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1
                                    )
                                    Text(
                                        if (media.type == "VIDEO") "فيديو" else (if (media.artist.isNotBlank()) media.artist else "أغنية"),
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                IconButton(onClick = {
                                    viewModel.removeMediaFromPlaylist(playlistId, media.filePath)
                                }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "إزالة من القائمة",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }
    
    if (showAddSongsDialog) {
        AddMediaToPlaylistDialog(
            playlistId = playlistId,
            viewModel = viewModel,
            onDismiss = { showAddSongsDialog = false }
        )
    }
}

@Composable
fun AddMediaToPlaylistDialog(playlistId: Long, viewModel: MediaViewModel, onDismiss: () -> Unit) {
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val videos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val mediaFlow = remember(playlistId) { viewModel.getPlaylistMediaFiles(playlistId) }
    val alreadyInPlaylist by mediaFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, AUDIO, VIDEO
    val selectedPaths = remember { mutableStateListOf<String>() }

    val alreadyInPaths = remember(alreadyInPlaylist) {
        alreadyInPlaylist.map { it.filePath }.toSet()
    }

    val availableMedia = remember(audios, videos, alreadyInPaths) {
        (audios + videos).filter { it.filePath !in alreadyInPaths }
    }

    val filteredList = remember(availableMedia, searchQuery, selectedFilter) {
        availableMedia.filter { item ->
            val matchesType = when (selectedFilter) {
                "AUDIO" -> item.type == "AUDIO"
                "VIDEO" -> item.type == "VIDEO"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.artist.contains(searchQuery, ignoreCase = true)

            matchesType && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("إضافة وسائط للقائمة", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن اسم الأغنية أو الفيديو...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Filter Tabs (الكل / أغاني / فيديوهات)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("الكل") }
                    )
                    FilterChip(
                        selected = selectedFilter == "AUDIO",
                        onClick = { selectedFilter = "AUDIO" },
                        label = { Text("أغاني (${audios.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "VIDEO",
                        onClick = { selectedFilter = "VIDEO" },
                        label = { Text("فيديوهات (${videos.size})") }
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                // Select All toggle row
                if (filteredList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredList.size} نتيجة متاحة",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = {
                                val allCurrentPaths = filteredList.map { it.filePath }
                                if (selectedPaths.containsAll(allCurrentPaths)) {
                                    selectedPaths.removeAll(allCurrentPaths)
                                } else {
                                    allCurrentPaths.forEach {
                                        if (it !in selectedPaths) selectedPaths.add(it)
                                    }
                                }
                            }
                        ) {
                            Text(
                                if (filteredList.all { it.filePath in selectedPaths }) "إلغاء التحديد"
                                else "تحديد الكل"
                            )
                        }
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isBlank()) "لا توجد ملفات جديدة للإضافة"
                            else "لا توجد نتائج مطابقة لبحثك",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filteredList, key = { it.filePath }) { media ->
                            val isSelected = selectedPaths.contains(media.filePath)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isSelected) selectedPaths.remove(media.filePath)
                                        else selectedPaths.add(media.filePath)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (media.type == "VIDEO") Color(0xFF3B2840) else Color(0xFF2B1D30)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (media.type == "VIDEO") Icons.Filled.Movie else Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = if (media.type == "VIDEO") Color(0xFF90CAF9) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(media.title, color = Color.White, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (media.type == "VIDEO") "فيديو" else (if (media.artist.isNotBlank()) media.artist else "أغنية"),
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedPaths.forEach { path ->
                        viewModel.addMediaToPlaylist(playlistId, path)
                    }
                    onDismiss()
                },
                enabled = selectedPaths.isNotEmpty()
            ) {
                Text("إضافة (${selectedPaths.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
