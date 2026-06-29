package com.example.service

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

object PlaybackManager {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist

    var currentPlaylistIndex = -1
        private set

    fun setPlaylist(context: Context, songs: List<Song>, startIndex: Int) {
        _playlist.value = songs
        currentPlaylistIndex = startIndex
        if (startIndex in songs.indices) {
            playSong(context, songs[startIndex])
        }
    }

    fun playSong(context: Context, song: Song) {
        _currentSong.value = song
        _progress.value = 0L
        _duration.value = 0L
        
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                // If song is downloaded, play the local file to save data
                if (song.isDownloaded && !song.localFilePath.isNullOrEmpty() && File(song.localFilePath).exists()) {
                    setDataSource(song.localFilePath)
                } else {
                    setDataSource(song.audioUrl)
                }
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = mp.duration.toLong()
                    startProgressUpdate()

                    // Start Background Playback Foreground Service
                    val serviceIntent = Intent(context, MusicPlaybackService::class.java).apply {
                        action = MusicPlaybackService.ACTION_PLAY
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                setOnCompletionListener {
                    playNext(context)
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    stopProgressUpdate()
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isPlaying.value = false
            }
        }
    }

    fun togglePlayPause(context: Context) {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _isPlaying.value = false
            stopProgressUpdate()
        } else {
            mp.start()
            _isPlaying.value = true
            startProgressUpdate()

            // Re-trigger foreground service just in case
            val serviceIntent = Intent(context, MusicPlaybackService::class.java).apply {
                action = MusicPlaybackService.ACTION_PLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
        MusicPlaybackService.updateNotification(context)
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _progress.value = positionMs
    }

    fun playNext(context: Context) {
        val list = _playlist.value
        if (list.isEmpty()) return
        currentPlaylistIndex = (currentPlaylistIndex + 1) % list.size
        playSong(context, list[currentPlaylistIndex])
    }

    fun playPrevious(context: Context) {
        val list = _playlist.value
        if (list.isEmpty()) return
        currentPlaylistIndex = if (currentPlaylistIndex - 1 < 0) list.size - 1 else currentPlaylistIndex - 1
        playSong(context, list[currentPlaylistIndex])
    }

    fun stop(context: Context) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _currentSong.value = null
        _progress.value = 0L
        stopProgressUpdate()

        // Stop Foreground Service
        val serviceIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _progress.value = mp.currentPosition.toLong()
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
}
