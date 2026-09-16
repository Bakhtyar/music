with open("app/src/main/java/com/example/ui/MediaViewModel.kt", "r") as f:
    content = f.read()

old_extract = """            val success = AudioExtractor.extractAudio(videoModel.filePath, outFile.absolutePath)
            if (success) {
                loadMedia() // reload to find the new mp3
            }"""

new_extract = """            val success = AudioExtractor.extractAudio(videoModel.filePath, outFile.absolutePath)
            if (success) {
                android.widget.Toast.makeText(context, "تم تحويل الفيديو إلى أغنية بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                loadMedia() // reload to find the new mp3
            } else {
                android.widget.Toast.makeText(context, "فشل تحويل الفيديو.", android.widget.Toast.LENGTH_SHORT).show()
            }"""

content = content.replace(old_extract, new_extract)

with open("app/src/main/java/com/example/ui/MediaViewModel.kt", "w") as f:
    f.write(content)

