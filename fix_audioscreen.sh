sed -i 's/fun AudioScreen(viewModel: MediaViewModel)/fun AudioScreen(viewModel: MediaViewModel, onNavigateToPlayer: () -> Unit = {})/g' app/src/main/java/com/example/ui/screens/AudioScreen.kt
sed -i '/exoPlayer?.play()/a \                                onNavigateToPlayer()' app/src/main/java/com/example/ui/screens/AudioScreen.kt
sed -i 's/1 -> AudioScreen(viewModel)/1 -> AudioScreen(viewModel, onNavigateToPlayer)/g' app/src/main/java/com/example/ui/screens/MainScreen.kt
