package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
fun MainScreen(
    viewModel: MediaViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToVideoPlayer: (String) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToArtists: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {}
) {
    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    val isNightVibes = themeState.style == com.example.ui.theme.AppUIStyle.NIGHT_VIBES

    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()
    var selectedTab by remember { mutableStateOf(1) } // 0: Videos, 1: Songs, 2: Playlists
    var nightVibesTab by remember { mutableStateOf(0) } // 0: Home, 1: Explore, 2: Library
    
    val context = LocalContext.current
    var showPlaylistDialog by remember { mutableStateOf(false) }

    if (isNightVibes) {
        Scaffold(
            containerColor = Color(0xFF0B0F19),
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    if (selectionMode) {
                        BottomAppBar(
                            containerColor = Color(0xFF161F30),
                            contentColor = Color.White
                        ) {
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
                        NavigationBar(
                            containerColor = Color(0xFF111827),
                            contentColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Home, contentDescription = "الرئيسية") },
                                label = { Text("الرئيسية") },
                                selected = nightVibesTab == 0,
                                onClick = { nightVibesTab = 0 },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF38BDF8),
                                    selectedTextColor = Color(0xFF38BDF8),
                                    indicatorColor = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    unselectedIconColor = Color(0xFF64748B),
                                    unselectedTextColor = Color(0xFF64748B)
                                )
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Explore, contentDescription = "استكشاف") },
                                label = { Text("استكشاف") },
                                selected = nightVibesTab == 1,
                                onClick = { nightVibesTab = 1 },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF38BDF8),
                                    selectedTextColor = Color(0xFF38BDF8),
                                    indicatorColor = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    unselectedIconColor = Color(0xFF64748B),
                                    unselectedTextColor = Color(0xFF64748B)
                                )
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "مكتبتك") },
                                label = { Text("مكتبتك") },
                                selected = nightVibesTab == 2,
                                onClick = { nightVibesTab = 2 },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF38BDF8),
                                    selectedTextColor = Color(0xFF38BDF8),
                                    indicatorColor = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    unselectedIconColor = Color(0xFF64748B),
                                    unselectedTextColor = Color(0xFF64748B)
                                )
                            )
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
                when (nightVibesTab) {
                    0 -> NightVibesHomeScreen(
                        viewModel = viewModel,
                        onNavigateToFavorites = onNavigateToFavorites,
                        onNavigateToPlaylists = {
                            val firstPlaylist = viewModel.playlists.value.firstOrNull()
                            if (firstPlaylist != null) onNavigateToPlaylist(firstPlaylist.id)
                            else nightVibesTab = 2
                        },
                        onNavigateToArtists = onNavigateToArtists,
                        onNavigateToExplore = { nightVibesTab = 1 },
                        onNavigateToPlayer = onNavigateToPlayer,
                        onNavigateToPlaylistDetails = onNavigateToPlaylist,
                        onNavigateToVideos = {
                            val firstVideo = viewModel.videoFiles.value.firstOrNull()
                            if (firstVideo != null) onNavigateToVideoPlayer(firstVideo.filePath)
                            else onNavigateToVideoPlayer("")
                        }
                    )
                    1 -> ExploreScreen(
                        viewModel = viewModel,
                        onNavigateToPlayer = onNavigateToPlayer
                    )
                    2 -> LibraryScreen(
                        viewModel = viewModel,
                        onNavigateToFavorites = onNavigateToFavorites,
                        onNavigateToPlaylists = {
                            val firstPlaylist = viewModel.playlists.value.firstOrNull()
                            if (firstPlaylist != null) onNavigateToPlaylist(firstPlaylist.id)
                            else selectedTab = 2
                        },
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onNavigateToVideos = {
                            val firstVideo = viewModel.videoFiles.value.firstOrNull()
                            if (firstVideo != null) onNavigateToVideoPlayer(firstVideo.filePath)
                            else onNavigateToVideoPlayer("")
                        }
                    )
                }
            }
        }
    } else {
        Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedItems.size} محددة", color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.ArrowForward, "إلغاء", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            } else {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                    var isSearchActive by remember { mutableStateOf(false) }
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    
                    if (isSearchActive) {
                        TopAppBar(
                            title = { 
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("بحث...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                }) { Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground) }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    } else {
                        TopAppBar(
                            title = { Text("Lark Player", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground) },
                            actions = {
                                IconButton(onClick = onNavigateToFavorites) { 
                                    Icon(Icons.Filled.Favorite, "المفضلة", tint = Color(0xFFFF2A6D)) 
                                }
                                IconButton(onClick = onNavigateToSettings) { 
                                    Icon(Icons.Filled.Palette, "السمات والمظهر", tint = MaterialTheme.colorScheme.primary) 
                                }
                                IconButton(onClick = { isSearchActive = true }) { 
                                    Icon(Icons.Filled.Search, "بحث", tint = MaterialTheme.colorScheme.onBackground) 
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    }
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("الفيديوهات") },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("الأغاني") },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("قوائم التشغيل") },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (selectionMode) {
                BottomAppBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
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
                        if (selectedTab == 0) {
                            ActionButton(Icons.Filled.MusicVideo, "تحويل لـ MP3") {
                                val videos = viewModel.rawVideoFiles.value
                                selectedItems.forEach { path ->
                                    videos.find { it.filePath == path }?.let { video ->
                                        viewModel.extractAudioFromVideo(video)
                                    }
                                }
                                viewModel.clearSelection()
                            }
                        }
                    }
                }
            } else {
                MiniPlayer(viewModel, onClick = onNavigateToPlayer)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (selectedTab) {
                0 -> VideoScreen(viewModel, onNavigateToVideoPlayer = onNavigateToVideoPlayer)
                1 -> AudioScreen(viewModel, onNavigateToPlayer)
                2 -> PlaylistsScreen(viewModel, onNavigateToPlaylist = onNavigateToPlaylist)
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
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(8.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
