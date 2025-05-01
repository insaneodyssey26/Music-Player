package com.masum.musicplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.PlayerNotificationManager
import com.masum.musicplayer.data.model.Song
import com.masum.musicplayer.data.repository.MusicRepository
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicPlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private lateinit var exoPlayer: ExoPlayer
    private var songList: List<Song> = emptyList()
    private lateinit var repository: MusicRepository

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this)
        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
        setMediaSession(mediaSession)
        createNotificationChannel()
        // Load songs asynchronously
        lifecycleScope.launch {
            songList = repository.getAllSongs()
        }
        setupNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.getStringExtra("ACTION")?.let { action ->
            when (action) {
                "PLAY" -> {
                    val songId = intent.getLongExtra("SONG_ID", -1)
                    val song = songList.find { it.id == songId }
                    song?.let { playSong(it) }
                }
                "PAUSE" -> exoPlayer.pause()
                "RESUME" -> exoPlayer.play()
                "TOGGLE_PLAY_PAUSE" -> if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                "NEXT" -> exoPlayer.seekToNext()
                "PREVIOUS" -> exoPlayer.seekToPrevious()
                "SEEK" -> {
                    val pos = intent.getLongExtra("SONG_ID", 0L)
                    exoPlayer.seekTo(pos)
                }
                "REWIND" -> exoPlayer.seekTo((exoPlayer.currentPosition - 5000).coerceAtLeast(0))
                "FORWARD" -> exoPlayer.seekTo((exoPlayer.currentPosition + 5000).coerceAtMost(exoPlayer.duration))
            }
        }
        return START_STICKY
    }

    private fun playSong(song: Song) {
        val mediaItems = songList.map { it.toMediaItem() }
        exoPlayer.setMediaItems(mediaItems)
        val index = songList.indexOf(song)
        if (index != -1) {
            exoPlayer.seekTo(index, 0)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun setupNotification() {
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: androidx.media3.common.Player): CharSequence {
                    return player.mediaMetadata.title ?: "Music Player"
                }
                override fun createCurrentContentIntent(player: androidx.media3.common.Player): PendingIntent? {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    return PendingIntent.getActivity(
                        this@MusicPlayerService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                override fun getCurrentContentText(player: androidx.media3.common.Player): CharSequence? {
                    return player.mediaMetadata.artist
                }
                override fun getCurrentLargeIcon(
                    player: androidx.media3.common.Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): android.graphics.Bitmap? {
                    return null
                }
            })
            .setChannelImportance(NotificationManager.IMPORTANCE_LOW)
            .setSmallIconResourceId(R.drawable.ic_launcher_foreground)
            .build()
        playerNotificationManager?.setPlayer(exoPlayer)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Music playback controls"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        mediaSession?.release()
        exoPlayer.release()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_playback_channel"
    }
} 