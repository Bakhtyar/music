package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val filePath: String,
    val mediaType: String, // "AUDIO" or "VIDEO"
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_media",
    primaryKeys = ["playlistId", "filePath"]
)
data class PlaylistMediaCrossRef(
    val playlistId: Long,
    val filePath: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_media")
data class HiddenMediaEntity(
    @PrimaryKey val filePath: String
)

@Entity(tableName = "media_metadata")
data class MediaMetadataEntity(
    @PrimaryKey val filePath: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUri: String
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
