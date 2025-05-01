package com.masum.musicplayer.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.masum.musicplayer.data.model.Song
import com.masum.musicplayer.data.repository.MusicRepository
import com.masum.musicplayer.MusicPlayerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(private val context: Context) : ViewModel() {
    private val repository = MusicRepository(context)
    private var player: ExoPlayer? = null
    private val TAG = "MusicViewModel"
    
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private var progressUpdateJob: Job? = null
    
    // --- FAVORITES LOGIC ---
    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()
    fun toggleFavorite(song: Song) {
        _favoriteSongIds.value = if (_favoriteSongIds.value.contains(song.id)) {
            _favoriteSongIds.value - song.id
        } else {
            _favoriteSongIds.value + song.id
        }
    }
    val favorites: StateFlow<List<Song>>
        get() = MutableStateFlow(_songs.value.filter { _favoriteSongIds.value.contains(it.id) })

    // --- MOST PLAYED LOGIC ---
    private val _playCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val playCounts: StateFlow<Map<Long, Int>> = _playCounts.asStateFlow()
    fun incrementPlayCount(song: Song) {
        _playCounts.value = _playCounts.value.toMutableMap().apply {
            put(song.id, getOrDefault(song.id, 0) + 1)
        }
    }
    val mostPlayed: StateFlow<List<Song>>
        get() = MutableStateFlow(_songs.value.sortedByDescending { _playCounts.value[it.id] ?: 0 }.take(10))

    // --- RECENTLY ADDED LOGIC ---
    // Use year as a proxy for recently added (if no dateAdded)
    val recentlyAdded: StateFlow<List<Song>>
        get() = MutableStateFlow(_songs.value.sortedByDescending { it.year }.take(10))
    
    init {
        Log.d(TAG, "Initializing MusicViewModel")
        try {
            initializePlayer(context)
            loadSongs()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize player: ${e.message}")
            _error.value = "Failed to initialize player: ${e.message}"
            _isLoading.value = false
        }
    }
    
    private fun initializePlayer(context: Context) {
        Log.d(TAG, "Initializing ExoPlayer")
        player = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startProgressUpdate()
                    } else {
                        stopProgressUpdate()
                    }
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            _duration.value = duration
                        }
                        Player.STATE_ENDED -> {
                            pauseSong()
                            seekTo(0)
                        }
                    }
                }
                
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error: ${error.message}")
                    _error.value = "Playback error: ${error.message}"
                }
            })
        }
    }
    
    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = player?.currentPosition ?: 0
                delay(100) // Update every 100ms
            }
        }
    }
    
    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }
    
    private fun loadSongs() {
        Log.d(TAG, "Loading songs from repository")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _songs.value = repository.getAllSongs()
                Log.d(TAG, "Successfully loaded ${_songs.value.size} songs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load songs: ${e.message}")
                _error.value = "Failed to load songs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun sendCommandToService(action: String, songId: Long? = null) {
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            putExtra("ACTION", action)
            songId?.let { putExtra("SONG_ID", it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun playSong(song: Song) {
        _currentSong.value = song
        sendCommandToService("PLAY", song.id)
        incrementPlayCount(song)
    }
    
    fun getCurrentSongWithoutRestart(): Song? {
        return _currentSong.value
    }
    
    fun togglePlayPause() {
        sendCommandToService("TOGGLE_PLAY_PAUSE")
    }
    
    fun resumeSong() {
        sendCommandToService("RESUME")
    }
    
    fun pauseSong() {
        sendCommandToService("PAUSE")
    }
    
    fun seekTo(position: Long) {
        sendCommandToService("SEEK", position)
    }
    
    fun seekToPercentage(percentage: Float) {
        try {
            player?.let { exoPlayer ->
                val newPosition = (percentage * (duration.value)).toLong()
                exoPlayer.seekTo(newPosition)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek to percentage: ${e.message}")
            _error.value = "Failed to seek to percentage: ${e.message}"
        }
    }
    
    fun next() {
        sendCommandToService("NEXT")
    }
    
    fun previous() {
        sendCommandToService("PREVIOUS")
    }
    
    fun rewind() {
        sendCommandToService("REWIND")
    }
    
    fun forward() {
        sendCommandToService("FORWARD")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Cleaning up MusicViewModel resources")
        try {
            // Stop progress update job
            stopProgressUpdate()
            
            // Release the player
            player?.let { exoPlayer ->
                exoPlayer.stop()
                exoPlayer.release()
                player = null
            }
            
            // Clear state
            _currentSong.value = null
            _isPlaying.value = false
            _currentPosition.value = 0L
            _duration.value = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
} 