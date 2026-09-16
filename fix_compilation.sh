#!/bin/bash

# Fix AudioScreen
sed -i 's/audio.uri == currentlyPlayingUri/audio.uri.toString() == currentlyPlayingUri/g' app/src/main/java/com/example/ui/screens/AudioScreen.kt

# Fix MiniPlayer
sed -i 's/it.uri == currentUri/it.uri.toString() == currentUri/g' app/src/main/java/com/example/ui/screens/MiniPlayer.kt

# Fix PlayerScreen
sed -i 's/it.uri == currentUri/it.uri.toString() == currentUri/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt

# Fix MainScreen (Add import)
sed -i '/import androidx.compose.material3.\*/a import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset' app/src/main/java/com/example/ui/screens/MainScreen.kt

