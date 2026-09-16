with open("app/src/main/java/com/example/ui/AppNavigation.kt", "r") as f:
    content = f.read()

# Fix MainScreen
bad_main = """MainScreen(viewModel, onNavigateToPlayer = { navController.navigate("player") },                    onNavigateToVideoPlayer = { uri -> 
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}") 
                    }, onNavigateToVideoPlayer = { uri -> navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}") }, onNavigateToPlaylist = { id -> navController.navigate("playlist_details/$id") })"""

good_main = """MainScreen(viewModel, onNavigateToPlayer = { navController.navigate("player") }, onNavigateToVideoPlayer = { uri -> navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}") }, onNavigateToPlaylist = { id -> navController.navigate("playlist_details/$id") })"""

content = content.replace(bad_main, good_main)

# Fix PlaylistDetailsScreen
bad_playlist = """onNavigateToVideoPlayer = { uri -> 
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}") 
                    }"""

good_playlist = """onNavigateToVideoPlayer = { uri, pId -> 
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}${if (pId != null) "?playlistId=$pId" else ""}") 
                    }"""

content = content.replace(bad_playlist, good_playlist)

with open("app/src/main/java/com/example/ui/AppNavigation.kt", "w") as f:
    f.write(content)

