package com.example.data.repository

import android.content.Context
import com.example.data.dao.MusicDao
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao
) {
    private val okHttpClient = OkHttpClient()

    val allSongs: Flow<List<Song>> = musicDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = musicDao.getFavoriteSongs()
    val downloadedSongs: Flow<List<Song>> = musicDao.getDownloadedSongs()
    val allPlaylists: Flow<List<Playlist>> = musicDao.getAllPlaylists()

    suspend fun getSongById(id: String): Song? = musicDao.getSongById(id)

    suspend fun insertSong(song: Song) = musicDao.insertSong(song)

    suspend fun toggleFavorite(songId: String, isFavorite: Boolean) {
        musicDao.updateFavoriteStatus(songId, isFavorite)
    }

    suspend fun createPlaylist(name: String): Long {
        return musicDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Int) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Int, songId: String) {
        musicDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String) {
        musicDao.deletePlaylistSongCrossRef(playlistId, songId)
    }

    fun getSongsInPlaylist(playlistId: Int): Flow<List<Song>> {
        return musicDao.getSongsInPlaylist(playlistId)
    }

    // --- File Download Handler ---
    suspend fun downloadSong(song: Song, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            // First ensure the song metadata is saved in the database
            musicDao.insertSong(song)

            val url = song.audioUrl
            if (url.isEmpty()) return@withContext false

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) return@withContext false
            val body = response.body ?: return@withContext false

            val downloadsDir = File(context.filesDir, "downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val localFile = File(downloadsDir, "${song.id}.mp3")
            val totalBytes = body.contentLength()

            body.byteStream().use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalBytesRead.toFloat() / totalBytes.toFloat())
                        }
                    }
                }
            }

            // Update database with local file path
            musicDao.updateDownloadStatus(
                songId = song.id,
                isDownloaded = true,
                localFilePath = localFile.absolutePath,
                timestamp = System.currentTimeMillis()
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = File(context.filesDir, "downloads")
            val localFile = File(downloadsDir, "$songId.mp3")
            if (localFile.exists()) {
                localFile.delete()
            }
            musicDao.updateDownloadStatus(
                songId = songId,
                isDownloaded = false,
                localFilePath = null,
                timestamp = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
