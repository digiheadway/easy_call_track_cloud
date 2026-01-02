package com.calltracker.manager.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat
import com.calltracker.manager.MainActivity
import com.calltracker.manager.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Timer
import java.util.TimerTask

data class PlaybackMetadata(
    val name: String?,
    val phoneNumber: String,
    val callTime: String,
    val callType: String
)

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var timer: Timer? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "playback_channel"
    private val NOTIFICATION_ID = 1001

    private val ACTION_PLAY_PAUSE = "com.calltracker.manager.ACTION_PLAY_PAUSE"
    private val ACTION_STOP = "com.calltracker.manager.ACTION_STOP"

    private var currentMetadata: PlaybackMetadata? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> togglePlayPause()
                ACTION_STOP -> stop()
            }
        }
    }

    init {
        createNotificationChannel()
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_STOP)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    fun release() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {}
        stop()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for audio playback"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        if (_currentFile.value == null) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, 
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) 
                PendingIntent.FLAG_IMMUTABLE 
            else 0
        )

        val playPauseIntent = Intent(ACTION_PLAY_PAUSE).apply {
            setPackage(context.packageName)
        }
        val playPauseFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT

        val playPausePendingIntent = PendingIntent.getBroadcast(
            context, 1, playPauseIntent, playPauseFlags
        )

        val stopIntent = Intent(ACTION_STOP).apply {
            setPackage(context.packageName)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 2, stopIntent, playPauseFlags
        )

        // Build title from metadata
        val title = currentMetadata?.let { meta ->
            if (_isPlaying.value) "Playing: ${meta.name ?: meta.phoneNumber}" 
            else "Paused: ${meta.name ?: meta.phoneNumber}"
        } ?: "Call Recording"

        // Build subtitle from metadata
        val subtitle = currentMetadata?.let { meta ->
            "Playing Recording • ${meta.callTime}"
        } ?: if (_isPlaying.value) "Playing Recording..." else "Playback Paused"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText("CallCloud Player")
            .setOngoing(_isPlaying.value)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                if (_isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (_isPlaying.value) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Close",
                stopPendingIntent
            )
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)) // Only show Play/Pause in compact view
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_primary))
            .setColorized(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()
    
    private val _duration = MutableStateFlow(0)
    val duration = _duration.asStateFlow()
    
    private val _currentFile = MutableStateFlow<String?>(null)
    val currentFile = _currentFile.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed = _speed.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition = _currentPosition.asStateFlow()

    fun play(path: String, metadata: PlaybackMetadata? = null) {
        if (_currentFile.value == path && mediaPlayer != null) {
            togglePlayPause()
            return
        }

        stop()
        currentMetadata = metadata
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener { this@AudioPlayer.stop() }
                start()
                this@AudioPlayer._duration.value = duration
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    playbackParams = playbackParams.setSpeed(_speed.value)
                }
            }
            _currentFile.value = path
            _isPlaying.value = true
            startTimer()
            updateNotification()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                it.start()
                _isPlaying.value = true
            }
            updateNotification()
        }
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let {
            val newPos = (it.duration * progress).toInt()
            it.seekTo(newPos)
            _currentPosition.value = newPos
            _progress.value = progress
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed) ?: android.media.PlaybackParams().setSpeed(speed)
                _speed.value = speed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _progress.value = 0f
        _currentPosition.value = 0
        _currentFile.value = null
        currentMetadata = null
        timer?.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                mediaPlayer?.let {
                    try {
                        if (it.isPlaying && it.duration > 0) {
                            _progress.value = it.currentPosition.toFloat() / it.duration.toFloat()
                            _currentPosition.value = it.currentPosition
                        }
                    } catch (e: Exception) {
                        // Media player invalid state
                    }
                }
            }
        }, 0, 100)
    }
}
