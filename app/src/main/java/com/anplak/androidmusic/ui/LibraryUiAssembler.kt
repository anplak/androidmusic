package com.anplak.androidmusic.ui

import com.anplak.androidmusic.data.AlbumSummary
import com.anplak.androidmusic.data.ArtistSummary
import com.anplak.androidmusic.data.LibraryBrowseAggregator
import com.anplak.androidmusic.data.LibraryFilter
import com.anplak.androidmusic.data.LibraryFilterEngine
import com.anplak.androidmusic.data.LibrarySyncState
import com.anplak.androidmusic.data.SearchCollectionMatch
import com.anplak.androidmusic.data.SearchEngine
import com.anplak.androidmusic.data.SearchRawResults
import com.anplak.androidmusic.data.SearchResultItem
import com.anplak.androidmusic.data.SearchResultKind
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Holds library browse/filter presentation state and builds [LibraryUiState].
 */
class LibraryUiAssembler(
    private val uiState: MutableStateFlow<LibraryUiState>,
    private val syncState: () -> LibrarySyncState,
    private val searchEngine: SearchEngine,
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

        val filteredBase =
            LibraryFilterEngine.apply(
                tracks = currentTracks,
                filter = filter,
                favoriteIds = favoriteIds,
                recentlyAddedIds = recentlyAddedIds,
            )
        val filtered = filteredBase.filter { LibraryFilterEngine.matchesLocalQuery(it, localQuery) }
        val localSearchResults =
            localQuery.takeIf { it.isNotBlank() }?.let { query ->
                searchEngine.buildGrouped(
                    query = query,
                    raw =
                        SearchRawResults(
                            tracks = filtered,
                            playlists = emptyList(),
                            history = emptyList(),
                            collectionMatches =
                                SearchCollectionMatch(
                                    artists =
                                        LibraryBrowseAggregator.aggregateArtists(filteredBase)
                                            .filter { it.displayName.contains(query, ignoreCase = true) }
                                            .map { artist ->
                                                SearchResultItem(
                                                    id = "artist:${artist.normalizedKey}",
                                                    kind = SearchResultKind.ARTIST,
                                                    title = artist.displayName,
                                                    subtitle = "${artist.trackCount} tracks",
                                                    artistKey = artist.normalizedKey,
                                                )
                                            },
                                    albums =
                                        LibraryBrowseAggregator.aggregateAlbums(filteredBase)
                                            .filter {
                                                it.displayTitle.contains(query, ignoreCase = true) ||
                                                    it.displayArtist.contains(query, ignoreCase = true)
                                            }
                                            .map { album ->
                                                SearchResultItem(
                                                    id = "album:${album.normalizedTitle}:${album.normalizedArtist}",
                                                    kind = SearchResultKind.ALBUM,
                                                    title = album.displayTitle,
                                                    subtitle = album.displayArtist,
                                                    albumTitle = album.displayTitle,
                                                    albumArtist = album.displayArtist,
                                                )
                                            },
                                ),
                        ),
                )
            }

        uiState.value =
            LibraryUiState.Content(
                tracks = filtered,
                isRefreshing = isRefreshing,
                syncFailed = syncFailed,
                favoriteIds = favoriteIds,
                filter = filter,
                localQuery = localQuery,
                showNoFilterResults =
                    browseTab == LibraryBrowseTab.Tracks &&
                        localSearchResults?.totalCount == 0 &&
                        localQuery.isNotBlank(),
                browseTab = browseTab,
                artists = cachedArtists.orEmpty(),
                albums = cachedAlbums.orEmpty(),
                localSearchResults = localSearchResults,
            )
    }
}
