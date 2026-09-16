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
                MainScreen(viewModel, onNavigateToPlayer = { navController.navigate("player") }, onNavigateToVideoPlayer = { uri -> navController.navigate("video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}") }, onNavigateToPlaylist = { id -> navController.navigate("playlist_details/$id") })
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
            composable("video_player/{uri}", arguments = listOf(navArgument("uri") { type = NavType.StringType })) {
                val uri = it.arguments?.getString("uri") ?: ""
                VideoPlayerScreen(uriString = java.net.URLDecoder.decode(uri, "UTF-8"), onNavigateBack = { navController.popBackStack() })
            }
            composable("playlist_details/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val id = it.arguments?.getLong("id") ?: 0L
                PlaylistDetailsScreen(
                    playlistId = id,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { navController.navigate("player") }
                )
            }
            composable("queue") {
                QueueScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
