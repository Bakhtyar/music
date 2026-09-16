with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

import_lines = """import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.VideoFrameDecoder
import androidx.compose.runtime.CompositionLocalProvider"""

if "VideoFrameDecoder" not in content:
    content = content.replace("import com.example.player.PlaybackService", import_lines + "\nimport com.example.player.PlaybackService")
    
    set_content_old = "setContent {\n            MyApplicationTheme {"
    set_content_new = """setContent {
            val imageLoader = ImageLoader.Builder(this)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .crossfade(true)
                .build()
                
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                MyApplicationTheme {"""
                
    content = content.replace(set_content_old, set_content_new)
    
    # Close CompositionLocalProvider
    content = content.replace("PermissionRequestScreen(permissionState)\n                }\n            }\n        }", "PermissionRequestScreen(permissionState)\n                }\n            }\n            }\n        }")
    
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
