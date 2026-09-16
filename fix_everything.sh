#!/bin/bash

# 1. Fix PlaylistsScreen
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MediaViewModel

@Composable
fun PlaylistsScreen(viewModel: MediaViewModel, onNavigateToPlaylist: (Long) -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(2f).background(Color(0xFF2C2C2C), MaterialTheme.shapes.large).clickable { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Add, "Add", tint = Color.Gray, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("إضافة جديد", color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        items(playlists) { playlist ->
            SmartPlaylistCard(
                title = playlist.name, 
                count = "قائمة تشغيل", 
                icon = Icons.Filled.QueueMusic, 
                iconTint = Color.White,
                onClick = { /* onNavigateToPlaylist(playlist.id) */ }
            )
        }
    }
    
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إنشاء قائمة جديدة") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("الاسم") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName)
                        showAddDialog = false
                        newPlaylistName = ""
                    }
                }) { Text("إنشاء") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun SmartPlaylistCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color(0xFF2C2C2C), MaterialTheme.shapes.large).clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Text(count, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Box(modifier = Modifier.size(64.dp).background(Color(0xFF404040), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
        }
    }
}
INNER_EOF

# 2. Fix AudioScreen
sed -i '/Icons.Filled.MusicNote/a import androidx.compose.material.icons.filled.MoreVert\nimport com.example.data.MediaModel' app/src/main/java/com/example/ui/screens/AudioScreen.kt
sed -i '/val selectionMode = selectedItems.isNotEmpty()/a \    var mediaToEdit by remember { mutableStateOf<MediaModel?>(null) }' app/src/main/java/com/example/ui/screens/AudioScreen.kt
sed -i '/Text("Download", style = MaterialTheme.typography.bodySmall, color = Color.Gray)/a \                    }\n                    if (!selectionMode) {\n                        IconButton(onClick = { mediaToEdit = audio }) {\n                            Icon(Icons.Filled.MoreVert, "More", tint = Color.Gray)\n                        }' app/src/main/java/com/example/ui/screens/AudioScreen.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/ui/screens/AudioScreen.kt

@Composable
fun EditMetadataDialog(media: MediaModel, viewModel: MediaViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(media.title) }
    var artist by remember { mutableStateOf(media.artist) }
    var album by remember { mutableStateOf(media.album) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل المعلومات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") })
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveMetadata(media.filePath, title, artist, album, media.coverUri)
                onDismiss()
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
INNER_EOF
sed -i '/^}$/i \    mediaToEdit?.let { EditMetadataDialog(it, viewModel) { mediaToEdit = null } }' app/src/main/java/com/example/ui/screens/AudioScreen.kt

# 3. Fix EqualizerScreen Presets
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/EqualizerScreen.kt
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.player.PlayerManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Mic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onNavigateBack: () -> Unit) {
    val equalizer = PlayerManager.equalizer ?: return
    var isEnabled by remember { mutableStateOf(equalizer.enabled) }
    
    val numBands = equalizer.numberOfBands.toInt()
    val bandLevels = remember { mutableStateListOf<Int>() }
    var activePreset by remember { mutableStateOf("مخصص") }
    
    LaunchedEffect(Unit) {
        if (bandLevels.isEmpty()) {
            for (i in 0 until numBands) bandLevels.add(equalizer.getBandLevel(i.toShort()).toInt())
        }
    }
    
    fun applyPreset(name: String, levels: List<Int>) {
        activePreset = name
        if (levels.size == numBands) {
            for (i in 0 until numBands) {
                bandLevels[i] = levels[i]
                if (isEnabled) equalizer.setBandLevel(i.toShort(), levels[i].toShort())
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2C1E30)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("المعدّل", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it; equalizer.enabled = it })
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            val minEQ = equalizer.bandLevelRange[0].toFloat()
            val maxEQ = equalizer.bandLevelRange[1].toFloat()
            
            for (i in 0 until numBands) {
                if (i < bandLevels.size) {
                    val freq = equalizer.getCenterFreq(i.toShort()) / 1000
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("+5", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Slider(
                                value = bandLevels[i].toFloat(),
                                onValueChange = { 
                                    bandLevels[i] = it.toInt()
                                    activePreset = "مخصص"
                                    if(isEnabled) equalizer.setBandLevel(i.toShort(), it.toInt().toShort())
                                },
                                valueRange = minEQ..maxEQ,
                                modifier = Modifier.rotate(-90f).width(200.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.DarkGray)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (freq >= 1000) "${freq/1000}K" else "$freq", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val maxEqInt = equalizer.bandLevelRange[1].toInt()
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PresetCard("معزز الباس", Icons.Filled.Speaker, modifier = Modifier.weight(1f), isActive = activePreset == "معزز الباس") {
                applyPreset("معزز الباس", List(numBands) { if (it < 2) maxEqInt else 0 })
            }
            PresetCard("مخصص", Icons.Filled.GraphicEq, modifier = Modifier.weight(1f), isActive = activePreset == "مخصص") {
                applyPreset("مخصص", List(numBands) { 0 })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PresetCard("معزز الصوت المرتفع", Icons.Filled.Speaker, modifier = Modifier.weight(1f), isActive = activePreset == "معزز الصوت المرتفع") {
                applyPreset("معزز الصوت المرتفع", List(numBands) { if (it > numBands - 3) maxEqInt else 0 })
            }
            PresetCard("معزز الصوت البشري", Icons.Filled.Mic, modifier = Modifier.weight(1f), isActive = activePreset == "معزز الصوت البشري") {
                applyPreset("معزز الصوت البشري", List(numBands) { if (it in 1..3) maxEqInt else 0 })
            }
        }
    }
}

@Composable
fun PresetCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, isActive: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .background(if (isActive) Color.White else Color.Transparent, RoundedCornerShape(16.dp))
            .border(1.dp, if (isActive) Color.Transparent else Color.DarkGray, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = if (isActive) Color.Black else Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = if (isActive) Color.Black else Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
INNER_EOF

# 4. Fix MainScreen
sed -i 's/PlaylistsScreen(viewModel)/PlaylistsScreen(viewModel, onNavigateToPlaylist = {})/g' app/src/main/java/com/example/ui/screens/MainScreen.kt

# 5. Fix PlayerScreen MoreVert and Lyrics
sed -i '/import androidx.compose.material.icons.filled.\*/a import com.example.data.MediaModel' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
sed -i '/val isFavorite = /a \    var showMetadataDialog by remember { mutableStateOf(false) }\n    var showLyricsDialog by remember { mutableStateOf(false) }' app/src/main/java/com/example/ui/screens/PlayerScreen.kt

sed -i 's/IconButton(onClick = { \/\* TODO \*\/ }) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }/IconButton(onClick = { showMetadataDialog = true }) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
sed -i 's/ElevatedButton(onClick = { \/\* Lyrics \*\/ }/ElevatedButton(onClick = { showLyricsDialog = true }/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/ui/screens/PlayerScreen.kt

    if (showMetadataDialog && media != null) {
        EditMetadataDialog(media, viewModel) { showMetadataDialog = false }
    }
    
    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsDialog = false },
            title = { Text("الكلمات") },
            text = { Text("لم يتم العثور على كلمات مدمجة لهذه الأغنية.") },
            confirmButton = { TextButton(onClick = { showLyricsDialog = false }) { Text("حسناً") } }
        )
    }
INNER_EOF

