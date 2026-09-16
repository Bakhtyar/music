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
                onClick = { onNavigateToPlaylist(playlist.id) }
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
