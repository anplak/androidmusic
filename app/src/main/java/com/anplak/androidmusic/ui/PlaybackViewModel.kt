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
import com.anplak.androidmusic.player.PlaybackSession
import com.anplak.androidmusic.player.PlaybackSessionClassifier
import com.anplak.androidmusic.player.PlaybackStatsTracker
import com.anplak.androidmusic.player.PlayStartReason
import com.anplak.androidmusic.player.SessionOutcome
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
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(
        database.favoriteDao(),
        database.trackDao()
    )
    private val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl(database.playlistDao())
    private val trackStatsRepository: TrackStatsRepository = TrackStatsRepositoryImpl(database.trackStatsDao())
    private val playHistoryRepository: PlayHistoryRepository = PlayHistoryRepositoryImpl(database.playHistoryDao())
    private val musicLibraryRepository: MusicLibraryRepository = MusicLibraryRepositoryFactory.create(application)
    private val smartShuffleGenerator = SmartShuffleGenerator(favoritesRepository, trackStatsRepository)
    private val playbackStatsTracker = PlaybackStatsTracker()

    private var queue = PlaybackQueue.EMPTY

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val currentTrackIdFlow = MutableStateFlow<Long?>(null)

    private var lastTrackedTrackId: Long? = null
    private var lastTrackWasPlaying: Boolean = false
    private var lastPosition: Long = 0L
    private var lastDuration: Long = 0L

    private var currentSessionId: String = UUID.randomUUID().toString()

    init {
        audioPlayer.connect()

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
                trackPlaybackStats(state)
                _uiState.value = state
                    currentTrackIdFlow.value = state.selectedTrack?.id
            }
        }
    }

    private fun trackPlaybackStats(state: PlaybackUiState) {
        val currentTrackId = state.selectedTrack?.id
        val nowMs = System.currentTimeMillis()

        if (lastTrackedTrackId != null &&
            lastTrackWasPlaying &&
            lastDuration > 0 &&
            currentTrackId != lastTrackedTrackId
        ) {
            val completionThreshold = lastDuration * 0.9
            if (lastPosition >= completionThreshold) {
                viewModelScope.launch {
                    lastTrackedTrackId?.let { trackStatsRepository.recordCompletion(it) }
                }
            }
        }

        if (currentTrackId != lastTrackedTrackId) {
            val finalizedSession = playbackStatsTracker.onTrackChanged(currentTrackId, nowMs)
            finalizedSession?.let { session ->
                viewModelScope.launch {
                    finalizeSession(session, nowMs)
                }
            }

            playbackStatsTracker.currentSession()?.let { session ->
                if (session.startReason == PlayStartReason.EXPLICIT) {
                    viewModelScope.launch {
                        recordQualifiedPlay(session)
                    }
                }
            }
            lastTrackedTrackId = currentTrackId
        } else if (currentTrackId != null) {
            playbackStatsTracker.updatePlaybackProgress(
                positionMs = state.currentPosition,
                durationMs = state.duration
            )
        }

        lastTrackWasPlaying = state.isPlaying
        lastPosition = state.currentPosition
        lastDuration = state.duration
    }

    private suspend fun finalizeSession(
        session: PlaybackSession,
        nowMs: Long
    ) {
        val listenedMs = PlaybackSessionClassifier.listenedMs(
            session,
            lastPositionMs = session.maxPositionMs,
            nowMs = nowMs
        )
        when (PlaybackSessionClassifier.classify(session, listenedMs, session.durationMs)) {
            SessionOutcome.AlreadyRecorded -> Unit
            SessionOutcome.QualifiedPlay -> recordQualifiedPlay(session)
            SessionOutcome.FastSkip -> {
                if (!session.skipRecorded) {
                    trackStatsRepository.recordSkip(session.trackId)
                }
            }
            SessionOutcome.NoOp -> Unit
        }
    }

    private suspend fun recordQualifiedPlay(session: PlaybackSession) {
        if (session.qualifiedPlayRecorded) return
        trackStatsRepository.recordQualifiedPlay(session.trackId, session.startedAtMs)
        session.qualifiedPlayRecorded = true
        recordQualifiedHistory(session)
    }

    private suspend fun recordQualifiedHistory(session: PlaybackSession) {
        if (session.historyRecorded) return
        playHistoryRepository.recordPlay(session.trackId, currentSessionId)
        session.historyRecorded = true
    }

    fun onTrackSelected(tracks: List<TrackInfo>, selectedIndex: Int) {
        playbackStatsTracker.markExplicitStart()
        queue = PlaybackQueue.fromLibrary(tracks, selectedIndex)
        audioPlayer.setQueue(tracks, selectedIndex)
    }

    fun onTrackSelected(track: TrackInfo) {
        onTrackSelected(listOf(track), 0)
    }

    fun clearTrack() {
        val finalized = playbackStatsTracker.onTrackChanged(null)
        finalized?.let { session ->
            viewModelScope.launch {
                finalizeSession(session, System.currentTimeMillis())
            }
        }
        queue = PlaybackQueue.EMPTY
        audioPlayer.stop()
        lastTrackedTrackId = null
    }

    fun onPlayPause() {
        if (_uiState.value.isPlaying) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }

    fun onNext() {
        recordSkipOnManualAdvance()
        playbackStatsTracker.markAutoAdvance()
        audioPlayer.next()
    }

    fun onPrevious() {
        recordSkipOnManualAdvance()
        playbackStatsTracker.markAutoAdvance()
        audioPlayer.previous()
    }

    private fun recordSkipOnManualAdvance() {
        val session = playbackStatsTracker.currentSession() ?: return
        if (session.startReason != PlayStartReason.AUTO_ADVANCE ||
            session.qualifiedPlayRecorded ||
            session.skipRecorded
        ) {
            return
        }
        session.skipRecorded = true
        viewModelScope.launch {
            trackStatsRepository.recordSkip(session.trackId)
        }
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
            favoritesRepository.toggleFavorite(track)
        }
    }

    fun addToPlaylist(playlistId: Long) {
        val track = _uiState.value.selectedTrack ?: return
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun startSmartShuffle() {
        viewModelScope.launch {
            val allTracks = musicLibraryRepository.getAllTracks()
            if (allTracks.isEmpty()) return@launch

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
        playbackStatsTracker.currentSession()?.let { session ->
            viewModelScope.launch {
                finalizeSession(session, System.currentTimeMillis())
            }
        }
        audioPlayer.release()
    }

    companion object {
        private const val SMART_SHUFFLE_RECENT_EXCLUDE_COUNT = 5
    }
}
