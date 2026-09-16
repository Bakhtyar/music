package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MediaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: MediaViewModel) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val audioFiles by viewModel.rawAudioFiles.collectAsStateWithLifecycle()
    val videoFiles by viewModel.rawVideoFiles.collectAsStateWithLifecycle()
    
    val favoriteAudios = favorites.filter { it.mediaType == "AUDIO" }.mapNotNull { fav -> audioFiles.find { it.filePath == fav.filePath } }
    val favoriteVideos = favorites.filter { it.mediaType == "VIDEO" }.mapNotNull { fav -> videoFiles.find { it.filePath == fav.filePath } }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Favorites") })
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Songs") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Videos") })
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedTab == 0) {
                if (favoriteAudios.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No favorite songs.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(favoriteAudios, key = { it.id }) { media ->
                            ListItem(
                                headlineContent = { Text(media.title) },
                                supportingContent = { Text(if (media.artist.isNotBlank()) media.artist else "Unknown Artist") },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.toggleFavorite(media) }) {
                                        Icon(Icons.Filled.Favorite, contentDescription = "Remove", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                if (favoriteVideos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No favorite videos.")
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { favoriteVideos.size })
                    val scope = rememberCoroutineScope()
                    
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { favoriteVideos[it].id }
                        ) { page ->
                            val video = favoriteVideos[page]
                            val isSelected = pagerState.currentPage == page
                            
                            VideoPlayerItem(
                                video = video,
                                isSelected = isSelected,
                                isFavorite = true,
                                onToggleFavorite = { viewModel.toggleFavorite(video) },
                                onExtractAudio = { viewModel.extractAudioFromVideo(video) },
                                onVideoEnded = {
                                    scope.launch {
                                        if (page < favoriteVideos.size - 1) {
                                            pagerState.animateScrollToPage(page + 1)
                                        } else {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
