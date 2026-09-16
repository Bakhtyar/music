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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.example.R
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel
import java.io.File

data class RecommendedPlaylist(
    val title: String,
    val subtitle: String,
    val coverResId: Int,
    val customImagePath: String? = null,
    val gradient: List<Color>
)

@Composable
fun NightVibesHomeScreen(
    viewModel: MediaViewModel,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylistDetails: (Long) -> Unit,
    onNavigateToVideos: () -> Unit = {}
) {
    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    val videos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val customArt by viewModel.customArtState.collectAsStateWithLifecycle()

    var showHeroCustomizeDialog by remember { mutableStateOf(false) }
    var playlistToCustomize by remember { mutableStateOf<String?>(null) } // "night", "moonlight", "hero"

    val heroImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val target = playlistToCustomize ?: "hero"
            viewModel.saveCustomImage(target, uri)
            playlistToCustomize = null
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val recommended = remember(customArt) {
        listOf(
            RecommendedPlaylist(
                "Night Drive",
                "Playlist",
                R.drawable.img_night_vibes_cover,
                customArt.nightVibesCoverPath,
                listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            ),
            RecommendedPlaylist(
                "Broken Hearts",
                "Playlist",
                R.drawable.img_moonlight_cover,
                customArt.moonlightCoverPath,
                listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))
            ),
            RecommendedPlaylist(
                "Chill Vibes",
                "Playlist",
                R.drawable.img_night_anime_hero,
                customArt.heroImagePath,
                listOf(Color(0xFF141E30), Color(0xFF243B55))
            )
        )
    }

    // Hero banner customization dialog triggered only on long-press
    if (showHeroCustomizeDialog) {
        AlertDialog(
            onDismissRequest = { showHeroCustomizeDialog = false },
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
                    "تخصيص صورة الواجهة (البانر)",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    "يمكنك اختيار صورة من معرض جهازك لتظهر كخلفية للواجهة الرئيسية، أو استعادة صورة الأنمي الأصلية.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHeroCustomizeDialog = false
                        playlistToCustomize = "hero"
                        heroImagePicker.launch(
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
                    if (customArt.heroImagePath != null) {
                        TextButton(
                            onClick = {
                                showHeroCustomizeDialog = false
                                viewModel.resetCustomImage("hero")
                            }
                        ) {
                            Text("استعادة الأصلية", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showHeroCustomizeDialog = false }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Hero Anime Header (Clean, sleek, long-press to customize)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showHeroCustomizeDialog = true
                            }
                        )
                    }
            ) {
                if (customArt.heroImagePath != null) {
                    AsyncImage(
                        model = File(customArt.heroImagePath!!),
                        contentDescription = "Night Vibes Hero",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_night_anime_hero),
                        contentDescription = "Night Vibes Hero",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Dark gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color(0xFF060913).copy(alpha = 0.6f),
                                    Color(0xFF060913)
                                )
                            )
                        )
                )

                // Greeting text on the side
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "مرحبا",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "استمع لما تحب",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 2. Search Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.setSearchQuery(it)
                    },
                    placeholder = {
                        Text(
                            "ابحث عن أغاني، فنانين، أو ألبومات",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "بحث",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Quick Category Icons Row (المفضلة, فيديوهات تيك توك, قوائم التشغيل, الفنانين, الأنواع)
        item {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    CategoryPillItem(
                        icon = Icons.Filled.Favorite,
                        label = "المفضلة",
                        iconColor = Color(0xFFFF2A6D),
                        onClick = onNavigateToFavorites
                    )
                }
                item {
                    CategoryPillItem(
                        icon = Icons.Filled.VideoLibrary,
                        label = "فيديوهات تيك توك",
                        iconColor = Color(0xFF00E5FF),
                        onClick = onNavigateToVideos
                    )
                }
                item {
                    CategoryPillItem(
                        icon = Icons.Filled.QueueMusic,
                        label = "قوائم التشغيل",
                        iconColor = Color(0xFF00B0FF),
                        onClick = onNavigateToPlaylists
                    )
                }
                item {
                    CategoryPillItem(
                        icon = Icons.Filled.Headphones,
                        label = "الفنانين",
                        iconColor = Color(0xFF00E676),
                        onClick = onNavigateToArtists
                    )
                }
                item {
                    CategoryPillItem(
                        icon = Icons.Filled.MusicNote,
                        label = "الأنواع",
                        iconColor = Color(0xFFBA68C8),
                        onClick = onNavigateToExplore
                    )
                }
            }
        }

        // TikTok Short Videos Spotlight Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF161F38),
                                Color(0xFF1E1032),
                                Color(0xFF0A2239)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onNavigateToVideos)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "فيديوهات نمط تيك توك",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFFF2A6D).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "سحب رأسي",
                                    color = Color(0xFFFF2A6D),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "سحب لأسفل ولأعلى • انتقال تلقائي • تشغيل عشوائي",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // 4. Section: مقترح لك (Recommended For You)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "مقترح لك",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(recommended) { item ->
                    RecommendedCard(
                        item = item,
                        onClick = {
                            // If playlist with this name exists in DB, open it; otherwise play or open first playlist
                            val existing = playlists.find { it.name.equals(item.title, ignoreCase = true) }
                            if (existing != null) {
                                onNavigateToPlaylistDetails(existing.id)
                            } else if (playlists.isNotEmpty()) {
                                onNavigateToPlaylistDetails(playlists.first().id)
                            } else if (audios.isNotEmpty()) {
                                val player = PlayerManager.initPlayer(viewModel.getApplication())
                                player.stop()
                                player.clearMediaItems()
                                player.addMediaItems(audios.take(5).map { MediaItem.fromUri(it.uri) })
                                player.prepare()
                                player.play()
                                onNavigateToPlayer()
                            }
                        }
                    )
                }
            }
        }

        // 5. Popular Tracks List
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "أغاني مختارة",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
        }

        val displayAudios = if (searchQuery.isNotEmpty()) {
            audios.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
        } else {
            audios.take(8)
        }

        items(displayAudios) { audio ->
            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                SongRowItem(
                    song = audio,
                    onClick = {
                        val player = PlayerManager.initPlayer(viewModel.getApplication())
                        player.stop()
                        player.clearMediaItems()
                        player.setMediaItem(MediaItem.fromUri(audio.uri))
                        player.prepare()
                        player.play()
                        onNavigateToPlayer()
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryPillItem(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        )
    }
}

@Composable
fun RecommendedCard(
    item: RecommendedPlaylist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            ) {
                if (item.customImagePath != null) {
                    AsyncImage(
                        model = File(item.customImagePath),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = item.coverResId),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Small play icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
