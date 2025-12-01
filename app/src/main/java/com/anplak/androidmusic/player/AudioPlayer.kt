package com.anplak.androidmusic.player

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayer(
    context: Context,
    private val scope: CoroutineScope
) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private var positionUpdateJob: Job? = null
    
    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updatePlaybackState()
                when (state) {
                    Player.STATE_READY -> {
                        _playbackState.value = _playbackState.value.copy(
                            duration = player.duration.coerceAtLeast(0L),
                            error = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false
                        )
                        stopPositionUpdates()
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying
                )
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                val playerError = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> PlayerError.FileNotFound
                    
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> PlayerError.UnsupportedFormat
                    
                    else -> PlayerError.Unknown(error.message ?: "Unknown error")
                }
                
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    error = playerError
                )
                stopPositionUpdates()
            }
        })
    }
    
    fun setMediaItem(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        _playbackState.value = PlaybackState()
    }
    
    fun play() {
        player.play()
    }
    
    fun pause() {
        player.pause()
    }
    
    fun seekTo(position: Long) {
        player.seekTo(position)
        _playbackState.value = _playbackState.value.copy(
            currentPosition = position
        )
    }
    
    fun release() {
        stopPositionUpdates()
        player.release()
    }
    
    fun clearError() {
        _playbackState.value = _playbackState.value.copy(error = null)
    }
    
    private fun updatePlaybackState() {
        if (player.playbackState == Player.STATE_READY) {
            _playbackState.value = _playbackState.value.copy(
                currentPosition = player.currentPosition.coerceAtLeast(0L),
                duration = player.duration.coerceAtLeast(0L)
            )
        }
    }
    
    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = scope.launch {
            while (isActive) {
                _playbackState.value = _playbackState.value.copy(
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                )
                delay(100)
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
}

