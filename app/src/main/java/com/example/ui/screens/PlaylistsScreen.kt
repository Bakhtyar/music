package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.MediaViewModel
import java.io.File

@Composable
fun PlaylistsScreen(viewModel: MediaViewModel, onNavigateToPlaylist: (Long) -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val statsMap by viewModel.playlistStats.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val videos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val customArt by viewModel.customArtState.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Long?>(null) }
    var selectedPlaylistForCover by remember { mutableStateOf<Long?>(null) }

    val playlistCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val targetId = selectedPlaylistForCover
        if (uri != null && targetId != null) {
            viewModel.savePlaylistCover(targetId, uri)
        }
        selectedPlaylistForCover = null
    }

    if (selectedPlaylistForCover != null) {
        val targetId = selectedPlaylistForCover!!
        val currentPath = customArt.playlistCovers[targetId]
        AlertDialog(
            onDismissRequest = { selectedPlaylistForCover = null },
            icon = {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("تخصيص غلاف قائمة التشغيل", style = MaterialTheme.typography.titleMedium) },
            text = { Text("اختر صورة من معرض الصور لتعيينها كغلاف لقائمة التشغيل.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        playlistCoverPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختيار من المعرض")
                }
            },
            dismissButton = {
                Row {
                    if (currentPath != null) {
                        TextButton(
                            onClick = {
                                viewModel.resetPlaylistCover(targetId)
                                selectedPlaylistForCover = null
                            }
                        ) {
                            Text("استعادة الافتراضي", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { selectedPlaylistForCover = null }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Create new playlist button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF241C28))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "إنشاء قائمة تشغيل جديدة",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "أضف أغانٍ وفيديوهات مفضلة لديك",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Header for User Playlists
        item {
            Text(
                "قوائم التشغيل الخاصة بك (${playlists.size})",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(playlists, key = { it.id }) { playlist ->
            val stats = statsMap[playlist.id]
            val customCoverPath = customArt.playlistCovers[playlist.id]
            val countText = if (stats == null || stats.totalCount == 0) {
                "فارغة (0 عنصر)"
            } else {
                val parts = mutableListOf<String>()
                if (stats.audioCount > 0) parts.add("${stats.audioCount} أغانٍ")
                if (stats.videoCount > 0) parts.add("${stats.videoCount} فيديو")
                parts.joinToString(" • ")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .pointerInput(playlist.id) {
                        detectTapGestures(
                            onLongPress = {
                                selectedPlaylistForCover = playlist.id
                            },
                            onTap = {
                                onNavigateToPlaylist(playlist.id)
                            }
                        )
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF332038))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customCoverPath != null) {
                                AsyncImage(
                                    model = File(customCoverPath),
                                    contentDescription = playlist.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                playlist.name,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                countText,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedPlaylistForCover = playlist.id }) {
                            Icon(
                                Icons.Filled.PhotoCamera,
                                contentDescription = "تغيير غلاف القائمة",
                                tint = Color(0xFF38BDF8).copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { playlistToDelete = playlist.id }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "حذف القائمة",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom space so content is never hidden
        item {
            Spacer(modifier = Modifier.height(80.dp))
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
                    label = { Text("اسم قائمة التشغيل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            showAddDialog = false
                            newPlaylistName = ""
                        }
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) { Text("إنشاء") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("حذف قائمة التشغيل") },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذه القائمة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        playlistToDelete?.let { viewModel.deletePlaylist(it) }
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}
