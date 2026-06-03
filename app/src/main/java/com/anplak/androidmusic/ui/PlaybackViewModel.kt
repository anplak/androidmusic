package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.MusicLibraryRepositoryFactory
import com.anplak.androidmusic.data.PlayHistoryRepository
import com.anplak.androidmusic.data.PlayHistoryRepositoryImpl
import com.anplak.androidmusic.data.PlaylistRepository
import com.anplak.androidmusic.data.PlaylistRepositoryImpl
import com.anplak.androidmusic.data.TrackStatsRepository
import com.anplak.androidmusic.data.TrackStatsRepositoryImpl
import java.util.UUID
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.AudioPlayer
import com.anplak.androidmusic.player.PlayerError
import com.anplak.androidmusic.player.PlaybackQueue
import com.anplak.androidmusic.player.SmartShuffleGenerator
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer(application, viewModelScope)
    private val database = AppDatabase.getInstance(application)
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(database.favoriteDao())
    private val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl(database.playlistDao())
    private val trackStatsRepository: TrackStatsRepository = TrackStatsRepositoryImpl(database.trackStatsDao())
    private val playHistoryRepository: PlayHistoryRepository = PlayHistoryRepositoryImpl(database.playHistoryDao())
    private val musicLibraryRepository: MusicLibraryRepository = MusicLibraryRepositoryFactory.create(application)
    private val smartShuffleGenerator = SmartShuffleGenerator(favoritesRepository, trackStatsRepository)
    
    private var queue = PlaybackQueue.EMPTY
    
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    
    // Track the current track ID for favorite status observation
    private val _currentTrackId = MutableStateFlow<Long?>(null)
    
    // Track state for stats recording
    private var lastRecordedTrackId: Long? = null
    private var lastTrackWasPlaying: Boolean = false
    private var lastPosition: Long = 0L
    private var lastDuration: Long = 0L
    
    // Play history tracking
    private var currentSessionId: String = UUID.randomUUID().toString()
    private var currentHistoryEntryId: Long? = null
    private var trackStartTime: Long = 0L
    
    init {
        // Connect to the service when ViewModel is created
        audioPlayer.connect()
        
        // Use flatMapLatest to automatically cancel previous favorite subscriptions
        // when the track changes - prevents subscription leaks
        // Note: StateFlow is already distinctUntilChanged, no need to apply it
        val favoriteStatusFlow = _currentTrackId
            .flatMapLatest { trackId ->
                if (trackId != null) {
                    favoritesRepository.isFavorite(trackId)
                } else {
                    flowOf(false)
                }
            }
        
        viewModelScope.launch {
            combine(
                audioPlayer.playbackState,
                audioPlayer.queueState,
                favoriteStatusFlow
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
                // Track stats before updating UI state
                trackPlaybackStats(state)
                
                _uiState.value = state
                
                // Update current track ID for favorite observation (flatMapLatest handles cancellation)
                _currentTrackId.value = state.selectedTrack?.id
            }
        }
    }
    
    /**
     * Tracks playback statistics and history:
     * - Records play when a new track starts
     * - Records completion when a track finishes naturally (reaches ~end of duration)
     * - Records play history entries for timeline view
     */
    private fun trackPlaybackStats(state: PlaybackUiState) {
        val currentTrackId = state.selectedTrack?.id
        
        // Check for track completion before track change
        if (lastRecordedTrackId != null && 
            lastTrackWasPlaying && 
            lastDuration > 0 &&
            currentTrackId != lastRecordedTrackId) {
            // Previous track finished - check if it was near the end (>90% played)
            val completionThreshold = lastDuration * 0.9
            if (lastPosition >= completionThreshold) {
                viewModelScope.launch {
                    lastRecordedTrackId?.let { trackStatsRepository.recordCompletion(it) }
                }
            }
            
            // Update play history entry with duration
            finalizeHistoryEntry()
        }
        
        // Record new track play
        if (currentTrackId != null && currentTrackId != lastRecordedTrackId) {
            viewModelScope.launch {
                trackStatsRepository.recordPlay(currentTrackId)
                
                // Record play history entry
                currentHistoryEntryId = playHistoryRepository.recordPlay(currentTrackId, currentSessionId)
                trackStartTime = System.currentTimeMillis()
            }
            lastRecordedTrackId = currentTrackId
        }
        
        // Update tracking state
        lastTrackWasPlaying = state.isPlaying
        lastPosition = state.currentPosition
        lastDuration = state.duration
    }
    
    /**
     * Finalizes the current play history entry with the actual duration listened.
     */
    private fun finalizeHistoryEntry() {
        val historyId = currentHistoryEntryId ?: return
        val duration = if (lastPosition > 0) lastPosition else System.currentTimeMillis() - trackStartTime
        
        viewModelScope.launch {
            playHistoryRepository.updateDuration(historyId, duration)
        }
        currentHistoryEntryId = null
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
    
    /**
     * Starts smart shuffle mode with the entire library.
     * Uses weighted randomization favoring favorites and frequently played tracks.
     */
    fun startSmartShuffle() {
        viewModelScope.launch {
            val allTracks = musicLibraryRepository.getAllTracks()
            if (allTracks.isEmpty()) return@launch
            
            // Get recently played track IDs to avoid immediate repeats
            val recentlyPlayedIds = queue.tracks
                .take(SMART_SHUFFLE_RECENT_EXCLUDE_COUNT)
                .map { it.id }
                .toSet()
            
            val shuffledTracks = smartShuffleGenerator.generateShuffledQueue(
                tracks = allTracks,
                recentlyPlayedIds = recentlyPlayedIds
            )
            
            if (shuffledTracks.isNotEmpty()) {
                onTrackSelected(shuffledTracks, 0)
            }
        }
    }

    /**
     * Starts smart shuffle mode using a provided track list (e.g., a playlist).
     */
    fun startSmartShuffleFromPlaylist(tracks: List<TrackInfo>) {
        viewModelScope.launch {
            if (tracks.isEmpty()) return@launch
            val recentlyPlayedIds = queue.tracks
                .take(SMART_SHUFFLE_RECENT_EXCLUDE_COUNT)
                .map { it.id }
                .toSet()
            val shuffledTracks = smartShuffleGenerator.generateShuffledQueue(
                tracks = tracks,
                recentlyPlayedIds = recentlyPlayedIds
            )
            if (shuffledTracks.isNotEmpty()) {
                onTrackSelected(shuffledTracks, 0)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Finalize any pending history entry before cleanup
        finalizeHistoryEntry()
        audioPlayer.release()
    }
    
    companion object {
        private const val SMART_SHUFFLE_RECENT_EXCLUDE_COUNT = 5
    }
}
