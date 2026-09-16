package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.R
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel
import com.example.ui.theme.AppUIStyle
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    onNavigateToPlaylist: ((Long) -> Unit)? = null
) {
    val player = PlayerManager.exoPlayer ?: return
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var currentUri by remember { mutableStateOf(player.currentMediaItem?.localConfiguration?.uri?.toString()) }
    
    var isShuffle by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }

    // Scrubbing / Dragging State
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val media = audios.find { it.uri.toString() == currentUri } ?: audios.firstOrNull()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite = favorites.any { it.filePath == media?.filePath }
    var showMetadataDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    
    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    val customArt by viewModel.customArtState.collectAsStateWithLifecycle()
    val isNightVibes = themeState.style == AppUIStyle.NIGHT_VIBES

    val isMoonlight = media?.title?.contains("Moonlight", ignoreCase = true) == true ||
            media?.album?.contains("Sad", ignoreCase = true) == true
    val coverType = if (isMoonlight) "moonlight" else "night"
    var showCoverCustomizeDialog by remember { mutableStateOf(false) }

    val songCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.saveCustomImage(coverType, uri)
        }
    }

    if (showCoverCustomizeDialog) {
        val currentCustomCover = if (isMoonlight) customArt.moonlightCoverPath else customArt.nightVibesCoverPath
        AlertDialog(
            onDismissRequest = { showCoverCustomizeDialog = false },
            icon = {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "تخصيص غلاف المشغل",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    "يمكنك اختيار صورة مخصصة من معرض جهازك لإطار هذا النمط الموسيقي، أو استعادة الغلاف الافتراضي.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCoverCustomizeDialog = false
                        songCoverPicker.launch(
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
                    if (currentCustomCover != null) {
                        TextButton(
                            onClick = {
                                showCoverCustomizeDialog = false
                                viewModel.resetCustomImage(coverType)
                            }
                        ) {
                            Text("استعادة الافتراضي", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showCoverCustomizeDialog = false }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) { 
                isPlaying = isPlayingNow 
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                }
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                currentUri = mediaItem?.localConfiguration?.uri?.toString()
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying, isDragging) {
        while (isPlaying) {
            if (!isDragging) {
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
            }
            delay(250L)
        }
    }

    val sliderProgress = if (isDragging) {
        dragProgress
    } else {
        if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }
    
    val displayCurrentTime = if (isDragging) {
        (dragProgress * duration).toLong()
    } else {
        currentPosition
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "إغلاق",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "مشغل الموسيقى",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            IconButton(onClick = { showMetadataDialog = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "خيارات",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Album Art Box (Clean, long-press to customize frame/artwork)
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showCoverCustomizeDialog = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val customCoverPath = if (isMoonlight) customArt.moonlightCoverPath else customArt.nightVibesCoverPath
            
            if (media?.coverUri != null && media.coverUri.isNotEmpty()) {
                AsyncImage(
                    model = media.coverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (customCoverPath != null) {
                AsyncImage(
                    model = File(customCoverPath),
                    contentDescription = "Custom Album Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isNightVibes) {
                val coverRes = if (isMoonlight) {
                    R.drawable.img_moonlight_cover
                } else {
                    R.drawable.img_night_vibes_cover
                }
                Image(
                    painter = painterResource(id = coverRes),
                    contentDescription = "Night Vibes Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        // Title and Favorite
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    media?.title ?: "Lost in the Night",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    media?.artist?.ifEmpty { "The Weeknd" } ?: "The Weeknd",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            IconButton(onClick = { media?.let { viewModel.toggleFavorite(it) } }) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "المفضلة",
                    tint = if (isFavorite) Color(0xFFFF2A6D) else MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))

        // Smooth interactive seeker / scrubber bar
        Slider(
            value = sliderProgress,
            onValueChange = { value ->
                isDragging = true
                dragProgress = value
            },
            onValueChangeFinished = {
                val targetMs = (dragProgress * duration).toLong().coerceIn(0L, duration)
                player.seekTo(targetMs)
                currentPosition = targetMs
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(displayCurrentTime),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                formatTime(duration),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))

        if (isNightVibes) {
            // Night Vibes Controls Row (Shuffle - Prev - Play/Pause - Next - Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = {
                    isShuffle = !isShuffle
                    player.shuffleModeEnabled = isShuffle
                }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        "خلط",
                        modifier = Modifier.size(28.dp),
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                // Previous
                IconButton(onClick = { player.seekToPreviousMediaItem() }) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        "السابق",
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Play / Pause FAB (Large Glowing Neon)
                FilledIconButton(
                    onClick = { if (isPlaying) player.pause() else player.play() },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "تشغيل/إيقاف",
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Next
                IconButton(onClick = { player.seekToNextMediaItem() }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        "التالي",
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Repeat
                IconButton(onClick = {
                    repeatMode = when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    player.repeatMode = repeatMode
                }) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        "تكرار",
                        modifier = Modifier.size(28.dp),
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Secondary Actions (Favorite, Queue, Equalizer)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { media?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        "المفضلة",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }

                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.Filled.QueueMusic,
                        "قائمة التشغيل",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }

                IconButton(onClick = onOpenEqualizer) {
                    Icon(
                        Icons.Filled.Tune,
                        "معادل الصوت",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Playlist Pill Card (Matching Screen 2 & 8)
            val playlistTitle = media?.album?.ifEmpty { "Night Vibes" } ?: "Night Vibes"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenQueue() }
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                painter = painterResource(
                                    id = if (playlistTitle.contains("Sad", ignoreCase = true)) R.drawable.img_moonlight_cover else R.drawable.img_night_vibes_cover
                                ),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "قائمة التشغيل",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                playlistTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "عرض",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Classic Lark Player Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Song
                IconButton(onClick = { player.seekToPreviousMediaItem() }) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        "السابق",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Rewind 10 Seconds
                IconButton(onClick = {
                    val newPos = (player.currentPosition - 10000L).coerceAtLeast(0L)
                    player.seekTo(newPos)
                    currentPosition = newPos
                }) {
                    Icon(
                        Icons.Filled.Replay10,
                        "ترجيع 10 ثوانٍ",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }

                // Play / Pause FAB
                FilledIconButton(
                    onClick = { if (isPlaying) player.pause() else player.play() },
                    modifier = Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "تشغيل/إيقاف",
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Forward 10 Seconds
                IconButton(onClick = {
                    val newPos = (player.currentPosition + 10000L).coerceAtMost(duration)
                    player.seekTo(newPos)
                    currentPosition = newPos
                }) {
                    Icon(
                        Icons.Filled.Forward10,
                        "تقديم 10 ثوانٍ",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }

                // Next Song
                IconButton(onClick = { player.seekToNextMediaItem() }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        "التالي",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Bottom utility actions (Equalizer, Lyrics, Queue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenEqualizer) {
                    Icon(
                        Icons.Filled.Tune,
                        "معادل الصوت",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                ElevatedButton(
                    onClick = { showLyricsDialog = true },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, "الكلمات", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الكلمات")
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.Filled.QueueMusic,
                        "قائمة الانتظار",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
    
    if (showMetadataDialog && media != null) {
        EditMetadataDialog(media, viewModel) { showMetadataDialog = false }
    }
    
    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsDialog = false },
            title = { Text("الكلمات") },
            text = { Text("لم يتم العثور على كلمات مدمجة لهذه الأغنية.") },
            confirmButton = { TextButton(onClick = { showLyricsDialog = false }) { Text("حسناً") } }
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
