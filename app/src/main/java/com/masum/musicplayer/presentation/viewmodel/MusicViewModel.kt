package com.masum.musicplayer.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.masum.musicplayer.data.model.Song
import com.masum.musicplayer.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(context: Context) : ViewModel() {
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
    
    // Recently Added: last 10 songs by a mock 'dateAdded' property (if available)
    val recentlyAdded: StateFlow<List<Song>>
        get() = MutableStateFlow(_songs.value.sortedByDescending { it.id }.take(10))

    // Most Played: mock logic, just return first 10 songs for now
    val mostPlayed: StateFlow<List<Song>>
        get() = MutableStateFlow(_songs.value.take(10))

    // Favorites: mock logic, just return first 5 songs for now
    val favorites: StateFlow<List<Song>>
        get() = MutableStateFlow(_songs.value.take(5))
    
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
    
    fun playSong(song: Song) {
        Log.d(TAG, "Playing song: ${song.title}")
        try {
            player?.let { exoPlayer ->
                _currentSong.value = song
                // Create a playlist with all songs
                val mediaItems = songs.value.map { it.toMediaItem() }
                exoPlayer.setMediaItems(mediaItems)
                // Find the index of the current song in the playlist
                val currentIndex = songs.value.indexOf(song)
                if (currentIndex != -1) {
                    exoPlayer.seekTo(currentIndex, 0)
                }
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                startProgressUpdate()
            } ?: run {
                Log.e(TAG, "Player not initialized")
                _error.value = "Player not initialized"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play song: ${e.message}")
            _error.value = "Failed to play song: ${e.message}"
        }
    }
    
    fun getCurrentSongWithoutRestart(): Song? {
        return _currentSong.value
    }
    
    fun togglePlayPause() {
        try {
            player?.let { exoPlayer ->
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle play/pause: ${e.message}")
            _error.value = "Failed to toggle play/pause: ${e.message}"
        }
    }
    
    fun resumeSong() {
        try {
            player?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume song: ${e.message}")
            _error.value = "Failed to resume song: ${e.message}"
        }
    }
    
    fun pauseSong() {
        try {
            player?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause song: ${e.message}")
            _error.value = "Failed to pause song: ${e.message}"
        }
    }
    
    fun seekTo(position: Long) {
        try {
            player?.seekTo(position)
            _currentPosition.value = position
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek: ${e.message}")
            _error.value = "Failed to seek: ${e.message}"
        }
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
        try {
            player?.let { exoPlayer ->
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNext()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play next song: ${e.message}")
            _error.value = "Failed to play next song: ${e.message}"
        }
    }
    
    fun previous() {
        try {
            player?.let { exoPlayer ->
                if (exoPlayer.hasPreviousMediaItem()) {
                    exoPlayer.seekToPrevious()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play previous song: ${e.message}")
            _error.value = "Failed to play previous song: ${e.message}"
        }
    }
    
    fun rewind() {
        try {
            player?.let { exoPlayer ->
                val newPosition = (exoPlayer.currentPosition - 5000).coerceAtLeast(0)
                exoPlayer.seekTo(newPosition)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rewind: ${e.message}")
            _error.value = "Failed to rewind: ${e.message}"
        }
    }
    
    fun forward() {
        try {
            player?.let { exoPlayer ->
                val newPosition = (exoPlayer.currentPosition + 5000).coerceAtMost(exoPlayer.duration)
                exoPlayer.seekTo(newPosition)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward: ${e.message}")
            _error.value = "Failed to forward: ${e.message}"
        }
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