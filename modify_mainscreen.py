import sys

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

# Replace the TopAppBar entirely to support search
topappbar_old = """TopAppBar(
                        title = { Text("Lark Player", style = MaterialTheme.typography.titleLarge, color = Color.White) },
                        actions = {
                            IconButton(onClick = { /* Settings */ }) { Icon(Icons.Filled.Settings, "Settings", tint = Color.White) }
                            IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Sort, "Sort", tint = Color.White) }
                            IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Search, "Search", tint = Color.White) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                    )"""

topappbar_new = """var isSearchActive by remember { mutableStateOf(false) }
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    
                    if (isSearchActive) {
                        TopAppBar(
                            title = { 
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("بحث...", color = Color.Gray) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                }) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                        )
                    } else {
                        TopAppBar(
                            title = { Text("Lark Player", style = MaterialTheme.typography.titleLarge, color = Color.White) },
                            actions = {
                                IconButton(onClick = { /* Settings */ }) { Icon(Icons.Filled.Settings, "Settings", tint = Color.White) }
                                IconButton(onClick = { /* Sort */ }) { Icon(Icons.Filled.Sort, "Sort", tint = Color.White) }
                                IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Filled.Search, "Search", tint = Color.White) }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                        )
                    }"""

content = content.replace(topappbar_old, topappbar_new)

# Add Convert to MP3 button in the BottomAppBar if selected tab is 0
bottomappbar_old = """ActionButton(Icons.Filled.PlaylistAdd, "إضافة إلى") { showPlaylistDialog = true }"""
bottomappbar_new = """ActionButton(Icons.Filled.PlaylistAdd, "إضافة إلى") { showPlaylistDialog = true }
                        if (selectedTab == 0) {
                            ActionButton(Icons.Filled.MusicVideo, "تحويل لـ MP3") {
                                val videos = viewModel.rawVideoFiles.value
                                selectedItems.forEach { path ->
                                    videos.find { it.filePath == path }?.let { video ->
                                        viewModel.extractAudioFromVideo(video)
                                    }
                                }
                                viewModel.clearSelection()
                            }
                        }"""

content = content.replace(bottomappbar_old, bottomappbar_new)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)

