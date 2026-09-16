with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "r") as f:
    content = f.read()

old_click = """                                .clickable {
                                    val index = playlistMedia.indexOf(media)
                                    val player = com.example.player.PlayerManager.exoPlayer
                                    player?.setMediaItems(playlistMedia.map { androidx.media3.common.MediaItem.fromUri(it.uri) })
                                    player?.seekTo(index, 0)
                                    player?.prepare()
                                    player?.play()
                                    onNavigateToPlayer()
                                }"""

new_click = """                                .clickable {
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

content = content.replace(old_click, new_click)
with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "w") as f:
    f.write(content)
