package com.anplak.androidmusic.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.anplak.androidmusic.service.MusicPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Bridge between the UI and the MusicPlaybackService via MediaController.
 * Exposes playback and queue state via StateFlow.
 */
class AudioPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = controllerFuture?.let { 
            if (it.isDone) it.get() else null 
        }
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()
    
    private var positionUpdateJob: Job? = null
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            updatePlaybackState()
            updateQueueState()
            when (state) {
                Player.STATE_READY -> {
                    _playbackState.value = _playbackState.value.copy(
                        duration = controller?.duration?.coerceAtLeast(0L) ?: 0L,
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
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateQueueState()
            updatePlaybackState()
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
    }
    
    /**
     * Connects to the playback service. Should be called when the app becomes active.
     */
    fun connect() {
        if (controllerFuture != null) return
        
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                controller?.addListener(playerListener)
                updatePlaybackState()
                updateQueueState()
            },
            MoreExecutors.directExecutor()
        )
    }
    
    /**
     * Sets the queue with tracks and starts playing from the specified index.
     */
    fun setQueue(tracks: List<TrackInfo>, startIndex: Int) {
        val mediaItems = MusicPlaybackService.tracksToMediaItems(tracks)
        controller?.apply {
            setMediaItems(mediaItems, startIndex, 0L)
            prepare()
            play()
        }
    }
    
    fun play() {
        controller?.play()
    }
    
    fun pause() {
        controller?.pause()
    }
    
    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _playbackState.value = _playbackState.value.copy(
            currentPosition = position
        )
    }
    
    fun next() {
        controller?.seekToNextMediaItem()
    }
    
    fun previous() {
        controller?.seekToPreviousMediaItem()
    }
    
    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        _playbackState.value = PlaybackState()
        _queueState.value = QueueState()
    }
    
    fun release() {
        stopPositionUpdates()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
    
    fun clearError() {
        _playbackState.value = _playbackState.value.copy(error = null)
    }
    
    private fun updatePlaybackState() {
        controller?.let { player ->
            if (player.playbackState == Player.STATE_READY) {
                _playbackState.value = _playbackState.value.copy(
                    currentPosition = player.currentPosition.coerceAtLeast(0L),
                    duration = player.duration.coerceAtLeast(0L)
                )
            }
        }
    }
    
    private fun updateQueueState() {
        controller?.let { player ->
            _queueState.value = QueueState(
                currentIndex = player.currentMediaItemIndex,
                queueSize = player.mediaItemCount,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem()
            )
        }
    }
    
    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = scope.launch {
            while (isActive) {
                controller?.let { player ->
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = player.currentPosition.coerceAtLeast(0L)
                    )
                }
                delay(100)
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
}

/**
 * State of the playback queue.
 */
data class QueueState(
    val currentIndex: Int = 0,
    val queueSize: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)
