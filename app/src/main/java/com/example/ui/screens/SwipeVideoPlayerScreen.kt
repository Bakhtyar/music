package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.MediaModel
import com.example.ui.MediaViewModel
import com.example.ui.components.AddToPlaylistDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

enum class VideoPlaybackMode(val title: String) {
    AUTO_PLAY("انتقال تلقائي"),
    SHUFFLE("عشوائي"),
    LOOP("تكرار الفيديو")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SwipeVideoPlayerScreen(
    viewModel: MediaViewModel,
    initialUri: String,
    playlistId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val rawVideos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlistMedia by (if (playlistId != null && playlistId != -1L) viewModel.getPlaylistMediaFiles(playlistId) else remember { kotlinx.coroutines.flow.flowOf(emptyList()) })
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    val baseList = remember(playlistId, playlistMedia, rawVideos, favorites) {
        if (playlistId == -1L) {
            val favPaths = favorites.filter { it.mediaType == "VIDEO" }.map { it.filePath }.toSet()
            rawVideos.filter { it.filePath in favPaths }
        } else if (playlistId != null) {
            playlistMedia.filter { it.type == "VIDEO" }
        } else {
            rawVideos
        }
    }
    
    if (baseList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (playlistId == -1L) Icons.Filled.FavoriteBorder else Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (playlistId == -1L) "لا توجد فيديوهات في المفضلة حالياً" else "لا توجد فيديوهات متاحة",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, "رجوع", tint = Color.White)
            }
        }
        return
    }

    var playbackMode by remember { mutableStateOf(VideoPlaybackMode.AUTO_PLAY) }
    var isFitMode by remember { mutableStateOf(false) } // False = Crop/Zoom (TikTok style), True = Fit
    var videoToAddToPlaylist by remember { mutableStateOf<MediaModel?>(null) }
    var modeFeedbackMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(modeFeedbackMessage) {
        if (modeFeedbackMessage != null) {
            delay(1600)
            modeFeedbackMessage = null
        }
    }
    
    val initialIndex = remember(baseList, initialUri) {
        val idx = baseList.indexOfFirst { it.uri.toString() == initialUri || it.filePath == initialUri }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { baseList.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Vertical Pager (TikTok style swipe)
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { baseList[it].id }
        ) { page ->
            val video = baseList[page]
            val isSelected = pagerState.currentPage == page
            val isFavorite = favorites.any { it.filePath == video.filePath }
            
            TikTokVideoPlayerItem(
                video = video,
                isSelected = isSelected,
                isFavorite = isFavorite,
                isFitMode = isFitMode,
                playbackMode = playbackMode,
                onToggleFavorite = { viewModel.toggleFavorite(video) },
                onExtractAudio = { viewModel.extractAudioFromVideo(video) },
                onAddToPlaylist = { videoToAddToPlaylist = video },
                onToggleFitMode = { isFitMode = !isFitMode },
                onShuffleNext = {
                    if (baseList.size > 1) {
                        scope.launch {
                            var nextRandom = Random.nextInt(baseList.size)
                            if (nextRandom == page) {
                                nextRandom = (page + 1) % baseList.size
                            }
                            pagerState.animateScrollToPage(nextRandom)
                        }
                    }
                },
                onVideoEnded = {
                    scope.launch {
                        when (playbackMode) {
                            VideoPlaybackMode.AUTO_PLAY -> {
                                if (page < baseList.size - 1) {
                                    pagerState.animateScrollToPage(page + 1)
                                } else {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                            VideoPlaybackMode.SHUFFLE -> {
                                if (baseList.size > 1) {
                                    var nextRandom = Random.nextInt(baseList.size)
                                    if (nextRandom == page) {
                                        nextRandom = (page + 1) % baseList.size
                                    }
                                    pagerState.animateScrollToPage(nextRandom)
                                }
                            }
                            VideoPlaybackMode.LOOP -> {
                                // Handled inside player via repeatMode
                            }
                        }
                    }
                }
            )
        }

        // Top Controls Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Filled.ArrowBack, "رجوع", tint = Color.White)
            }

            // Video Counter (e.g. 2 / 10)
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${baseList.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Mode Selector Toggle (تلقائي / عشوائي / تكرار)
            Surface(
                modifier = Modifier.clickable {
                    playbackMode = when (playbackMode) {
                        VideoPlaybackMode.AUTO_PLAY -> VideoPlaybackMode.SHUFFLE
                        VideoPlaybackMode.SHUFFLE -> VideoPlaybackMode.LOOP
                        VideoPlaybackMode.LOOP -> VideoPlaybackMode.AUTO_PLAY
                    }
                    modeFeedbackMessage = "وضع التشغيل: ${playbackMode.title}"
                },
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (playbackMode) {
                        VideoPlaybackMode.AUTO_PLAY -> Icons.Filled.Repeat
                        VideoPlaybackMode.SHUFFLE -> Icons.Filled.Shuffle
                        VideoPlaybackMode.LOOP -> Icons.Filled.RepeatOne
                    }
                    Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        playbackMode.title,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Animated feedback pill when changing modes
        AnimatedVisibility(
            visible = modeFeedbackMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp)
        ) {
            Surface(
                color = Color(0xFF13182C).copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
            ) {
                Text(
                    text = modeFeedbackMessage ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Add to playlist modal bottom sheet
        videoToAddToPlaylist?.let { video ->
            AddToPlaylistDialog(
                viewModel = viewModel,
                mediaPaths = listOf(video.filePath),
                onDismiss = { videoToAddToPlaylist = null }
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun TikTokVideoPlayerItem(
    video: MediaModel,
    isSelected: Boolean,
    isFavorite: Boolean,
    isFitMode: Boolean,
    playbackMode: VideoPlaybackMode,
    onToggleFavorite: () -> Unit,
    onExtractAudio: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFitMode: () -> Unit,
    onShuffleNext: () -> Unit,
    onVideoEnded: () -> Unit
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubRatio by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(900)
            showDoubleTapHeart = false
        }
    }

    DisposableEffect(isSelected) {
        if (isSelected) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(video.uri))
                repeatMode = if (playbackMode == VideoPlaybackMode.LOOP) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                prepare()
                playWhenReady = true
            }
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onVideoEnded()
                    }
                    if (playbackState == Player.STATE_READY) {
                        duration = player.duration.coerceAtLeast(0L)
                    }
                }
            }
            player.addListener(listener)
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

    // Update repeat mode dynamically without resetting video playback
    LaunchedEffect(playbackMode) {
        exoPlayer?.repeatMode = if (playbackMode == VideoPlaybackMode.LOOP) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Periodic position updater for progress bar
    LaunchedEffect(isSelected, isPlaying, isScrubbing) {
        while (isSelected && exoPlayer != null) {
            val player = exoPlayer ?: break
            if (!isScrubbing) {
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
            }
            delay(200)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video View
        exoPlayer?.let { player ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false // Custom TikTok UI controls
                        resizeMode = if (isFitMode) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.resizeMode = if (isFitMode) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                                showPlayPauseIndicator = true
                            },
                            onDoubleTap = {
                                if (!isFavorite) {
                                    onToggleFavorite()
                                }
                                showDoubleTapHeart = true
                            }
                        )
                    }
            )
        }

        // Center Play/Pause animated icon on tap
        AnimatedVisibility(
            visible = showPlayPauseIndicator && !isPlaying,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Animated double-tap heart pop (TikTok style)
        AnimatedVisibility(
            visible = showDoubleTapHeart,
            enter = scaleIn(initialScale = 0.3f) + fadeIn(),
            exit = scaleOut(targetScale = 1.3f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF2A6D),
                modifier = Modifier.size(120.dp)
            )
        }

        // Dark gradient overlay at bottom for clean text legibility and navigation bar tint
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Right side floating action buttons (TikTok style, elevated comfortably above navigation bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Favorite Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        onToggleFavorite()
                        if (!isFavorite) {
                            showDoubleTapHeart = true
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "إعجاب",
                        tint = if (isFavorite) Color(0xFFFF2A6D) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text("إعجاب", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Shuffle Next Video Button (عشوائي)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShuffleNext,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "عشوائي",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("عشوائي", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Convert to MP3
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onExtractAudio,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "تحويل MP3",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text("تحويل MP3", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Add to Playlist
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlaylistAdd,
                        contentDescription = "قائمة",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text("قائمة", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Fit / Crop Zoom mode toggle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleFitMode,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (isFitMode) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit,
                        contentDescription = "توسيع/ملاءمة",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(if (isFitMode) "ملء" else "ملاءمة", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Share Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/*"
                                putExtra(Intent.EXTRA_STREAM, video.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "مشاركة",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("مشاركة", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }

        // Bottom Details (TikTok style, elevated above seekbar and navigation bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .fillMaxWidth(0.72f)
                .padding(start = 16.dp, bottom = 38.dp)
        ) {
            Text(
                text = video.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (video.duration > 0) formatDuration(video.duration) else "فيديو محلي",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Video Scrubber Tooltip & Modern TikTok Expandable Progress Bar
        val currentRatio = if (isScrubbing) scrubRatio else (if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f)
        val barHeight by animateDpAsState(
            targetValue = if (isScrubbing) 6.dp else 3.dp,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "scrubberHeight"
        )
        val thumbAlpha by animateFloatAsState(
            targetValue = if (isScrubbing) 1f else 0f,
            animationSpec = tween(150),
            label = "thumbAlpha"
        )

        // Floating timestamp preview badge during scrub (above the seekbar)
        AnimatedVisibility(
            visible = isScrubbing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 52.dp)
        ) {
            val previewMs = (scrubRatio * duration).toLong().coerceIn(0L, duration)
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.94f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(previewMs),
                        color = Color(0xFF38BDF8),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = " / ",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = formatDuration(duration),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Interactive video seek / extension bar (TikTok style: elevated above navigation buttons with generous touch zone)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(duration) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        
                        if (duration > 0) {
                            isScrubbing = true
                            var currentRatio = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            scrubRatio = currentRatio

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                
                                if (change == null || !change.pressed) {
                                    // Pointer released
                                    val targetMs = (scrubRatio * duration).toLong().coerceIn(0L, duration)
                                    exoPlayer?.seekTo(targetMs)
                                    currentPosition = targetMs
                                    isScrubbing = false
                                    break
                                } else {
                                    change.consume()
                                    currentRatio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    scrubRatio = currentRatio
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = if (isScrubbing) 0.4f else 0.25f))
            ) {
                val fullWidth = maxWidth
                val activeWidth = fullWidth * currentRatio

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(activeWidth)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF), Color(0xFF38BDF8))
                            )
                        )
                )

                if (thumbAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (activeWidth - 7.dp).coerceAtLeast(0.dp))
                            .size(14.dp)
                            .graphicsLayer { alpha = thumbAlpha }
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color(0xFF38BDF8), CircleShape)
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
