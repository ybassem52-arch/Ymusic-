package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.service.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    application: Application,
    private val repository: MusicRepository
) : AndroidViewModel(application) {

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _activeLyrics = MutableStateFlow<String?>(null)
    val activeLyrics: StateFlow<String?> = _activeLyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading

    // Room-backed Flow states
    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<Song>> = repository.downloadedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Data Saver state
    var isDataSaverEnabled by mutableStateOf(false)
        private set

    // ID to Progress maps (0.0f to 1.0f)
    val downloadProgressMap = mutableStateMapOf<String, Float>()

    init {
        fetchTrending()
    }

    fun toggleDataSaver() {
        isDataSaverEnabled = !isDataSaverEnabled
    }

    fun fetchTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            _trendingSongs.value = GeminiClient.getTrendingCharts()
            _isLoading.value = false
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = GeminiClient.searchSongs(query)
            _isLoading.value = false
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isFav = !song.isFavorite
            val existing = repository.getSongById(song.id)
            if (existing == null) {
                repository.insertSong(song.copy(isFavorite = isFav))
            } else {
                repository.toggleFavorite(song.id, isFav)
            }
        }
    }

    fun downloadSong(song: Song) {
        if (downloadProgressMap.containsKey(song.id)) return
        viewModelScope.launch {
            downloadProgressMap[song.id] = 0.0f
            repository.downloadSong(song) { progress ->
                downloadProgressMap[song.id] = progress
            }
            downloadProgressMap.remove(song.id)
        }
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            repository.deleteDownload(song.id)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Int, song: Song) {
        viewModelScope.launch {
            if (repository.getSongById(song.id) == null) {
                repository.insertSong(song)
            }
            repository.addSongToPlaylist(playlistId, song.id)
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, song: Song) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, song.id)
        }
    }

    fun getSongsInPlaylist(playlistId: Int): kotlinx.coroutines.flow.Flow<List<Song>> {
        return repository.getSongsInPlaylist(playlistId)
    }

    fun loadLyrics(song: Song) {
        viewModelScope.launch {
            _lyricsLoading.value = true
            _activeLyrics.value = null
            _activeLyrics.value = GeminiClient.getLyrics(song.title, song.artist)
            _lyricsLoading.value = false
        }
    }

    fun playSong(context: Context, song: Song) {
        // Automatically save song metadata in local database history on play
        viewModelScope.launch {
            if (repository.getSongById(song.id) == null) {
                repository.insertSong(song)
            }
        }
        PlaybackManager.playSong(context, song)
    }

    fun playAll(context: Context, songs: List<Song>, startIndex: Int = 0) {
        if (songs.isNotEmpty()) {
            viewModelScope.launch {
                songs.forEach { song ->
                    if (repository.getSongById(song.id) == null) {
                        repository.insertSong(song)
                    }
                }
            }
            PlaybackManager.setPlaylist(context, songs, startIndex)
        }
    }
}
