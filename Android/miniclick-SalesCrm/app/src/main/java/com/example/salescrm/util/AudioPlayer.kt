package com.example.salescrm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import java.io.File
import kotlinx.coroutines.*

/**
 * AudioPlayer - Singleton to manage audio playback for call recordings
 */
object AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPath: String? = null
    private var _isPlaying = mutableStateOf(false)
    private var _currentProgress = mutableStateOf(0f)
    private var _currentPosition = mutableStateOf(0)
    private var _duration = mutableStateOf(0)
    private var _isMinimized = mutableStateOf(false)
    
    val isPlaying: State<Boolean> = _isPlaying
    val currentPosition: State<Int> = _currentPosition
    val currentProgress: State<Float> = _currentProgress
    val duration: State<Int> = _duration
    val isMinimized: State<Boolean> = _isMinimized
    
    private var progressTracker: kotlinx.coroutines.Job? = null
    
    fun setMinimized(minimized: Boolean) {
        _isMinimized.value = minimized
    }
    
    fun play(context: Context, path: String, onError: (String) -> Unit = {}, onComplete: () -> Unit = {}) {
        try {
            // If same file is playing, toggle pause/play
            if (currentPath == path && mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == true) {
                    pause()
                } else {
                    resume()
                    _isMinimized.value = false
                }
                return
            }
            
            // Stop any current playback
            stop()
            
            val file = File(path)
            if (!file.exists()) {
                onError("Recording file not found")
                Log.e("AudioPlayer", "File not found: $path")
                return
            }
            
            currentPath = path
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setDataSource(context, Uri.fromFile(file))
                
                setOnPreparedListener { mp ->
                    _duration.value = mp.duration
                    mp.start()
                    _isPlaying.value = true
                    _isMinimized.value = false
                    Log.d("AudioPlayer", "Playing: $path, duration: ${mp.duration}ms")
                    startProgressTracker()
                }
                
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    _currentProgress.value = 0f
                    currentPath = null
                    stopProgressTracker()
                    onComplete()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer error: what=$what, extra=$extra")
                    onError("Playback error: $what")
                    stop()
                    true
                }
                
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing audio", e)
            onError("Cannot play: ${e.message}")
            stop()
        }
    }
    
    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }
    
    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                _isMinimized.value = false
            }
        }
    }
    
    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping", e)
        }
        mediaPlayer = null
        currentPath = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _currentProgress.value = 0f
        _duration.value = 0
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTracker = scope.launch {
            while (true) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
                        _currentProgress.value = it.currentPosition.toFloat() / it.duration.toFloat()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTracker?.cancel()
        progressTracker = null
    }
    
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }
    
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }
    
    fun isPlayingPath(path: String): Boolean {
        return currentPath == path && _isPlaying.value
    }
    
    fun getPlayingPath(): String? = currentPath
}

/**
 * Composable state holder for audio playback
 */
@Composable
fun rememberAudioPlayerState(): AudioPlayerState {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isPlaying by AudioPlayer.isPlaying
    val playingPath = AudioPlayer.getPlayingPath()
    val isMinimized by AudioPlayer.isMinimized
    val progress by AudioPlayer.currentProgress
    val position by AudioPlayer.currentPosition
    val duration by AudioPlayer.duration
    
    return remember(isPlaying, playingPath, isMinimized, progress, position, duration) {
        AudioPlayerState(
            isPlaying = isPlaying,
            playingPath = playingPath,
            isMinimized = isMinimized,
            progress = progress,
            position = position,
            duration = duration,
            play = { path, onError -> AudioPlayer.play(context, path, onError) },
            stop = { AudioPlayer.stop() },
            toggle = { path, onError -> AudioPlayer.play(context, path, onError) },
            setMinimized = { AudioPlayer.setMinimized(it) },
            seekTo = { AudioPlayer.seekTo(it) }
        )
    }
}

data class AudioPlayerState(
    val isPlaying: Boolean,
    val playingPath: String?,
    val isMinimized: Boolean,
    val progress: Float,
    val position: Int,
    val duration: Int,
    val play: (String, (String) -> Unit) -> Unit,
    val stop: () -> Unit,
    val toggle: (String, (String) -> Unit) -> Unit,
    val setMinimized: (Boolean) -> Unit,
    val seekTo: (Int) -> Unit
) {
    fun isPlayingPath(path: String): Boolean = playingPath == path && isPlaying
}
