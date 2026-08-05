package com.anplak.androidmusic.ui

import com.anplak.androidmusic.data.AlbumSummary
import com.anplak.androidmusic.data.ArtistSummary
import com.anplak.androidmusic.data.LibraryBrowseAggregator
import com.anplak.androidmusic.data.LibraryFilter
import com.anplak.androidmusic.data.LibraryFilterEngine
import com.anplak.androidmusic.data.LibrarySyncState
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Holds library browse/filter presentation state and builds [LibraryUiState].
 */
class LibraryUiAssembler(
    private val uiState: MutableStateFlow<LibraryUiState>,
    private val syncState: () -> LibrarySyncState,
) {
    var currentTracks: List<TrackInfo> = emptyList()
    var favoriteIds: Set<Long> = emptySet()
    var recentlyAddedIds: Set<Long> = emptySet()
    var filter: LibraryFilter = LibraryFilter()
    var localQuery: String = ""
    var isRefreshing: Boolean = false
    var syncFailed: Boolean = false
    var cachedArtists: List<ArtistSummary>? = null
    var cachedAlbums: List<AlbumSummary>? = null
    var browseTab: LibraryBrowseTab = LibraryBrowseTab.Tracks

    fun rebuildAggregates() {
        if (currentTracks.isEmpty()) {
            cachedArtists = emptyList()
            cachedAlbums = emptyList()
            return
        }
        cachedArtists = LibraryBrowseAggregator.aggregateArtists(currentTracks)
        cachedAlbums = LibraryBrowseAggregator.aggregateAlbums(currentTracks)
    }

    fun shouldApplyFilters(): Boolean {
        val loadingEmpty = uiState.value is LibraryUiState.Loading && currentTracks.isEmpty()
        return !loadingEmpty || syncState() !is LibrarySyncState.Running
    }

    fun updateSyncFlags(
        isRefreshing: Boolean,
        syncFailed: Boolean,
    ) {
        this.isRefreshing = isRefreshing
        this.syncFailed = syncFailed
        refreshFavoriteUiState()
    }

    fun refreshFavoriteUiState() {
        if (shouldApplyFilters()) {
            applyFilters()
        }
    }

    fun applyFilters() {
        if (uiState.value is LibraryUiState.Loading &&
            currentTracks.isEmpty() &&
            syncState() is LibrarySyncState.Running
        ) {
            return
        }

        if (currentTracks.isEmpty()) {
            uiState.value = LibraryUiState.Empty
            return
        }

        val filtered =
            LibraryFilterEngine.apply(
                tracks = currentTracks,
                filter = filter,
                favoriteIds = favoriteIds,
                recentlyAddedIds = recentlyAddedIds,
            ).filter { LibraryFilterEngine.matchesLocalQuery(it, localQuery) }

        uiState.value =
            LibraryUiState.Content(
                tracks = filtered,
                isRefreshing = isRefreshing,
                syncFailed = syncFailed,
                favoriteIds = favoriteIds,
                filter = filter,
                localQuery = localQuery,
                showNoFilterResults = filtered.isEmpty() && browseTab == LibraryBrowseTab.Tracks,
                browseTab = browseTab,
                artists = cachedArtists.orEmpty(),
                albums = cachedAlbums.orEmpty(),
            )
    }
}
