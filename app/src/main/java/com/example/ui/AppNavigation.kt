package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*

@Composable
fun AppNavigation(viewModel: MediaViewModel) {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(viewModel, onNavigateToPlayer = { navController.navigate("player") })
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
            composable("queue") {
                QueueScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
