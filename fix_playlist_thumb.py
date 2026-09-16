with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "r") as f:
    content = f.read()

import_lines = "import coil.compose.AsyncImage\nimport androidx.compose.ui.layout.ContentScale\n"
if "coil.compose.AsyncImage" not in content:
    content = content.replace("import com.example.ui.MediaViewModel", import_lines + "import com.example.ui.MediaViewModel")

old_box = """                            Box(
                                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFF2C2C2C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = "Music", tint = Color.Gray)
                            }"""

new_box = """                            Box(
                                modifier = Modifier.size(64.dp, 48.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFF2C2C2C)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (media.type == "VIDEO") {
                                    AsyncImage(
                                        model = media.uri,
                                        contentDescription = "Video Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Icon(androidx.compose.material.icons.filled.PlayCircleOutline, contentDescription = "Video", tint = Color.White.copy(alpha = 0.8f))
                                } else {
                                    Icon(Icons.Filled.MusicNote, contentDescription = "Music", tint = Color.Gray)
                                }
                            }"""

content = content.replace(old_box, new_box)

with open("app/src/main/java/com/example/ui/screens/PlaylistDetailsScreen.kt", "w") as f:
    f.write(content)
