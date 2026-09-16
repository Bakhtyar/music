with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.material.icons.filled.PlayArrow", "Icons.Filled.PlayArrow")

with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "w") as f:
    f.write(content)

