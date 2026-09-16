package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.example.player.PlayerManager
import com.example.ui.MediaViewModel

data class GenreCategory(
    val title: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

data class ChartItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val bgGradient: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: MediaViewModel,
    onNavigateToPlayer: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val audios by viewModel.audioFiles.collectAsStateWithLifecycle()

    val categories = remember {
        listOf(
            GenreCategory("موسيقى عربية", Icons.Filled.NightsStay, listOf(Color(0xFF1E3A2F), Color(0xFF0F1E28))),
            GenreCategory("روك", Icons.Filled.ElectricBolt, listOf(Color(0xFF3E2723), Color(0xFF181014))),
            GenreCategory("بوب", Icons.Filled.Star, listOf(Color(0xFF4A148C), Color(0xFF1B0B2E))),
            GenreCategory("هيب هوب", Icons.Filled.Headphones, listOf(Color(0xFF263238), Color(0xFF0F171E))),
            GenreCategory("إلكتروني", Icons.Filled.GraphicEq, listOf(Color(0xFF0D47A1), Color(0xFF071B36))),
            GenreCategory("موسيقى هادئة", Icons.Filled.Landscape, listOf(Color(0xFF311B92), Color(0xFF120C2D)))
        )
    }

    val charts = remember {
        listOf(
            ChartItem("Top 50", "أكثر الأغاني استماعاً الآن", Icons.Filled.Whatshot, Color(0xFFFF5722), listOf(Color(0xFF3E1E16), Color(0xFF121727))),
            ChartItem("اكتشف جديد", "أحدث الإصدارات", Icons.Filled.AutoAwesome, Color(0xFF2979FF), listOf(Color(0xFF12284D), Color(0xFF121727))),
            ChartItem("مزاجك", "قوائم حسب حالتك المزاجية", Icons.Filled.SentimentSatisfied, Color(0xFFFFD600), listOf(Color(0xFF3E3612), Color(0xFF121727)))
        )
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
                            placeholder = { Text("ابحث في الفئات والأنواع...", color = Color.Gray) },
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
                            "استكشاف",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, "بحث", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: الفئات (Categories 2x3 Grid)
            item {
                Text(
                    "الفئات",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in categories.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GenreGridCard(
                                category = categories[i],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (audios.isNotEmpty()) {
                                        val player = PlayerManager.initPlayer(viewModel.getApplication())
                                        player.stop()
                                        player.clearMediaItems()
                                        player.addMediaItems(audios.shuffled().take(6).map { MediaItem.fromUri(it.uri) })
                                        player.prepare()
                                        player.play()
                                        onNavigateToPlayer()
                                    }
                                }
                            )
                            if (i + 1 < categories.size) {
                                GenreGridCard(
                                    category = categories[i + 1],
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (audios.isNotEmpty()) {
                                            val player = PlayerManager.initPlayer(viewModel.getApplication())
                                            player.stop()
                                            player.clearMediaItems()
                                            player.addMediaItems(audios.shuffled().take(6).map { MediaItem.fromUri(it.uri) })
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

            // Section 2: أكثر استماعاً (Trending Charts)
            item {
                Text(
                    "أكثر استماعاً",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    charts.forEach { chart ->
                        ChartListCard(
                            chart = chart,
                            onClick = {
                                if (audios.isNotEmpty()) {
                                    val player = PlayerManager.initPlayer(viewModel.getApplication())
                                    player.stop()
                                    player.clearMediaItems()
                                    player.addMediaItems(audios.take(8).map { MediaItem.fromUri(it.uri) })
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
}

@Composable
fun GenreGridCard(
    category: GenreCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(category.gradient))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Icon(
                    category.icon,
                    contentDescription = category.title,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ChartListCard(
    chart: ChartItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(chart.bgGradient))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(chart.iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    chart.icon,
                    contentDescription = chart.title,
                    tint = chart.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chart.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    chart.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            Icon(
                Icons.Filled.PlayCircleFilled,
                contentDescription = "تشغيل",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
