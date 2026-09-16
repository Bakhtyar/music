with open("app/src/main/java/com/example/data/MediaRepository.kt", "r") as f:
    content = f.read()

video_local = """
        val localDir = context.getExternalFilesDir(null)
        if (localDir != null && localDir.exists()) {
            localDir.listFiles()?.forEach { file ->
                if (file.extension.lowercase() == "mp4" && !list.any { it.filePath == file.absolutePath }) {
                    list.add(
                        MediaModel(
                            id = file.hashCode().toLong(),
                            uri = Uri.fromFile(file),
                            filePath = file.absolutePath,
                            title = file.nameWithoutExtension,
                            duration = 0L,
                            type = "VIDEO"
                        )
                    )
                }
            }
        }
        list
    }
"""

content = content.replace("        list\n    }\n}", video_local + "}")

audio_local = """
        val localDir = context.getExternalFilesDir(null)
        if (localDir != null && localDir.exists()) {
            localDir.listFiles()?.forEach { file ->
                if (file.extension.lowercase() == "mp3" && !list.any { it.filePath == file.absolutePath }) {
                    list.add(
                        MediaModel(
                            id = file.hashCode().toLong(),
                            uri = Uri.fromFile(file),
                            filePath = file.absolutePath,
                            title = file.nameWithoutExtension,
                            duration = 0L,
                            type = "AUDIO"
                        )
                    )
                }
            }
        }
"""
content = content.replace("        val extractedDir = File(context.getExternalFilesDir(null), \"ExtractedAudio\")\n        if (extractedDir.exists()) {\n            extractedDir.listFiles()?.forEach { file ->\n                if (file.extension == \"mp3\" && !list.any { it.filePath == file.absolutePath }) {\n                    list.add(\n                        MediaModel(\n                            id = file.hashCode().toLong(),\n                            uri = Uri.fromFile(file),\n                            filePath = file.absolutePath,\n                            title = file.nameWithoutExtension,\n                            duration = 0L, // We don't have duration immediately without MediaMetadataRetriever\n                            type = \"AUDIO\"\n                        )\n                    )\n                }\n            }\n        }", audio_local)

with open("app/src/main/java/com/example/data/MediaRepository.kt", "w") as f:
    f.write(content)

