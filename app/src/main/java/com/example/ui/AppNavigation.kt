package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ui.screens.*

@Composable
fun AppNavigation(viewModel: MediaViewModel) {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToPlayer = { navController.navigate("player") },
                    onNavigateToVideoPlayer = { uri ->
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}")
                    },
                    onNavigateToPlaylist = { id -> navController.navigate("playlist_details/$id") },
                    onNavigateToPlaylists = { navController.navigate("playlists") },
                    onNavigateToVideos = { navController.navigate("videos_list") },
                    onNavigateToSettings = { navController.navigate("theme_settings") },
                    onNavigateToArtists = { navController.navigate("artists") },
                    onNavigateToFavorites = { navController.navigate("favorites") }
                )
            }
            composable("videos_list") {
                VideosListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToVideoPlayer = { uri ->
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}")
                    }
                )
            }
            composable("playlists") {
                PlaylistsScreen(
                    viewModel = viewModel,
                    onNavigateToPlaylist = { id -> navController.navigate("playlist_details/$id") },
                    onNavigateToFavorites = { navController.navigate("favorites") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { navController.navigate("player") },
                    onNavigateToVideoPlayer = { uri, pId ->
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}${if (pId != null) "?playlistId=$pId" else ""}")
                    }
                )
            }
            composable("artists") {
                ArtistsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { navController.navigate("player") }
                )
            }
            composable("theme_settings") {
                ThemeSettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("player") {
                PlayerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenEqualizer = { navController.navigate("equalizer") },
                    onOpenQueue = { navController.navigate("queue") }
                )
            }
            composable("equalizer") {
                EqualizerScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("swipe_video_player/{uri}?playlistId={playlistId}", arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("playlistId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )) {
                val uri = it.arguments?.getString("uri") ?: ""
                val playlistIdStr = it.arguments?.getString("playlistId")
                val pId = playlistIdStr?.toLongOrNull()
                com.example.ui.screens.SwipeVideoPlayerScreen(
                    viewModel = viewModel,
                    initialUri = java.net.URLDecoder.decode(uri, "UTF-8"),
                    playlistId = pId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("playlist_details/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val id = it.arguments?.getLong("id") ?: 0L
                PlaylistDetailsScreen(
                    playlistId = id,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { navController.navigate("player") },
                    onNavigateToVideoPlayer = { uri, pId -> 
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}${if (pId != null) "?playlistId=$pId" else ""}") 
                    }
                )
            }
            composable("queue") {
                QueueScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
