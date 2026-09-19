package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
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
import com.example.ui.components.TikTokCarouselDotsIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

enum class VideoPlaybackMode(val title: String) {
    AUTO_PLAY("انتقال تلقائي"),
    SHUFFLE("عشوائي"),
    LOOP("تكرار الفيديو")
}

/**
 * مدير التشغيل العشوائي الذكي والعادل للفيديوهات:
 * - يضمن إعطاء كل فيديو حقه في الظهور داخل كل دورة دون استثناء أو تجاهل أي فيديو (Fair Deck Shuffle).
 * - يمنع تكرار نفس الفيديو بشكل متتالي ومباشر.
 * - يتيح إعادة ظهور الفيديوهات بشكل عشوائي مرن وطبيعي بعد عدة فيديوهات (مثل 2 أو 3 أو أكثر).
 */
class SmartVideoShuffleManager(private val totalItemsProvider: () -> Int) {
    private val unplayedDeck = mutableListOf<Int>()
    private val recentHistory = ArrayDeque<Int>()

    fun onPageVisited(page: Int) {
        val total = totalItemsProvider()
        if (total <= 1) return

        // إزالة الفيديو من قائمة غير المشغل في الدورة الحالية
        unplayedDeck.remove(page)

        if (recentHistory.lastOrNull() != page) {
            recentHistory.addLast(page)
            if (recentHistory.size > 30) {
                recentHistory.removeFirst()
            }
        }
    }

    fun getNextShuffleIndex(currentPage: Int): Int {
        val total = totalItemsProvider()
        if (total <= 1) return 0

        // إذا انتهت الدورة أو بقي فقط الفيديو الحالي، نبدأ دورة جديدة تضم كل الفيديوهات
        if (unplayedDeck.isEmpty() || (unplayedDeck.size == 1 && unplayedDeck[0] == currentPage)) {
            val all = (0 until total).toMutableList()
            all.shuffle()

            // منع ظهور نفس الفيديو الحالي كأول فيديو في الدورة الجديدة
            if (all.size > 1 && all[0] == currentPage) {
                val swapIndex = 1 + Random.nextInt(all.size - 1)
                val tmp = all[0]
                all[0] = all[swapIndex]
                all[swapIndex] = tmp
            }

            // إذا كان هناك أكثر من 3 فيديوهات، تجنب تكرار آخر فيديو تم تشغيله فوراً
            if (all.size >= 4 && recentHistory.isNotEmpty()) {
                val lastPlayed = recentHistory.lastOrNull()
                if (all[0] == lastPlayed && all.size > 2) {
                    val swapIndex = 2 + Random.nextInt(all.size - 2)
                    val tmp = all[0]
                    all[0] = all[swapIndex]
                    all[swapIndex] = tmp
                }
            }

            unplayedDeck.clear()
            unplayedDeck.addAll(all)
        }

        var next = unplayedDeck.removeAt(0)
        // في حال كان العنصر مساوياً للفيديو الحالي ووجود بدائل، نبدله ونعيده لاحقاً في الدورة
        if (next == currentPage && unplayedDeck.isNotEmpty()) {
            val alt = unplayedDeck.removeAt(0)
            unplayedDeck.add(next)
            next = alt
        }

        if (recentHistory.lastOrNull() != next) {
            recentHistory.addLast(next)
            if (recentHistory.size > 30) {
                recentHistory.removeFirst()
            }
        }

        return next
    }

    fun reset() {
        unplayedDeck.clear()
        recentHistory.clear()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SwipeVideoPlayerScreen(
    viewModel: MediaViewModel,
    initialUri: String,
    playlistId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val customList by viewModel.customVideoPlaybackList.collectAsStateWithLifecycle()
    val rawVideos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlistMedia by (if (playlistId != null && playlistId != -1L) viewModel.getPlaylistMediaFiles(playlistId) else remember { kotlinx.coroutines.flow.flowOf(emptyList()) })
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setCustomVideoList(emptyList())
        }
    }

    val baseList = remember(customList, playlistId, playlistMedia, rawVideos, favorites) {
        if (customList.isNotEmpty()) {
            customList
        } else if (playlistId == -1L) {
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
    var isCleanScreenMode by remember { mutableStateOf(false) } // شاشة خالية / إخفاء الأزرار
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

    val shuffleManager = remember(baseList) {
        SmartVideoShuffleManager(totalItemsProvider = { baseList.size })
    }

    LaunchedEffect(pagerState.currentPage) {
        shuffleManager.onPageVisited(pagerState.currentPage)
    }

    DisposableEffect(Unit) {
        com.example.player.PlayerManager.exoPlayer?.pause()
        onDispose { }
    }

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
            
            if (video.isPhotoPost) {
                TikTokPhotoPostItem(
                    video = video,
                    isSelected = isSelected,
                    isFavorite = isFavorite,
                    isCleanScreenMode = isCleanScreenMode,
                    playbackMode = playbackMode,
                    onToggleFavorite = { viewModel.toggleFavorite(video) },
                    onAddToPlaylist = { videoToAddToPlaylist = video },
                    onToggleCleanScreen = {
                        isCleanScreenMode = !isCleanScreenMode
                        modeFeedbackMessage = if (isCleanScreenMode) "تم إخفاء الأزرار • انقر مرتين للرجوع" else "تم إظهار الأزرار"
                    },
                    onExitCleanScreen = {
                        isCleanScreenMode = false
                        modeFeedbackMessage = "تم إظهار الأزرار"
                    },
                    onShuffleNext = {
                        if (baseList.size > 1) {
                            scope.launch {
                                val nextShuffleIndex = shuffleManager.getNextShuffleIndex(pagerState.currentPage)
                                pagerState.animateScrollToPage(nextShuffleIndex)
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
                                        val nextShuffleIndex = shuffleManager.getNextShuffleIndex(pagerState.currentPage)
                                        pagerState.animateScrollToPage(nextShuffleIndex)
                                    }
                                }
                                VideoPlaybackMode.LOOP -> {
                                    // Handled inside player
                                }
                            }
                        }
                    }
                )
            } else {
                TikTokVideoPlayerItem(
                    video = video,
                    isSelected = isSelected,
                    isFavorite = isFavorite,
                    isCleanScreenMode = isCleanScreenMode,
                    playbackMode = playbackMode,
                    onToggleFavorite = { viewModel.toggleFavorite(video) },
                    onExtractAudio = { viewModel.extractAudioFromVideo(video) },
                    onAddToPlaylist = { videoToAddToPlaylist = video },
                    onToggleCleanScreen = {
                        isCleanScreenMode = !isCleanScreenMode
                        modeFeedbackMessage = if (isCleanScreenMode) "تم إخفاء الأزرار • انقر مرتين للرجوع" else "تم إظهار الأزرار"
                    },
                    onExitCleanScreen = {
                        isCleanScreenMode = false
                        modeFeedbackMessage = "تم إظهار الأزرار"
                    },
                    onShuffleNext = {
                        if (baseList.size > 1) {
                            scope.launch {
                                val nextShuffleIndex = shuffleManager.getNextShuffleIndex(pagerState.currentPage)
                                pagerState.animateScrollToPage(nextShuffleIndex)
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
                                        val nextShuffleIndex = shuffleManager.getNextShuffleIndex(pagerState.currentPage)
                                        pagerState.animateScrollToPage(nextShuffleIndex)
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
        }

        // Top Controls Overlay (Hidden when clean screen mode is active)
        AnimatedVisibility(
            visible = !isCleanScreenMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
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

/**
 * حاوية تفاعلية تدعم التكبير والتصغير بإصبعين (Pinch to Zoom)
 * مع سحب وتحريك العنصر في جميع الاتجاهات (فوق/تحت/يمين/يسار) في نفس الوقت.
 * وعند رفع اليدين تعود الشاشة والمحتوى تلقائياً وبسلاسة إلى الحجم والوضع الطبيعي الأصلي.
 * يستمر تشغيل الفيديو أو الصوت في الخلفية دون أي تقطيع أو إعادة تحميل.
 */
@Composable
fun PinchZoomPanContainer(
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                translationX = offsetXAnim.value
                translationY = offsetYAnim.value
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    var isZooming = false
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount >= 2) {
                            isZooming = true
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            event.changes.forEach { it.consume() }

                            coroutineScope.launch {
                                val newScale = (scaleAnim.value * zoomChange).coerceIn(0.6f, 6.0f)
                                scaleAnim.snapTo(newScale)
                                offsetXAnim.snapTo(offsetXAnim.value + panChange.x)
                                offsetYAnim.snapTo(offsetYAnim.value + panChange.y)
                            }
                        } else if (pointerCount == 1 && isZooming) {
                            val panChange = event.calculatePan()
                            event.changes.forEach { it.consume() }
                            coroutineScope.launch {
                                offsetXAnim.snapTo(offsetXAnim.value + panChange.x)
                                offsetYAnim.snapTo(offsetYAnim.value + panChange.y)
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (isZooming || scaleAnim.value != 1f || offsetXAnim.value != 0f || offsetYAnim.value != 0f) {
                        coroutineScope.launch {
                            launch {
                                scaleAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                            launch {
                                offsetXAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                            launch {
                                offsetYAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
    ) {
        content()
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun TikTokVideoPlayerItem(
    video: MediaModel,
    isSelected: Boolean,
    isFavorite: Boolean,
    isCleanScreenMode: Boolean,
    playbackMode: VideoPlaybackMode,
    onToggleFavorite: () -> Unit,
    onExtractAudio: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleCleanScreen: () -> Unit,
    onExitCleanScreen: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Pinch-to-zoom and pan container for the video and thumbnail
        PinchZoomPanContainer(
            modifier = Modifier.fillMaxSize(),
            onTap = {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                    showPlayPauseIndicator = true
                }
            },
            onDoubleTap = {
                if (isCleanScreenMode) {
                    onExitCleanScreen()
                } else {
                    if (!isFavorite) {
                        onToggleFavorite()
                    }
                    showDoubleTapHeart = true
                }
            }
        ) {
            // Instant thumbnail poster backdrop (avoids any black screen or jank)
            AsyncImage(
                model = video.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Video View (Always FIT, zoomable via pinch gestures)
            exoPlayer?.let { player ->
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false // Custom TikTok UI controls
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
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

        // Overlays: hidden completely in Clean Screen Mode
        AnimatedVisibility(
            visible = !isCleanScreenMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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

            // Clean Screen / Hide Buttons toggle ("شاشة خالية")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleCleanScreen,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = "شاشة خالية",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("شاشة خالية", color = Color.White, style = MaterialTheme.typography.labelSmall)
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

        // Interactive video seek / extension bar (TikTok RTL style: 0% at Right -> 100% at Left)
        // Dragging towards the left moves forward, dragging towards the right rewinds
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                                // In RTL: right edge (x near width) is 0%, left edge (x near 0) is 100%
                                var currentRatio = (1f - (down.position.x / size.width.toFloat())).coerceIn(0f, 1f)
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
                                        currentRatio = (1f - (change.position.x / size.width.toFloat())).coerceIn(0f, 1f)
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
                            .align(Alignment.CenterStart) // In RTL, CenterStart is the RIGHT edge
                            .fillMaxHeight()
                            .width(activeWidth)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF38BDF8), Color(0xFF00C6FF), Color(0xFF0072FF))
                                )
                            )
                    )

                    if (thumbAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart) // In RTL, CenterStart is the RIGHT edge
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
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * TikTok-style Photo Post item.
 * - Horizontal swipe for multiple images.
 * - Auto-transitions through images based on audio duration divided by image count.
 * - Shows dots indicator at bottom (capped at 8 dots max).
 * - Shows photo counter at top-left (e.g., 1/4, 2/4).
 * - Tap anywhere to pause/play audio.
 * - Double-tap to heart/favorite.
 * - Moves vertically to next item when audio ends.
 */
@Composable
fun TikTokPhotoPostItem(
    video: MediaModel,
    isSelected: Boolean,
    isFavorite: Boolean,
    isCleanScreenMode: Boolean,
    playbackMode: VideoPlaybackMode,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleCleanScreen: () -> Unit,
    onExitCleanScreen: () -> Unit,
    onShuffleNext: () -> Unit,
    onVideoEnded: () -> Unit
) {
    val context = LocalContext.current
    val photoCount = video.photoPaths.size
    val photoPagerState = rememberPagerState(initialPage = 0, pageCount = { photoCount })
    val scope = rememberCoroutineScope()

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var audioDuration by remember { mutableLongStateOf(video.duration.coerceAtLeast(0L)) }

    // Reset photo pager to first photo when post is selected
    LaunchedEffect(isSelected) {
        if (isSelected && photoCount > 0) {
            photoPagerState.scrollToPage(0)
        }
    }

    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(900)
            showDoubleTapHeart = false
        }
    }

    // Audio Playback
    DisposableEffect(isSelected) {
        if (isSelected) {
            val player = ExoPlayer.Builder(context).build().apply {
                val audioUri = if (video.audioPath.isNotBlank()) {
                    if (video.audioPath.startsWith("content://") || video.audioPath.startsWith("file://")) {
                        Uri.parse(video.audioPath)
                    } else {
                        Uri.fromFile(File(video.audioPath))
                    }
                } else null

                if (audioUri != null) {
                    setMediaItem(MediaItem.fromUri(audioUri))
                    repeatMode = if (playbackMode == VideoPlaybackMode.LOOP) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    prepare()
                    playWhenReady = true
                }
            }

            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        if (player.duration > 0) {
                            audioDuration = player.duration
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        if (playbackMode == VideoPlaybackMode.LOOP) {
                            player.seekTo(0)
                            player.play()
                            scope.launch {
                                if (photoCount > 0) {
                                    photoPagerState.scrollToPage(0)
                                }
                            }
                        } else {
                            onVideoEnded()
                        }
                    }
                }
            }
            player.addListener(listener)
            exoPlayer = player

            onDispose {
                player.removeListener(listener)
                player.release()
                exoPlayer = null
            }
        } else {
            onDispose { }
        }
    }

    // Auto-advance photos based on song duration divided by photo count
    val effectiveDuration = if (audioDuration > 0) audioDuration else (video.duration.takeIf { it > 0 } ?: 30_000L)
    val slideIntervalMs = if (photoCount > 0) {
        (effectiveDuration / photoCount).coerceIn(2000L, 25000L)
    } else 5000L

    LaunchedEffect(isSelected, isPlaying, playbackMode, slideIntervalMs, photoCount) {
        if (!isSelected || !isPlaying || photoCount <= 1) return@LaunchedEffect
        if (playbackMode != VideoPlaybackMode.AUTO_PLAY && playbackMode != VideoPlaybackMode.SHUFFLE && playbackMode != VideoPlaybackMode.LOOP) return@LaunchedEffect

        while (true) {
            delay(slideIntervalMs)
            if (!isPlaying) break
            if (photoPagerState.currentPage < photoCount - 1) {
                photoPagerState.animateScrollToPage(
                    photoPagerState.currentPage + 1,
                    animationSpec = tween(durationMillis = 400)
                )
            } else if (playbackMode == VideoPlaybackMode.LOOP) {
                photoPagerState.animateScrollToPage(0, animationSpec = tween(durationMillis = 400))
            } else {
                // Reached last photo; keep showing it until the song finishes
                break
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Pinch-to-zoom and pan container for photos
        PinchZoomPanContainer(
            modifier = Modifier.fillMaxSize(),
            onTap = {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                    showPlayPauseIndicator = true
                }
            },
            onDoubleTap = {
                if (isCleanScreenMode) {
                    onExitCleanScreen()
                } else {
                    if (!isFavorite) {
                        onToggleFavorite()
                    }
                    showDoubleTapHeart = true
                }
            }
        ) {
            // Horizontal Pager for Photos
            HorizontalPager(
                state = photoPagerState,
                modifier = Modifier.fillMaxSize()
            ) { photoIdx ->
                val path = video.photoPaths.getOrNull(photoIdx) ?: ""
                val imageModel = if (path.startsWith("content://") || path.startsWith("file://")) {
                    Uri.parse(path)
                } else {
                    File(path)
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = "صورة ${photoIdx + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Overlays: hidden completely in Clean Screen Mode
        AnimatedVisibility(
            visible = !isCleanScreenMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Left: Photo Counter e.g. "1/4", "2/4"
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 60.dp, start = 16.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                ) {
            Text(
                text = "${photoPagerState.currentPage + 1}/${video.photoPaths.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Bottom Dots Indicator (Capped at 8 dots max)
        if (photoCount > 1) {
            TikTokCarouselDotsIndicator(
                totalCount = photoCount,
                currentIndex = photoPagerState.currentPage,
                maxVisibleDots = 8,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 90.dp)
            )
        }

        // Center Play/Pause Indicator on single tap
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

        // Animated double-tap heart pop
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

        // Dark gradient overlay at bottom for clean text legibility
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

        // Right side floating action buttons
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

            // Shuffle Next Video / Photo Post Button (عشوائي)
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

            // Clean Screen / Hide Buttons toggle ("شاشة خالية")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleCleanScreen,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = "شاشة خالية",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("شاشة خالية", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }

        // Bottom Left/Start Info: Title and Audio name badge
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 44.dp, end = 80.dp)
        ) {
            // Photo post tag
            Surface(
                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("منشور صور", color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Music Disc / Audio Ticker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFFF2A6D),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = video.artist.ifBlank { "موسيقى تيك توك" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
            }
        }
    }
}
