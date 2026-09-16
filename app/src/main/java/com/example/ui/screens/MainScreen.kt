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
                2 -> PlaylistsScreen(viewModel, onNavigateToPlaylist = {})
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
