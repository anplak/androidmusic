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
import com.anplak.androidmusic.data.LibraryFilterEngine
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

enum class LibraryBrowseTab(@StringRes val labelResId: Int) {
    Tracks(R.string.library_tab_tracks),
    Artists(R.string.library_tab_artists),
    Albums(R.string.library_tab_albums)
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
        val albums: List<AlbumSummary> = emptyList()
    ) : LibraryUiState
    data object Empty : LibraryUiState
}

class LibraryViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val repository: MusicLibraryRepository = MusicLibraryRepositoryFactory.create(application),
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(
        AppDatabase.getInstance(application).favoriteDao(),
        AppDatabase.getInstance(application).trackDao()
    ),
    private val trackDao: TrackDao = AppDatabase.getInstance(application).trackDao(),
    private val syncCoordinator: LibrarySyncCoordinator = LibrarySyncCoordinatorFactory.get(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _scanSummary = MutableStateFlow<LibraryScanResult?>(null)
    val scanSummary: StateFlow<LibraryScanResult?> = _scanSummary.asStateFlow()

    private var currentTracks: List<TrackInfo> = emptyList()
    private var favoriteIds: Set<Long> = emptySet()
    private var recentlyAddedIds: Set<Long> = emptySet()
    private var filter: LibraryFilter = LibraryFilter()
    private var localQuery: String = ""
    private var isRefreshing: Boolean = false
    private var syncFailed: Boolean = false
    private var cachedArtists: List<ArtistSummary>? = null
    private var cachedAlbums: List<AlbumSummary>? = null
    private var browseTab: LibraryBrowseTab = savedStateHandle.get<String>(KEY_BROWSE_TAB)
        ?.let { runCatching { LibraryBrowseTab.valueOf(it) }.getOrNull() }
        ?: LibraryBrowseTab.Tracks

    init {
        viewModelScope.launch {
            favoritesRepository.getAllFavoriteIds().collect { ids ->
                favoriteIds = ids
                if (shouldApplyFilters()) {
                    applyFilters()
                }
            }
        }

        viewModelScope.launch {
            repository.observeCachedTracks().collect { cached ->
                currentTracks = cached
                recentlyAddedIds = loadRecentlyAddedIds()
                cachedArtists = null
                cachedAlbums = null
                if (shouldApplyFilters()) {
                    rebuildAggregates()
                    applyFilters()
                }
            }
        }

        viewModelScope.launch {
            syncCoordinator.syncState.collect { state ->
                when (state) {
                    LibrarySyncState.Idle -> Unit
                    LibrarySyncState.Running -> updateSyncFlags(isRefreshing = true, syncFailed = false)
                    is LibrarySyncState.Success -> {
                        updateSyncFlags(isRefreshing = false, syncFailed = false)
                        _scanSummary.value = state.result
                    }
                    is LibrarySyncState.Failed -> updateSyncFlags(isRefreshing = false, syncFailed = true)
                }
            }
        }

        viewModelScope.launch {
            if (repository.getCachedTracks().isEmpty()) {
                _uiState.value = LibraryUiState.Loading
                syncCoordinator.syncNow()
            } else {
                currentTracks = repository.getCachedTracks()
                rebuildAggregates()
                applyFilters()
                syncCoordinator.scheduleSync()
            }
        }
    }

    fun onLibraryVisible() {
        syncCoordinator.scheduleSync()
        viewModelScope.launch {
            favoriteIds = favoritesRepository.getAllFavoriteIds().first()
            refreshFavoriteUiState()
        }
    }

    fun setBrowseTab(tab: LibraryBrowseTab) {
        browseTab = tab
        savedStateHandle[KEY_BROWSE_TAB] = tab.name
        applyFilters()
    }

    fun setFilter(newFilter: LibraryFilter) {
        filter = newFilter
        applyFilters()
    }

    fun setLocalQuery(query: String) {
        localQuery = query
        applyFilters()
    }

    fun applyLibraryHint(query: String) {
        localQuery = query
        browseTab = LibraryBrowseTab.Tracks
        savedStateHandle[KEY_BROWSE_TAB] = browseTab.name
        applyFilters()
    }

    fun tracksForArtist(normalizedKey: String): List<TrackInfo> =
        LibraryBrowseAggregator.tracksForArtist(currentTracks, normalizedKey)

    fun tracksForAlbum(summary: AlbumSummary): List<TrackInfo> =
        LibraryBrowseAggregator.tracksForAlbum(
            currentTracks,
            summary.normalizedTitle,
            summary.normalizedArtist
        )

    fun toggleFavorite(trackId: Long) {
        val track = currentTracks.find { it.id == trackId } ?: return
        favoriteIds = if (trackId in favoriteIds) {
            favoriteIds - trackId
        } else {
            favoriteIds + trackId
        }
        applyFilters()
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            syncFailed = false
            if (currentTracks.isEmpty()) {
                _uiState.value = LibraryUiState.Loading
            } else {
                updateSyncFlags(isRefreshing = true, syncFailed = false)
            }
            syncCoordinator.syncNow()
        }
    }

    fun clearScanSummary() {
        _scanSummary.value = null
    }

    private suspend fun loadRecentlyAddedIds(): Set<Long> {
        val sinceMs = System.currentTimeMillis() - RECENTLY_ADDED_WINDOW_MS
        return trackDao.getTracksAddedSince(sinceMs).map { it.id }.toSet()
    }

    private fun rebuildAggregates() {
        if (currentTracks.isEmpty()) {
            cachedArtists = emptyList()
            cachedAlbums = emptyList()
            return
        }
        cachedArtists = LibraryBrowseAggregator.aggregateArtists(currentTracks)
        cachedAlbums = LibraryBrowseAggregator.aggregateAlbums(currentTracks)
    }

    private fun shouldApplyFilters(): Boolean {
        if (_uiState.value !is LibraryUiState.Loading) return true
        if (currentTracks.isNotEmpty()) return true
        return syncCoordinator.syncState.value !is LibrarySyncState.Running
    }

    private fun updateSyncFlags(isRefreshing: Boolean, syncFailed: Boolean) {
        this.isRefreshing = isRefreshing
        this.syncFailed = syncFailed
        refreshFavoriteUiState()
    }

    private fun refreshFavoriteUiState() {
        if (shouldApplyFilters()) {
            applyFilters()
        }
    }

    private fun applyFilters() {
        if (_uiState.value is LibraryUiState.Loading &&
            currentTracks.isEmpty() &&
            syncCoordinator.syncState.value is LibrarySyncState.Running
        ) {
            return
        }

        if (currentTracks.isEmpty()) {
            _uiState.value = LibraryUiState.Empty
            return
        }

        val filtered = LibraryFilterEngine.apply(
            tracks = currentTracks,
            filter = filter,
            favoriteIds = favoriteIds,
            recentlyAddedIds = recentlyAddedIds
        ).filter { LibraryFilterEngine.matchesLocalQuery(it, localQuery) }

        _uiState.value = LibraryUiState.Content(
            tracks = filtered,
            isRefreshing = isRefreshing,
            syncFailed = syncFailed,
            favoriteIds = favoriteIds,
            filter = filter,
            localQuery = localQuery,
            showNoFilterResults = filtered.isEmpty() && browseTab == LibraryBrowseTab.Tracks,
            browseTab = browseTab,
            artists = cachedArtists.orEmpty(),
            albums = cachedAlbums.orEmpty()
        )
    }

    companion object {
        const val KEY_BROWSE_TAB = "library_browse_tab"
        private const val RECENTLY_ADDED_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}
