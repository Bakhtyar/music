package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel

data class ArtistItem(
    val name: String,
    val followers: String,
    val initialColor: Color,
    val isPopular: Boolean = true,
    val isNew: Boolean = false,
    val genre: String = "Pop"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: MediaViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf("الكل", "محبوبون", "جدد", "الأنواع")

    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()
    
    // Curated artist list matching the mockup + any local device artists
    val baseArtists = remember {
        listOf(
            ArtistItem("The Weeknd", "62.4M متابع", Color(0xFFE53935), isPopular = true, genre = "R&B"),
            ArtistItem("Imagine Dragons", "58.3M متابع", Color(0xFF1E88E5), isPopular = true, genre = "Rock"),
            ArtistItem("Post Malone", "55.1M متابع", Color(0xFF43A047), isPopular = true, genre = "Hip Hop"),
            ArtistItem("Billie Eilish", "52.7M متابع", Color(0xFFFB8C00), isPopular = true, isNew = true, genre = "Alternative"),
            ArtistItem("Arctic Monkeys", "49.2M متابع", Color(0xFF8E24AA), isPopular = true, genre = "Rock"),
            ArtistItem("Eminem", "46.8M متابع", Color(0xFF00ACC1), isPopular = true, genre = "Hip Hop"),
            ArtistItem("Mr.Kitty", "24.1M متابع", Color(0xFF3949AB), isPopular = false, isNew = true, genre = "Synthwave"),
            ArtistItem("Lord Huron", "19.5M متابع", Color(0xFF00897B), isPopular = false, isNew = false, genre = "Indie"),
            ArtistItem("XXXTENTACION", "38.2M متابع", Color(0xFF5E35B1), isPopular = true, genre = "Rap"),
            ArtistItem("Beach House", "15.3M متابع", Color(0xFFD81B60), isPopular = false, genre = "Dream Pop")
        )
    }

    val dynamicArtists = remember(audios) {
        val deviceArtists = audios.map { it.artist.trim() }
            .filter { it.isNotEmpty() && it != "فنان غير معروف" && baseArtists.none { b -> b.name.equals(it, ignoreCase = true) } }
            .distinct()
            .map { ArtistItem(it, "أغاني محلية", Color(0xFF0288D1), isPopular = false) }
        baseArtists + deviceArtists
    }

    val filteredArtists = dynamicArtists.filter { artist ->
        val matchesQuery = artist.name.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "محبوبون" -> artist.isPopular
            "جدد" -> artist.isNew
            "الأنواع" -> true
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث عن فنان...", color = Color.Gray) },
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
                            Icon(Icons.Filled.ArrowBack, "إغلاق البحث", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "الفنانين",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Filled.ArrowForward, "رجوع", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Filled.Search, "بحث", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    },
                    actions = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Filled.Search, "بحث", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Filter chips row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Artists list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredArtists) { artist ->
                    ArtistRowItem(
                        artist = artist,
                        onClick = {
                            // Find any songs by this artist and play
                            val artistSongs = audios.filter { it.artist.contains(artist.name, ignoreCase = true) }
                            if (artistSongs.isNotEmpty()) {
                                val player = PlayerManager.initPlayer(viewModel.getApplication())
                                player.stop()
                                player.clearMediaItems()
                                val items = artistSongs.map { MediaItem.fromUri(it.uri) }
                                player.addMediaItems(items)
                                player.prepare()
                                player.play()
                                onNavigateToPlayer()
                            } else if (audios.isNotEmpty()) {
                                val player = PlayerManager.initPlayer(viewModel.getApplication())
                                player.stop()
                                player.clearMediaItems()
                                val items = audios.take(5).map { MediaItem.fromUri(it.uri) }
                                player.addMediaItems(items)
                                player.prepare()
                                player.play()
                                onNavigateToPlayer()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistRowItem(
    artist: ArtistItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with subtle glowing gradient
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(artist.initialColor, artist.initialColor.copy(alpha = 0.3f))
                    )
                )
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                artist.name.take(2).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                artist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                artist.followers,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Filled.ChevronLeft,
            contentDescription = "عرض",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
