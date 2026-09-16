with open("app/src/main/java/com/example/ui/AppNavigation.kt", "r") as f:
    content = f.read()

# Fix MainScreen parameter in AppNavigation (it should only accept one argument from MainScreen, as it doesn't know about playlistId)
old_main = """onNavigateToVideoPlayer = { uri, pId -> 
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}${if (pId != null) "?playlistId=$pId" else ""}") 
                    }"""

new_main = """onNavigateToVideoPlayer = { uri -> 
                        navController.navigate("swipe_video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}") 
                    }"""
content = content.replace(old_main, new_main)

# However, PlaylistDetailsScreen DOES pass two parameters. Wait, AppNavigation doesn't define what MainScreen calls.
# It defines what it passes to MainScreen and PlaylistDetailsScreen.
with open("app/src/main/java/com/example/ui/AppNavigation.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "r") as f:
    content2 = f.read()

if "import androidx.compose.material.icons.filled.PlayCircleOutline" not in content2:
    content2 = content2.replace("import androidx.compose.material.icons.filled.RadioButtonUnchecked", "import androidx.compose.material.icons.filled.RadioButtonUnchecked\nimport androidx.compose.material.icons.filled.PlayCircleOutline")

with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "w") as f:
    f.write(content2)

