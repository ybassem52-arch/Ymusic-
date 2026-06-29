package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String, // YouTube Video ID
    val title: String,
    val artist: String,
    val duration: String,
    val thumbnailUrl: String,
    val audioUrl: String,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val isFavorite: Boolean = false,
    val downloadTimestamp: Long? = null
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Int,
    val songId: String
)
