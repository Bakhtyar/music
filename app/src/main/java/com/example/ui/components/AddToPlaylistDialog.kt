package com.example.ui.components

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    mediaPaths: List<String>,
    viewModel: MediaViewModel,
    onDismiss: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF2C2C2C)) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("إضافة الى قائمة تشغيل", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(16.dp))
            
            ListItem(
                headlineContent = { Text("قائمة تشغيل جديدة", color = Color.White) },
                supportingContent = { Text("٠ أغنية", color = Color.Gray) },
                trailingContent = {
                    Box(modifier = Modifier.size(48.dp).background(Color(0xFF404040), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                    }
                },
                modifier = Modifier.clickable { showCreateDialog = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { Text("أغاني أعجبتني", color = Color.White) },
                supportingContent = { Text("١٥ أغنية", color = Color.Gray) },
                trailingContent = {
                    Box(modifier = Modifier.size(48.dp).background(Color(0xFF404040), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = "Playlist", tint = Color.White)
                    }
                },
                modifier = Modifier.clickable { onDismiss() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            LazyColumn {
                items(playlists) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name, color = Color.White) },
                        supportingContent = { Text("٠ أغنية", color = Color.Gray) },
                        trailingContent = {
                            Box(modifier = Modifier.size(48.dp).background(Color(0xFF404040), MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.QueueMusic, contentDescription = "Playlist", tint = Color.White)
                            }
                        },
                        modifier = Modifier.clickable {
                            mediaPaths.forEach { path -> viewModel.addMediaToPlaylist(playlist.id, path) }
                            onDismiss()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF2C2C2C),
            title = { Text("قائمة تشغيل جديدة", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم قائمة التشغيل") },
                    supportingText = { Text("${name.length}/200", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) viewModel.createPlaylist(name)
                    showCreateDialog = false
                }, enabled = name.isNotBlank()) { Text("إنشاء") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("إلغاء الأمر") }
            }
        )
    }
}
