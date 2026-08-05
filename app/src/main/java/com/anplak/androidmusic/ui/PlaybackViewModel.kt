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
import com.anplak.androidmusic.data.TrackStatsRepository
import com.anplak.androidmusic.data.TrackStatsRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.AudioPlayer
import com.anplak.androidmusic.player.PlaybackController
import com.anplak.androidmusic.player.PlaybackSessionRecorder
import com.anplak.androidmusic.player.PlaybackStatsTracker
import com.anplak.androidmusic.player.PlayerError
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
    val isFavorite: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer(application, viewModelScope)
    private val database = AppDatabase.getInstance(application)
    private val favoritesRepository: FavoritesRepository =
        FavoritesRepositoryImpl(
            database.favoriteDao(),
            database.trackDao(),
        )
    private val trackStatsRepository: TrackStatsRepository = TrackStatsRepositoryImpl(database.trackStatsDao())
    private val playHistoryRepository: PlayHistoryRepository = PlayHistoryRepositoryImpl(database.playHistoryDao())
    private val musicLibraryRepository: MusicLibraryRepository = MusicLibraryRepositoryFactory.create(application)
    private val smartShuffleGenerator = SmartShuffleGenerator(favoritesRepository, trackStatsRepository)
    private val playbackStatsTracker = PlaybackStatsTracker()
    private val playbackController = PlaybackController(audioPlayer, playbackStatsTracker)
    private val sessionRecorder =
        PlaybackSessionRecorder(
            playbackStatsTracker = playbackStatsTracker,
            trackStatsRepository = trackStatsRepository,
            playHistoryRepository = playHistoryRepository,
            scope = viewModelScope,
        )

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val currentTrackIdFlow = MutableStateFlow<Long?>(null)

    init {
        playbackController.connect()

        val favoriteStatusFlow =
            currentTrackIdFlow
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
                favoriteStatusFlow,
            ) { playbackState, queueState, isFavorite ->
                val queue = playbackController.queue
                val currentTrack =
                    if (queueState.queueSize > 0 && queue.tracks.isNotEmpty()) {
                        queue.tracks.getOrNull(queueState.currentIndex)
                    } else {
                        null
                    }

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
                    isFavorite = isFavorite,
                )
            }.collect { state ->
                sessionRecorder.onUiStateTick(
                    trackId = state.selectedTrack?.id,
                    isPlaying = state.isPlaying,
                    positionMs = state.currentPosition,
                    durationMs = state.duration,
                )
                _uiState.value = state
                currentTrackIdFlow.value = state.selectedTrack?.id
            }
        }
    }

    fun onTrackSelected(
        tracks: List<TrackInfo>,
        selectedIndex: Int,
    ) {
        playbackController.onTrackSelected(tracks, selectedIndex)
    }

    fun clearTrack() {
        sessionRecorder.onClear()
        playbackController.clearQueueAndStop()
    }

    fun onPlayPause() {
        playbackController.onPlayPause(_uiState.value.isPlaying)
    }

    fun onNext() {
        sessionRecorder.onManualAdvance()
        playbackController.onNext()
    }

    fun onPrevious() {
        sessionRecorder.onManualAdvance()
        playbackController.onPrevious()
    }

    fun onSeek(position: Long) {
        playbackController.onSeek(position)
    }

    fun onErrorDismissed() {
        playbackController.onErrorDismissed()
    }

    fun toggleFavorite() {
        val track = _uiState.value.selectedTrack ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track)
        }
    }

    fun startSmartShuffle(tracks: List<TrackInfo>? = null) {
        viewModelScope.launch {
            val source = tracks ?: musicLibraryRepository.getAllTracks()
            if (source.isEmpty()) return@launch
            val recentlyPlayedIds = playbackController.recentTrackIds(SMART_SHUFFLE_RECENT_EXCLUDE_COUNT)
            val shuffledTracks =
                smartShuffleGenerator.generateShuffledQueue(
                    tracks = source,
                    recentlyPlayedIds = recentlyPlayedIds,
                )
            if (shuffledTracks.isNotEmpty()) {
                onTrackSelected(shuffledTracks, 0)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionRecorder.onDestroy()
        playbackController.release()
    }

    companion object {
        private const val SMART_SHUFFLE_RECENT_EXCLUDE_COUNT = 5
    }
}
