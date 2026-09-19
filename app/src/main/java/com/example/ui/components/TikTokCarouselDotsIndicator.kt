package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * TikTok-style carousel indicator dots for photo posts.
 * - Displays dots matching the number of photos in the post.
 * - Caps the visible dots at a maximum (default 8 dots) to keep the UI clean and avoid clutter.
 * - Implements a sliding window centered around the active photo with edge dot scaling.
 */
@Composable
fun TikTokCarouselDotsIndicator(
    totalCount: Int,
    currentIndex: Int,
    maxVisibleDots: Int = 8,
    modifier: Modifier = Modifier
) {
    if (totalCount <= 1) return

    val visibleCount = minOf(totalCount, maxVisibleDots)

    // Calculate window range [start, end)
    val startIndex = when {
        totalCount <= maxVisibleDots -> 0
        currentIndex <= maxVisibleDots / 2 -> 0
        currentIndex >= totalCount - (maxVisibleDots / 2) -> totalCount - maxVisibleDots
        else -> currentIndex - (maxVisibleDots / 2)
    }
    val endIndex = startIndex + visibleCount

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in startIndex until endIndex) {
            val isSelected = i == currentIndex
            val isEdge = totalCount > maxVisibleDots && (i == startIndex || i == endIndex - 1)

            val targetSize = when {
                isSelected -> 7.dp
                isEdge -> 4.dp
                else -> 5.5.dp
            }
            val animatedSize by animateDpAsState(
                targetValue = targetSize,
                animationSpec = tween(durationMillis = 250),
                label = "dot_size"
            )

            val color = when {
                isSelected -> Color(0xFF38BDF8) // Highlighting cyan/white
                isEdge -> Color.White.copy(alpha = 0.35f)
                else -> Color.White.copy(alpha = 0.6f)
            }

            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
