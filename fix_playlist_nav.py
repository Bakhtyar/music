import re

with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    "onNavigateToPlayer: () -> Unit",
    "onNavigateToPlayer: () -> Unit,\n    onNavigateToVideoPlayer: (String, Long?) -> Unit"
)

old_click = """                                .clickable {
                                    if (media.type == "VIDEO") {
                                        // We need a way to navigate to swipe_video_player
                                    } else {
                                        val index = playlistMedia.indexOf(media)
                                        val player = com.example.player.PlayerManager.exoPlayer
                                        player?.setMediaItems(playlistMedia.map { androidx.media3.common.MediaItem.fromUri(it.uri) })
                                        player?.seekTo(index, 0)
                                        player?.prepare()
                                        player?.play()
                                        onNavigateToPlayer()
                                    }
                                }"""

new_click = """                                .clickable {
                                    if (media.type == "VIDEO") {
                                        onNavigateToVideoPlayer(media.uri.toString(), playlistId)
                                    } else {
                                        val index = playlistMedia.indexOf(media)
                                        val player = com.example.player.PlayerManager.exoPlayer
                                        // Filter only audio for the audio player queue
                                        val audioList = playlistMedia.filter { it.type == "AUDIO" }
                                        val audioIndex = audioList.indexOf(media)
                                        if (audioIndex >= 0) {
                                            player?.setMediaItems(audioList.map { androidx.media3.common.MediaItem.fromUri(it.uri) })
                                            player?.seekTo(audioIndex, 0)
                                            player?.prepare()
                                            player?.play()
                                            onNavigateToPlayer()
                                        }
                                    }
                                }"""

content = content.replace(old_click, new_click)
with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/AppNavigation.kt", "r") as f:
    nav = f.read()

nav = nav.replace(
    "onNavigateToPlayer = { navController.navigate(\"player\") }",
    "onNavigateToPlayer = { navController.navigate(\"player\") },\n                    onNavigateToVideoPlayer = { uri, pId -> \n                        navController.navigate(\"swipe_video_player/${java.net.URLEncoder.encode(uri, \"UTF-8\")}${if (pId != null) \"?playlistId=$pId\" else \"\"}\") \n                    }"
)

with open("app/src/main/java/com/example/ui/AppNavigation.kt", "w") as f:
    f.write(nav)
