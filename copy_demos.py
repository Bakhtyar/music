import sys

with open("app/src/main/java/com/example/data/MediaRepository.kt", "r") as f:
    content = f.read()

injection = """
    private fun copyDemoFilesIfNeeded() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("demos_copied", false)) {
            try {
                val audioFile = File(context.getExternalFilesDir(null), "Preview_Audio.mp3")
                val videoFile = File(context.getExternalFilesDir(null), "Preview_Video.mp4")
                if (!audioFile.exists()) {
                    context.resources.openRawResource(context.resources.getIdentifier("demo_audio", "raw", context.packageName)).use { input ->
                        audioFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (!videoFile.exists()) {
                    context.resources.openRawResource(context.resources.getIdentifier("demo_video", "raw", context.packageName)).use { input ->
                        videoFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                prefs.edit().putBoolean("demos_copied", true).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
"""

if "copyDemoFilesIfNeeded" not in content:
    content = content.replace("class MediaRepository(private val context: Context, private val dao: MediaDao) {", "class MediaRepository(private val context: Context, private val dao: MediaDao) {\n" + injection)
    
    # Call it in loadAudioFiles
    content = content.replace("suspend fun loadAudioFiles(): List<MediaModel> = withContext(Dispatchers.IO) {", "suspend fun loadAudioFiles(): List<MediaModel> = withContext(Dispatchers.IO) {\n        copyDemoFilesIfNeeded()\n")
    
with open("app/src/main/java/com/example/data/MediaRepository.kt", "w") as f:
    f.write(content)

