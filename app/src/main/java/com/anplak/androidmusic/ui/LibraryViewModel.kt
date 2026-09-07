package com.anplak.androidmusic.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.AlbumSummary
import com.anplak.androidmusic.data.ArtistSummary
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.LibraryBrowseAggregator
import com.anplak.androidmusic.data.LibraryFilter
import com.anplak.androidmusic.data.LibraryScanResult
import com.anplak.androidmusic.data.LibrarySyncCoordinator
import com.anplak.androidmusic.data.LibrarySyncCoordinatorFactory
import com.anplak.androidmusic.data.LibrarySyncState
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.MusicLibraryRepositoryFactory
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class LibraryBrowseTab(
    @StringRes val labelResId: Int,
) {
    Tracks(R.string.library_tab_tracks),
    Artists(R.string.library_tab_artists),
    Albums(R.string.library_tab_albums),
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Content(
        val tracks: List<TrackInfo>,
        val isRefreshing: Boolean = false,
        val syncFailed: Boolean = false,
        val favoriteIds: Set<Long> = emptySet(),
        val filter: LibraryFilter = LibraryFilter(),
        val localQuery: String = "",
        val showNoFilterResults: Boolean = false,
        val browseTab: LibraryBrowseTab = LibraryBrowseTab.Tracks,
        val artists: List<ArtistSummary> = emptyList(),
        val albums: List<AlbumSummary> = emptyList(),
    ) : LibraryUiState

    data object Empty : LibraryUiState
}

class LibraryViewModel
    @JvmOverloads
    constructor(
        application: Application,
        private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
        private val repository: MusicLibraryRepository = MusicLibraryRepositoryFactory.create(application),
        private val favoritesRepository: FavoritesRepository =
            FavoritesRepositoryImpl(
                AppDatabase.getInstance(application).favoriteDao(),
                AppDatabase.getInstance(application).trackDao(),
            ),
        private val trackDao: TrackDao = AppDatabase.getInstance(application).trackDao(),
        private val syncCoordinator: LibrarySyncCoordinator = LibrarySyncCoordinatorFactory.get(application),
    ) : AndroidViewModel(application) {
        private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
        val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

        private val _scanSummary = MutableStateFlow<LibraryScanResult?>(null)
        val scanSummary: StateFlow<LibraryScanResult?> = _scanSummary.asStateFlow()

        private val assembler =
            LibraryUiAssembler(
                uiState = _uiState,
                syncState = { syncCoordinator.syncState.value },
            ).also { assembler ->
                assembler.browseTab =
                    savedStateHandle.get<String>(KEY_BROWSE_TAB)
                        ?.let { runCatching { LibraryBrowseTab.valueOf(it) }.getOrNull() }
                        ?: LibraryBrowseTab.Tracks
            }

        init {
            viewModelScope.launch {
                favoritesRepository.getAllFavoriteIds().collect { ids ->
                    assembler.favoriteIds = ids
                    if (assembler.shouldApplyFilters()) {
                        assembler.applyFilters()
                    }
                }
            }

            viewModelScope.launch {
                repository.observeCachedTracks().collect { cached ->
                    assembler.currentTracks = cached
                    assembler.recentlyAddedIds =
                        run {
                            val sinceMs = System.currentTimeMillis() - RECENTLY_ADDED_WINDOW_MS
                            trackDao.getTracksAddedSince(sinceMs).map { it.id }.toSet()
                        }
                    assembler.cachedArtists = null
                    assembler.cachedAlbums = null
                    if (assembler.shouldApplyFilters()) {
                        assembler.rebuildAggregates()
                        assembler.applyFilters()
                    }
                }
            }

            viewModelScope.launch {
                syncCoordinator.syncState.collect { state ->
                    when (state) {
                        LibrarySyncState.Idle -> Unit
                        LibrarySyncState.Running ->
                            assembler.updateSyncFlags(isRefreshing = true, syncFailed = false)
                        is LibrarySyncState.Success -> {
                            assembler.updateSyncFlags(isRefreshing = false, syncFailed = false)
                            _scanSummary.value = state.result
                        }
                        is LibrarySyncState.Failed ->
                            assembler.updateSyncFlags(isRefreshing = false, syncFailed = true)
                    }
                }
            }

            viewModelScope.launch {
                if (repository.getCachedTracks().isEmpty()) {
                    _uiState.value = LibraryUiState.Loading
                    syncCoordinator.syncNow()
                } else {
                    assembler.currentTracks = repository.getCachedTracks()
                    assembler.rebuildAggregates()
                    assembler.applyFilters()
                    syncCoordinator.scheduleSync()
                }
            }
        }

        fun onLibraryVisible() {
            syncCoordinator.scheduleSync()
            viewModelScope.launch {
                assembler.favoriteIds = favoritesRepository.getAllFavoriteIds().first()
                assembler.refreshFavoriteUiState()
            }
        }

        fun setBrowseTab(tab: LibraryBrowseTab) {
            assembler.browseTab = tab
            savedStateHandle[KEY_BROWSE_TAB] = tab.name
            assembler.applyFilters()
        }

        fun setFilter(newFilter: LibraryFilter) {
            assembler.filter = newFilter
            assembler.applyFilters()
        }

        fun setLocalQuery(query: String) {
            assembler.localQuery = query
            assembler.applyFilters()
        }

        fun applyLibraryHint(query: String) {
            assembler.localQuery = query
            assembler.browseTab = LibraryBrowseTab.Tracks
            savedStateHandle[KEY_BROWSE_TAB] = assembler.browseTab.name
            assembler.applyFilters()
        }

        fun tracksForArtist(normalizedKey: String): List<TrackInfo> =
            LibraryBrowseAggregator.tracksForArtist(assembler.currentTracks, normalizedKey)

        fun tracksForAlbum(summary: AlbumSummary): List<TrackInfo> =
            LibraryBrowseAggregator.tracksForAlbum(
                assembler.currentTracks,
                summary.normalizedTitle,
                summary.normalizedArtist,
            )

        fun toggleFavorite(trackId: Long) {
            val track = assembler.currentTracks.find { it.id == trackId } ?: return
            assembler.favoriteIds =
                if (trackId in assembler.favoriteIds) {
                    assembler.favoriteIds - trackId
                } else {
                    assembler.favoriteIds + trackId
                }
            assembler.applyFilters()
            viewModelScope.launch {
                favoritesRepository.toggleFavorite(track)
            }
        }

        fun refresh() {
            viewModelScope.launch {
                assembler.syncFailed = false
                if (assembler.currentTracks.isEmpty()) {
                    _uiState.value = LibraryUiState.Loading
                } else {
                    assembler.updateSyncFlags(isRefreshing = true, syncFailed = false)
                }
                syncCoordinator.syncNow()
            }
        }

        fun clearScanSummary() {
            _scanSummary.value = null
        }

        companion object {
            const val KEY_BROWSE_TAB = "library_browse_tab"
            private const val RECENTLY_ADDED_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
        }
    }
