package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.MediaViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(viewModel: MediaViewModel, onNavigateToVideoPlayer: (String) -> Unit = {}) {
    val videos by viewModel.videoFiles.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedAudios.collectAsStateWithLifecycle()
    val selectionMode = selectedItems.isNotEmpty()

    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No videos found", color = Color.Gray)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(videos, key = { it.id }) { video ->
            val isSelected = selectedItems.contains(video.filePath)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (selectionMode) {
                                viewModel.toggleSelection(video.filePath)
                            } else {
                                onNavigateToVideoPlayer(video.uri.toString())
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(video.filePath) }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(64.dp, 48.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFF2C2C2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = video.uri,
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Video", tint = Color.White.copy(alpha = 0.8f))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(video.title, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1)
                    Text(formatTime(video.duration), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}
