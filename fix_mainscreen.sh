sed -i 's/fun MainScreen(viewModel: MediaViewModel, onNavigateToPlayer: () -> Unit)/fun MainScreen(viewModel: MediaViewModel, onNavigateToPlayer: () -> Unit, onNavigateToVideoPlayer: (String) -> Unit)/g' app/src/main/java/com/example/ui/screens/MainScreen.kt
sed -i 's/0 -> VideoScreen(viewModel, onNavigateToVideoPlayer = { uri -> navController.navigate("video_player\/${java.net.URLEncoder.encode(uri, "UTF-8")}") })/0 -> VideoScreen(viewModel, onNavigateToVideoPlayer = onNavigateToVideoPlayer)/g' app/src/main/java/com/example/ui/screens/MainScreen.kt

# In AppNavigation.kt:
sed -i 's/MainScreen(viewModel, onNavigateToPlayer = { navController.navigate("player") })/MainScreen(viewModel, onNavigateToPlayer = { navController.navigate("player") }, onNavigateToVideoPlayer = { uri -> navController.navigate("video_player\/${java.net.URLEncoder.encode(uri, "UTF-8")}") })/g' app/src/main/java/com/example/ui/AppNavigation.kt
