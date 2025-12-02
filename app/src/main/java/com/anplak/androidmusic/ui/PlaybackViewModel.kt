package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.player.AudioPlayer
import com.anplak.androidmusic.player.PlayerError
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val selectedTrack: TrackInfo? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val error: PlayerError? = null
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer(application, viewModelScope)
    
    private val _selectedTrack = MutableStateFlow<TrackInfo?>(null)
    
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            combine(
                _selectedTrack,
                audioPlayer.playbackState
            ) { track, playbackState ->
                PlaybackUiState(
                    selectedTrack = track,
                    isPlaying = playbackState.isPlaying,
                    currentPosition = playbackState.currentPosition,
                    duration = playbackState.duration,
                    error = playbackState.error
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    fun onTrackSelected(track: TrackInfo) {
        _selectedTrack.value = track
        audioPlayer.setMediaItem(track.uri)
        audioPlayer.play()
    }
    
    fun clearTrack() {
        _selectedTrack.value = null
        audioPlayer.pause()
    }
    
    fun onPlayPause() {
        if (_uiState.value.isPlaying) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }
    
    fun onSeek(position: Long) {
        audioPlayer.seekTo(position)
    }
    
    fun onErrorDismissed() {
        audioPlayer.clearError()
    }
    
    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}

