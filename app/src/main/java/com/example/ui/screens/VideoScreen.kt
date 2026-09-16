package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.MediaModel
import com.example.ui.MediaViewModel
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(viewModel: MediaViewModel) {
    val videos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val isShuffle by viewModel.videoShuffleMode.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    val displayVideos = remember(videos, isShuffle) {
        if (isShuffle) videos.shuffled() else videos
    }

    if (displayVideos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No videos found on device")
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { displayVideos.size })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { displayVideos[it].id }
        ) { page ->
            val video = displayVideos[page]
            val isSelected = pagerState.currentPage == page
            val isFavorite = favorites.any { it.filePath == video.filePath }
            
            VideoPlayerItem(
                video = video,
                isSelected = isSelected,
                isFavorite = isFavorite,
                onToggleFavorite = { viewModel.toggleFavorite(video) },
                onExtractAudio = { viewModel.extractAudioFromVideo(video) },
                onVideoEnded = {
                    scope.launch {
                        if (page < displayVideos.size - 1) {
                            pagerState.animateScrollToPage(page + 1)
                        } else {
                            pagerState.animateScrollToPage(0)
                        }
                    }
                }
            )
        }
        
        // Overlay Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setShuffle("video", !isShuffle) },
                modifier = Modifier.testTag("shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerItem(
    video: MediaModel,
    isSelected: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onExtractAudio: () -> Unit,
    onVideoEnded: () -> Unit
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    
    DisposableEffect(isSelected) {
        if (isSelected) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(video.uri))
                repeatMode = Player.REPEAT_MODE_OFF
                prepare()
                playWhenReady = true
            }
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onVideoEnded()
                    }
                }
            })
            exoPlayer = player
        } else {
            exoPlayer?.release()
            exoPlayer = null
        }

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        exoPlayer?.let { player ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Right side action buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Red else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            IconButton(onClick = onExtractAudio) {
                Icon(
                    imageVector = Icons.Filled.MusicVideo,
                    contentDescription = "Extract Audio to MP3",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Title at bottom
        Text(
            text = video.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(bottom = 64.dp)
        )
    }
}
