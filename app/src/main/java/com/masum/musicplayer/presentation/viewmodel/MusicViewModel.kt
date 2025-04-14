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
                exoPlayer.setMediaItem(song.toMediaItem())
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
                _currentPosition.value = newPosition
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek to percentage: ${e.message}")
            _error.value = "Failed to seek to percentage: ${e.message}"
        }
    }
    
    fun skipToNext() {
        try {
            player?.seekToNext()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to skip to next: ${e.message}")
            _error.value = "Failed to skip to next: ${e.message}"
        }
    }
    
    fun skipToPrevious() {
        try {
            player?.seekToPrevious()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to skip to previous: ${e.message}")
            _error.value = "Failed to skip to previous: ${e.message}"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            player?.release()
            player = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release player: ${e.message}")
            _error.value = "Failed to release player: ${e.message}"
        }
    }
} 