package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.PlaylistRepository
import com.anplak.androidmusic.data.PlaylistRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.AudioPlayer
import com.anplak.androidmusic.player.PlayerError
import com.anplak.androidmusic.player.PlaybackQueue
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
    val error: PlayerError? = null,
    val queuePosition: Int = 0,
    val queueSize: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isFavorite: Boolean = false
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer(application, viewModelScope)
    private val database = AppDatabase.getInstance(application)
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(database.favoriteDao())
    private val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl(database.playlistDao())
    
    private var queue = PlaybackQueue.EMPTY
    
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    
    private val _currentTrackFavorite = MutableStateFlow(false)
    
    init {
        // Connect to the service when ViewModel is created
        audioPlayer.connect()
        
        viewModelScope.launch {
            combine(
                audioPlayer.playbackState,
                audioPlayer.queueState,
                _currentTrackFavorite
            ) { playbackState, queueState, isFavorite ->
                val currentTrack = if (queueState.queueSize > 0 && queue.tracks.isNotEmpty()) {
                    queue.tracks.getOrNull(queueState.currentIndex)
                } else null
                
                PlaybackUiState(
                    selectedTrack = currentTrack,
                    isPlaying = playbackState.isPlaying,
                    currentPosition = playbackState.currentPosition,
                    duration = playbackState.duration,
                    error = playbackState.error,
                    queuePosition = if (queueState.queueSize > 0) queueState.currentIndex + 1 else 0,
                    queueSize = queueState.queueSize,
                    hasNext = queueState.hasNext,
                    hasPrevious = queueState.hasPrevious,
                    isFavorite = isFavorite
                )
            }.collect { state ->
                _uiState.value = state
                // Update favorite status when track changes
                state.selectedTrack?.let { track ->
                    observeFavoriteStatus(track.id)
                }
            }
        }
    }
    
    private fun observeFavoriteStatus(trackId: Long) {
        viewModelScope.launch {
            favoritesRepository.isFavorite(trackId).collect { isFavorite ->
                _currentTrackFavorite.value = isFavorite
            }
        }
    }
    
    /**
     * Starts playback with a queue built from the track list, starting at the selected track.
     */
    fun onTrackSelected(tracks: List<TrackInfo>, selectedIndex: Int) {
        queue = PlaybackQueue.fromLibrary(tracks, selectedIndex)
        audioPlayer.setQueue(tracks, selectedIndex)
    }
    
    /**
     * Legacy single-track selection (builds a single-item queue).
     */
    fun onTrackSelected(track: TrackInfo) {
        onTrackSelected(listOf(track), 0)
    }
    
    fun clearTrack() {
        queue = PlaybackQueue.EMPTY
        audioPlayer.stop()
    }
    
    fun onPlayPause() {
        if (_uiState.value.isPlaying) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }
    
    fun onNext() {
        audioPlayer.next()
    }
    
    fun onPrevious() {
        audioPlayer.previous()
    }
    
    fun onSeek(position: Long) {
        audioPlayer.seekTo(position)
    }
    
    fun onErrorDismissed() {
        audioPlayer.clearError()
    }
    
    fun toggleFavorite() {
        val track = _uiState.value.selectedTrack ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track.id)
        }
    }
    
    fun addToPlaylist(playlistId: Long) {
        val track = _uiState.value.selectedTrack ?: return
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, track.id)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
