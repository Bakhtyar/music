with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.material.icons.filled.PlayArrow" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.MusicNote", "import androidx.compose.material.icons.filled.MusicNote\nimport androidx.compose.material.icons.filled.PlayArrow")
    
content = content.replace("Icons.Filled.PlayArrow", "androidx.compose.material.icons.filled.PlayArrow")

with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/screens/VideoScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.material.icons.filled.PlayArrow" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.RadioButtonUnchecked", "import androidx.compose.material.icons.filled.RadioButtonUnchecked\nimport androidx.compose.material.icons.filled.PlayArrow")

content = content.replace("PlayCircleOutline", "PlayArrow")

with open("app/src/main/java/com/example/ui/screens/VideoScreen.kt", "w") as f:
    f.write(content)
