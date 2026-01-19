package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.ArtistPlayCount
import com.anplak.androidmusic.data.PlayHistoryRepository
import com.anplak.androidmusic.data.PlayHistoryRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Track with play count for insights display.
 */
data class TrackWithPlayCount(
    val track: TrackInfo,
    val playCount: Int
)

/**
 * UI state for insights section.
 */
data class InsightsUiState(
    val isLoading: Boolean = true,
    val todayPlayTime: Long = 0,
    val weekPlayTime: Long = 0,
    val todayTopTracks: List<TrackWithPlayCount> = emptyList(),
    val weekTopTracks: List<TrackWithPlayCount> = emptyList(),
    val todayTopArtists: List<ArtistPlayCount> = emptyList(),
    val weekTopArtists: List<ArtistPlayCount> = emptyList(),
    val hasData: Boolean = false
)

class InsightsViewModel @JvmOverloads constructor(
    application: Application,
    private val playHistoryRepository: PlayHistoryRepository = PlayHistoryRepositoryImpl(
        AppDatabase.getInstance(application).playHistoryDao()
    ),
    private val trackDao: TrackDao = AppDatabase.getInstance(application).trackDao()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    /**
     * Loads all insights data.
     */
    fun loadInsights() {
        viewModelScope.launch {
            _uiState.value = InsightsUiState(isLoading = true)

            val todayStart = getStartOfToday()
            val weekStart = getStartOfWeek()

            // Combine all the flows for today and week data
            combine(
                playHistoryRepository.getTotalPlayTimeSince(todayStart),
                playHistoryRepository.getTotalPlayTimeSince(weekStart),
                playHistoryRepository.getTopTracksSince(todayStart, TOP_TRACKS_TODAY_LIMIT),
                playHistoryRepository.getTopTracksSince(weekStart, TOP_TRACKS_WEEK_LIMIT),
                playHistoryRepository.getTopArtistsSince(todayStart, TOP_ARTISTS_TODAY_LIMIT),
                playHistoryRepository.getTopArtistsSince(weekStart, TOP_ARTISTS_WEEK_LIMIT)
            ) { flows ->
                val todayPlayTime = flows[0] as Long
                val weekPlayTime = flows[1] as Long
                @Suppress("UNCHECKED_CAST")
                val todayTopTrackCounts = flows[2] as List<com.anplak.androidmusic.data.TrackPlayCount>
                @Suppress("UNCHECKED_CAST")
                val weekTopTrackCounts = flows[3] as List<com.anplak.androidmusic.data.TrackPlayCount>
                @Suppress("UNCHECKED_CAST")
                val todayTopArtists = flows[4] as List<ArtistPlayCount>
                @Suppress("UNCHECKED_CAST")
                val weekTopArtists = flows[5] as List<ArtistPlayCount>

                // Fetch track info for top tracks
                val todayTopTracks = fetchTracksWithCounts(todayTopTrackCounts)
                val weekTopTracks = fetchTracksWithCounts(weekTopTrackCounts)

                val hasData = todayPlayTime > 0 || weekPlayTime > 0 ||
                        todayTopTracks.isNotEmpty() || weekTopTracks.isNotEmpty()

                InsightsUiState(
                    isLoading = false,
                    todayPlayTime = todayPlayTime,
                    weekPlayTime = weekPlayTime,
                    todayTopTracks = todayTopTracks,
                    weekTopTracks = weekTopTracks,
                    todayTopArtists = todayTopArtists,
                    weekTopArtists = weekTopArtists,
                    hasData = hasData
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Refreshes insights data.
     */
    fun refresh() {
        loadInsights()
    }

    /**
     * Fetches track info for a list of track play counts.
     */
    private suspend fun fetchTracksWithCounts(
        trackCounts: List<com.anplak.androidmusic.data.TrackPlayCount>
    ): List<TrackWithPlayCount> {
        if (trackCounts.isEmpty()) return emptyList()

        val trackIds = trackCounts.map { it.trackId }
        val tracks = trackDao.getByIds(trackIds)
        val trackMap = tracks.associateBy { it.id }

        return trackCounts.mapNotNull { trackCount ->
            trackMap[trackCount.trackId]?.let { entity ->
                TrackWithPlayCount(
                    track = TrackInfo(
                        uri = TrackInfo.uriFromId(entity.id),
                        title = entity.title,
                        artist = entity.artist,
                        album = entity.album,
                        duration = entity.duration
                    ),
                    playCount = trackCount.playCount
                )
            }
        }
    }

    /**
     * Gets the start of today (midnight) in milliseconds.
     */
    private fun getStartOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Gets the start of the current week (Monday midnight) in milliseconds.
     */
    private fun getStartOfWeek(): Long {
        return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val TOP_TRACKS_TODAY_LIMIT = 5
        private const val TOP_TRACKS_WEEK_LIMIT = 10
        private const val TOP_ARTISTS_TODAY_LIMIT = 5
        private const val TOP_ARTISTS_WEEK_LIMIT = 10
    }
}
