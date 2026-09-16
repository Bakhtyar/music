sed -i '/val audioShuffleMode = /i \    val searchQuery = MutableStateFlow("")\n    fun setSearchQuery(query: String) {\n        searchQuery.value = query\n    }' app/src/main/java/com/example/ui/MediaViewModel.kt

sed -i 's/audios.filter { a -> hidden.none { it.filePath == a.filePath } }/audios.filter { a -> hidden.none { it.filePath == a.filePath } \&\& a.title.contains(searchQuery.value, ignoreCase = true) }/g' app/src/main/java/com/example/ui/MediaViewModel.kt
sed -i 's/videos.filter { a -> hidden.none { it.filePath == a.filePath } }/videos.filter { a -> hidden.none { it.filePath == a.filePath } \&\& a.title.contains(searchQuery.value, ignoreCase = true) }/g' app/src/main/java/com/example/ui/MediaViewModel.kt
sed -i 's/repository.hiddenMedia,/repository.hiddenMedia,\n        searchQuery,/g' app/src/main/java/com/example/ui/MediaViewModel.kt
sed -i 's/) { audios, hidden, metadata ->/) { audios, hidden, query, metadata ->/g' app/src/main/java/com/example/ui/MediaViewModel.kt
