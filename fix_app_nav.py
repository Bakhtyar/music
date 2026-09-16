with open("app/src/main/java/com/example/ui/AppNavigation.kt", "r") as f:
    content = f.read()

import re

# We will remove VideoPlayerScreen and replace it with SwipeVideoPlayerScreen
old_nav = """composable("video_player/{uri}", arguments = listOf(navArgument("uri") { type = NavType.StringType })) {
                val uri = it.arguments?.getString("uri") ?: ""
                VideoPlayerScreen(uriString = java.net.URLDecoder.decode(uri, "UTF-8"), onNavigateBack = { navController.popBackStack() })
            }"""

new_nav = """composable("swipe_video_player/{uri}?playlistId={playlistId}", arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("playlistId") { type = NavType.StringType; nullable = true }
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
            }"""
content = content.replace(old_nav, new_nav)

content = content.replace('navController.navigate("video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}")', 'navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}")')

with open("app/src/main/java/com/example/ui/AppNavigation.kt", "w") as f:
    f.write(content)
