package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
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
    val statsMap by viewModel.playlistStats.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E2433),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                "إضافة إلى قائمة تشغيل (${mediaPaths.size} عناصر محددة)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            
            // New Playlist Action
            ListItem(
                headlineContent = { Text("قائمة تشغيل جديدة", color = Color(0xFF38BDF8)) },
                supportingContent = { Text("إنشاء قائمة جديدة وإضافة العناصر المحددة إليها", color = Color.Gray) },
                leadingContent = {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color(0xFF38BDF8))
                    }
                },
                modifier = Modifier.clickable { showCreateDialog = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Favorites Action
            ListItem(
                headlineContent = { Text("المفضلة", color = Color.White) },
                supportingContent = { Text("${favorites.size} عنصر في المفضلة", color = Color.Gray) },
                leadingContent = {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFFFF2A6D).copy(alpha = 0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Favorite", tint = Color(0xFFFF2A6D))
                    }
                },
                modifier = Modifier.clickable {
                    viewModel.addSelectedToFavorites()
                    onDismiss()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 6.dp))

            Text(
                "قوائمك الحالية (${playlists.size})",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(playlists, key = { it.id }) { playlist ->
                    val stats = statsMap[playlist.id]
                    val countStr = if (stats == null || stats.totalCount == 0) "فارغة (0 عنصر)"
                    else "${stats.audioCount} أغانٍ • ${stats.videoCount} فيديو"
                    ListItem(
                        headlineContent = { Text(playlist.name, color = Color.White) },
                        supportingContent = { Text(countStr, color = Color.Gray) },
                        leadingContent = {
                            Box(modifier = Modifier.size(44.dp).background(Color(0xFF243048), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.QueueMusic, contentDescription = "Playlist", tint = Color(0xFF38BDF8))
                            }
                        },
                        modifier = Modifier.clickable {
                            viewModel.addMultipleMediaToPlaylist(playlist.id, playlist.name, mediaPaths) {
                                onDismiss()
                            }
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
            containerColor = Color(0xFF1E2433),
            title = { Text("قائمة تشغيل جديدة", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم قائمة التشغيل", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createPlaylistAndAddMedia(name.trim(), mediaPaths) {
                                showCreateDialog = false
                                onDismiss()
                            }
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) { Text("إنشاء وإضافة", color = Color.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("إلغاء", color = Color.Gray) }
            }
        )
    }
}
