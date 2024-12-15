package ru.nyakshoot.aston_intensive_1.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.nyakshoot.aston_intensive_1.R
import ru.nyakshoot.aston_intensive_1.utils.MusicServiceConstants.PLAYBACK_STATUS_ACTION
import ru.nyakshoot.aston_intensive_1.utils.MusicServiceConstants.PLAYBACK_STATUS_EXTRA

private const val NOTIFICATION_CHANNEL_ID = "MusicPlayerChannel"
private const val NOTIFICATION_ID = 1

class MusicPlayerService : Service() {

    private lateinit var mediaPlayer: MediaPlayer

    private var currentTrackIndex = 0

    private val playlist = listOf(
        R.raw.duran_duran_invisible,
        R.raw.home_resonance
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!this::mediaPlayer.isInitialized) {
            mediaPlayer = MediaPlayer.create(this, playlist[currentTrackIndex])
            mediaPlayer.isLooping = false
            mediaPlayer.setOnCompletionListener {
                nextTrack(this)
            }
        }

        when (intent?.action) {
            PLAY -> startPlayback()
            PAUSE -> pausePlayback()
            NEXT -> nextTrack(this)
            PREVIOUS -> previousTrack(this)
            STOP -> stopForegroundService()
        }

        return START_STICKY
    }

    private fun startPlayback() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            startForeground(NOTIFICATION_ID, createNotification())
            broadcastPlaybackStatus(true)
        }
        updateNotification()
    }

    private fun pausePlayback() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            broadcastPlaybackStatus(false)
        }
        stopForeground(STOP_FOREGROUND_DETACH)
        updateNotification()
    }

    private fun stopForegroundService() {
        mediaPlayer.pause()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun broadcastPlaybackStatus(isPlaying: Boolean) {
        val intent = Intent(PLAYBACK_STATUS_ACTION)
        intent.putExtra(PLAYBACK_STATUS_EXTRA, isPlaying)
        sendBroadcast(intent)
    }

    private fun loadTrack(context: Context, resourceId: Int) {
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer.setOnCompletionListener {
            nextTrack(context)
        }
    }

    private fun nextTrack(context: Context) {
        if (playlist.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size
        loadTrack(context, playlist[currentTrackIndex])
        mediaPlayer.start()
        updateNotification()
    }

    private fun previousTrack(context: Context) {
        if (playlist.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else playlist.size - 1
        loadTrack(context, playlist[currentTrackIndex])
        mediaPlayer.start()
        updateNotification()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        ).setName("Music Player").build()

        NotificationManagerCompat.from(this)
            .createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val trackName = resources.getResourceEntryName(playlist[currentTrackIndex])

        val pendingIntentPlay = PendingIntent.getService(
            this, 0,
            Intent(this, MusicPlayerService::class.java).apply { action = PLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntentPause = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlayerService::class.java).apply { action = PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntentNext = PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlayerService::class.java).apply { action = NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntentPrevious = PendingIntent.getService(
            this, 3,
            Intent(this, MusicPlayerService::class.java).apply { action = PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntentStopService = PendingIntent.getService(
            this, 4,
            Intent(this, MusicPlayerService::class.java).apply { action = STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText(trackName)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setDeleteIntent(pendingIntentStopService)
            .addAction(R.drawable.baseline_skip_previous_24, PREVIOUS, pendingIntentPrevious)
            .addAction(
                if (mediaPlayer.isPlaying) R.drawable.baseline_pause_circle_24
                else R.drawable.baseline_play_circle_24,
                if (mediaPlayer.isPlaying) PAUSE else PLAY,
                if (mediaPlayer.isPlaying) pendingIntentPause else pendingIntentPlay
            )
            .addAction(R.drawable.baseline_skip_next_24, NEXT, pendingIntentNext)
            .addAction(R.drawable.baseline_skip_next_24, "CLOSE", pendingIntentNext)
            .build()
    }

    private fun updateNotification() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID, createNotification())
        }
    }

    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
    }

    companion object MusicServiceActions {
        const val PLAY = "PLAY"
        const val PAUSE = "PAUSE"
        const val NEXT = "NEXT"
        const val PREVIOUS = "PREVIOUS"
        const val STOP = "STOP"
    }
}