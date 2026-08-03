### User story

As a user searching my library, I want matching **tracks, artists, and albums** to appear as distinct results so I can open the exact collection I meant instead of filtering a flat track list.

### Description

Global Search already displays Tracks, Artists, and Albums, but Artist and Album rows are derived from every matching track. A title-only track match can therefore produce an unrelated artist or album suggestion. Selecting a collection also returns to the Library Tracks tab with a text filter instead of opening that Artist or Album. This story makes collection matching explicit and routes collection results to their existing detail screens.

**Estimate:** 2–3 days.

**Prerequisites:** stories **09** (Search) and **13** (Artist/Album browsing). Compatible with story **20** artwork.

### Acceptance criteria

- **AC1:** A query that matches a track title shows that track in **Tracks** without automatically showing its artist or album unless the artist/album name also matches.
- **AC2:** A case-insensitive partial match on an artist name shows the artist in an **Artists** section.
- **AC3:** A case-insensitive partial match on an album title shows the album in an **Albums** section, with artist text to disambiguate duplicate titles.
- **AC4:** Selecting an Artist result opens `LibraryArtistDetailScreen` for that exact normalized artist.
- **AC5:** Selecting an Album result opens `LibraryAlbumDetailScreen` for that exact album-and-artist identity.
- **AC6:** Track, playlist, and history result behavior remains unchanged.
- **AC7:** Empty, loading, recent-search, and no-result states remain understandable and do not flash stale results while a new query is running.

### Functional requirements

- **FR1:** Search output must carry stable collection identity, not only display text:
  - Artist: normalized artist key and display name.
  - Album: normalized album title, normalized artist, display title, and display artist.
- **FR2:** Artist results must be built only from artists whose name matches the normalized query.
- **FR3:** Album results must be built only from albums whose title matches the normalized query; artist-name matching may additionally return that artist's albums only if this behavior is explicitly labeled and unit-tested.
- **FR4:** Deduplicate Artist and Album results with the same normalization rules used by `LibraryBrowseAggregator`.
- **FR5:** `SearchScreen` must expose separate callbacks for Artist and Album selection instead of converting both to a Library text query.
- **FR6:** `MusicPlayerApp` must route those callbacks to existing `AppScreen.LibraryArtistDetail` and `AppScreen.LibraryAlbumDetail` destinations.
- **FR7:** Preserve grouped sections and current result limits; a missing section is omitted rather than rendered empty.

### Non-functional requirements

- **NFR1:** Keep the existing debounced search behavior and perform search/aggregation off the main thread.
- **NFR2:** Search should remain responsive for a cached library of at least 10,000 tracks; avoid a new database query per displayed result.
- **NFR3:** Use one canonical normalization implementation for Search and Library browsing to avoid results that cannot be opened.
- **NFR4:** No Room migration is required unless profiling proves aggregate queries are necessary.
- **NFR5:** Preserve existing Search test tags where behavior is unchanged and add stable tags for Artist and Album rows.

### UX design

- Keep the current grouped order: **Tracks, Artists, Albums, Playlists, History**.
- Artist rows show the artist name and optional track/album count.
- Album rows show album title as primary text and artist as secondary text.
- Use collection artwork from story **20** when available; a missing image must use the shared fallback.
- A result row opens on one tap. Do not add nested action icons or an intermediate choice dialog.
- Returning from a collection detail screen should return to the Search results and preserve the query where the current navigation model permits; at minimum, Back must not leave the user on an unrelated Library filter.

### Testing/validation strategy

- **Unit — matching:** title-only track match does not create false Artist/Album results; artist and album partial matches are case-insensitive.
- **Unit — identity:** artists with case/whitespace variants deduplicate; same album title by different artists remains two distinct results.
- **Unit — navigation payload:** result models retain normalized keys required by `LibraryBrowseAggregator`.
- **Compose/UI:** grouped section visibility, Artist and Album row tags, loading/no-result states, and one-tap callbacks.
- **E2E:** Search from Library → type artist → open artist detail; repeat for album; Back behavior is coherent.
- **Regression:** selecting Tracks still starts playback, Playlist opens detail, and History replays the track.
- **Performance:** manually search a large seeded library while typing rapidly; no main-thread stall or stale result replacement.

### Out of scope

- Fuzzy matching, typo correction, phonetic search, and transliteration.
- Lyrics, genre, year, language, or internet search.
- A new full-text-search engine or Room FTS migration.
- Redesigning Library's local track-filter field.
- Adding collections to playlists; owned by story **22**.

---

### Overall app state after this story

Library Search produces trustworthy collection-aware results and opens the selected Artist or Album directly.

### Value added after this story

Search becomes a navigation tool for the library hierarchy, not only a flat track filter.

---

## Implementation Plan

### Overview

The core change is to make search results for **Artists** and **Albums** independent from tracks. Currently, `SearchEngine.buildGrouped()` extracts artists and albums from *every matching track*. This story adds explicit artist/album queries that only return results when the collection name itself matches the query.

### Key Decisions

1. **Canonical normalization**: Use `LibraryIndexFilter.normalizeArtist()` for all search normalization to avoid discrepancies between Search and Library browsing.
2. **Off-main-thread aggregation**: Use `SearchRepository` to fetch raw data, then perform aggregation in `SearchEngine` on a background thread via `viewModelScope`.
3. **No database changes**: Leverage existing `searchTracks()` query; aggregate artists/albums in memory. Profiling should determine if dedicated aggregate queries are needed (NFR4).
4. **Stable result identity**: Extend `SearchResultItem` to carry normalized keys for Artists/Albums so navigation payloads are correct.

---

### Step 1: Extend `SearchResultItem` for collection identity

**File**: `app/src/main/java/com/anplak/androidmusic/data/SearchModels.kt`

```kotlin
data class SearchResultItem(
    val id: String,
    val kind: SearchResultKind,
    val title: String,
    val subtitle: String? = null,
    val trackId: Long? = null,
    val playlistId: Long? = null,
    val historyId: Long? = null,
    // NEW: normalized keys for collection navigation
    val artistNormalizedKey: String? = null,
    val albumNormalizedTitle: String? = null,
    val albumNormalizedArtist: String? = null
)
```

**Rationale**: FR1 requires stable identity. Carrying normalized keys in the result model ensures that selecting an Artist/Album produces the exact same key used by `LibraryBrowseAggregator`.

---

### Step 2: Update `SearchRepository` to provide artist/album data

**File**: `app/src/main/java/com/anplak/androidmusic/data/SearchRepository.kt`

```kotlin
data class SearchRawResults(
    val tracks: List<TrackInfo>,
    val playlists: List<Playlist>,
    val history: List<PlayHistoryEntry>,
    val artists: List<ArtistSummary>,   // NEW
    val albums: List<AlbumSummary>      // NEW
)
```

Add repository methods:

```kotlin
interface SearchRepository {
    suspend fun searchAll(query: String): SearchRawResults
    suspend fun getSuggestions(): List<String>
    suspend fun searchArtists(query: String, limit: Int): List<ArtistSummary>
    suspend fun searchAlbums(query: String, limit: Int): List<AlbumSummary>
}
```

Implementation (`SearchRepositoryImpl`):

```kotlin
override suspend fun searchArtists(query: String, limit: Int): List<ArtistSummary> {
    val normalizedQuery = LibraryIndexFilter.normalizeArtist(query)
    val tracks = trackDao.searchTracks(query, TRACK_LIMIT)
    return tracks
        .asSequence()
        .map { it.toTrackInfo() }
        .filter { LibraryIndexFilter.normalizeArtist(it.artist).contains(normalizedQuery) }
        .map { ArtistSummary(
            displayName = it.artist.trim().ifBlank { LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL },
            normalizedKey = LibraryIndexFilter.normalizeArtist(it.artist),
            trackCount = 1,
            artworkUri = it.artworkUri
        ) }
        .distinctBy { it.normalizedKey }
        .sortedBy { it.displayName.lowercase() }
        .take(limit)
        .toList()
}

override suspend fun searchAlbums(query: String, limit: Int): List<AlbumSummary> {
    val normalizedQuery = LibraryIndexFilter.normalizeArtist(query)
    val tracks = trackDao.searchTracks(query, TRACK_LIMIT)
    val homonyms = findHomonymAlbumTitles(tracks.map { it.toTrackInfo() })
    
    return tracks
        .asSequence()
        .map { it.toTrackInfo() }
        .filter { LibraryIndexFilter.normalizeArtist(it.album).contains(normalizedQuery) }
        .map { track ->
            val title = track.album.trim().ifBlank { LibraryBrowseAggregator.UNKNOWN_ALBUM }
            val artist = track.artist.trim().ifBlank { LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL }
            val needsArtist = homonyms.contains(LibraryIndexFilter.normalizeArtist(title))
            AlbumSummary(
                displayTitle = title,
                displayArtist = artist,
                normalizedTitle = LibraryIndexFilter.normalizeArtist(title),
                normalizedArtist = if (needsArtist) LibraryIndexFilter.normalizeArtist(artist) else "",
                trackCount = 1,
                artworkUri = track.artworkUri
            )
        }
        .distinctBy { 
            if (it.normalizedArtist.isEmpty()) it.normalizedTitle 
            else "${it.normalizedTitle}|${it.normalizedArtist}"
        }
        .sortedWith(compareBy({ it.displayTitle.lowercase() }, { it.displayArtist.lowercase() }))
        .take(limit)
        .toList()
}

private fun findHomonymAlbumTitles(tracks: List<TrackInfo>): Set<String> =
    tracks.groupBy { LibraryIndexFilter.normalizeArtist(it.album) }
        .filter { (_, group) -> group.map { LibraryIndexFilter.normalizeArtist(it.artist) }.toSet().size > 1 }
        .keys
```

**Rationale**: FR2/FR3 require artists/albums only when their name matches. We reuse `LibraryBrowseAggregator` homonym logic (FR4) and `LibraryIndexFilter.normalizeArtist` (NFR3). Off-main-thread via coroutine scope.

---

### Step 3: Update `SearchEngine.buildGrouped()` to use explicit artist/album results

**File**: `app/src/main/java/com/anplak/androidmusic/data/SearchEngine.kt`

```kotlin
class SearchEngine {

    fun buildGrouped(query: String, raw: SearchRawResults): GroupedSearchResults {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return GroupedSearchResults(emptyList(), 0)

        val sections = listOfNotNull(
            section(SECTION_TRACKS, raw.tracks.take(MAX_TRACKS).map { trackItem(it) }),
            section(SECTION_ARTISTS, raw.artists.take(MAX_ARTISTS).map { artistItem(it) })
                .takeIf { it.items.isNotEmpty() },
            section(SECTION_ALBUMS, raw.albums.take(MAX_ALBUMS).map { albumItem(it) })
                .takeIf { it.items.isNotEmpty() },
            section(SECTION_PLAYLISTS, raw.playlists.take(MAX_PLAYLISTS).map { playlistItem(it) })
                .takeIf { it.items.isNotEmpty() },
            section(SECTION_HISTORY, raw.history.take(MAX_HISTORY).map { historyItem(it) })
                .takeIf { it.items.isNotEmpty() }
        )

        return GroupedSearchResults(
            sections = sections,
            totalCount = sections.sumOf { it.items.size }
        )
    }

    private fun artistItem(artist: ArtistSummary): SearchResultItem {
        return SearchResultItem(
            id = "artist:${artist.normalizedKey}",
            kind = SearchResultKind.ARTIST,
            title = artist.displayName,
            subtitle = "${artist.trackCount} tracks",
            artistNormalizedKey = artist.normalizedKey
        )
    }

    private fun albumItem(album: AlbumSummary): SearchResultItem {
        return SearchResultItem(
            id = "album:${album.normalizedTitle}|${album.normalizedArtist}",
            kind = SearchResultKind.ALBUM,
            title = album.displayTitle,
            subtitle = album.displayArtist,
            albumNormalizedTitle = album.normalizedTitle,
            albumNormalizedArtist = album.normalizedArtist
        )
    }

    companion object {
        // ... existing constants
    }
}
```

**Rationale**: FR5 requires separate Artist/Album callbacks; FR7 preserves grouped sections. Now `SearchResultItem` carries normalized keys for direct navigation.

---

### Step 4: Update `SearchViewModel` to perform aggregation

**File**: `app/src/main/java/com/anplak/androidmusic/ui/SearchViewModel.kt`

```kotlin
private fun performSearch(q: String) {
    val trimmed = q.trim()
    if (trimmed.isEmpty()) {
        _uiState.value = SearchUiState.Idle(recentQueries.value, suggestions)
        return
    }
    if (trimmed.length < MIN_QUERY_LENGTH) {
        _uiState.value = SearchUiState.Idle(recentQueries.value, suggestions)
        return
    }

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        _uiState.value = SearchUiState.Searching
        runCatching {
            val raw = repository.searchAll(trimmed)
            trackById = raw.tracks.associateBy { it.id } +
                raw.history.associate { it.trackId to it.track }
            historyById = raw.history.associateBy { it.id }
            
            // NEW: aggregate artists/albums from raw tracks
            val allTracks = raw.tracks + raw.history.map { it.track }
            val artists = LibraryBrowseAggregator.aggregateArtists(allTracks)
                .filter { LibraryIndexFilter.normalizeArtist(it.displayName).contains(LibraryIndexFilter.normalizeArtist(trimmed)) }
                .take(MAX_ARTISTS)
            val albums = LibraryBrowseAggregator.aggregateAlbums(allTracks)
                .filter { LibraryIndexFilter.normalizeArtist(it.displayTitle).contains(LibraryIndexFilter.normalizeArtist(trimmed)) }
                .take(MAX_ALBUMS)
            
            val grouped = engine.buildGrouped(trimmed, raw.copy(
                artists = artists,
                albums = albums
            ))
            _uiState.value = when {
                grouped.totalCount == 0 -> SearchUiState.NoResults
                else -> SearchUiState.Results(grouped)
            }
        }.onFailure { error ->
            _uiState.value = SearchUiState.Error(
                error.message ?: "Search failed"
            )
        }
    }
}
```

**Rationale**: NFR1 keeps debounced search; NFR2 reuses existing cached tracks. Aggregation happens off-main-thread in `viewModelScope`.

---

### Step 5: Update `SearchViewModel.libraryQueryForItem()` to support direct navigation

**File**: `app/src/main/java/com/anplak/androidmusic/ui/SearchViewModel.kt`

```kotlin
fun libraryQueryForItem(item: SearchResultItem): String? {
    return when (item.kind) {
        SearchResultKind.ARTIST -> item.artistNormalizedKey?.let { "artist:$it" }
        SearchResultKind.ALBUM -> {
            val title = item.albumNormalizedTitle ?: return null
            val artist = item.albumNormalizedArtist ?: ""
            if (artist.isEmpty()) "album:$title" else "album:$title|$artist"
        }
        else -> null
    }
}
```

**Rationale**: FR6 requires routing Artist/Album selections to existing detail screens. The normalized key payload matches what `LibraryBrowseAggregator` expects.

---

### Step 6: Update `SearchScreen` to expose separate callbacks

**File**: `app/src/main/java/com/anplak/androidmusic/ui/SearchScreen.kt`

```kotlin
fun SearchScreen(
    onBackClick: () -> Unit,
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onArtistSelected: (String, String) -> Unit,  // NEW: normalizedKey, displayName
    onAlbumSelected: (AlbumSummary) -> Unit,     // NEW
    onNavigateToLibrary: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    // ...
    when (val state = uiState) {
        // ...
        is SearchUiState.Results -> SearchResultsContent(
            sections = state.grouped.sections,
            onItemClick = { item ->
                viewModel.onSubmit(queryText)
                handleSearchResultClick(
                    item = item,
                    viewModel = viewModel,
                    onTrackSelected = onTrackSelected,
                    onPlaylistSelected = onPlaylistSelected,
                    onArtistSelected = onArtistSelected,
                    onAlbumSelected = onAlbumSelected,
                    onNavigateToLibrary = onNavigateToLibrary
                )
            }
        )
        // ...
    }
}

private fun handleSearchResultClick(
    item: SearchResultItem,
    viewModel: SearchViewModel,
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onArtistSelected: (String, String) -> Unit,  // NEW
    onAlbumSelected: (AlbumSummary) -> Unit,     // NEW
    onNavigateToLibrary: (String) -> Unit
) {
    when (item.kind) {
        SearchResultKind.TRACK,
        SearchResultKind.HISTORY -> {
            val track = viewModel.resolveTrack(item) ?: return
            onTrackSelected(listOf(track), 0)
        }
        SearchResultKind.PLAYLIST -> {
            item.playlistId?.let(onPlaylistSelected)
        }
        SearchResultKind.ARTIST -> {
            val key = item.artistNormalizedKey ?: return
            val name = item.title
            onArtistSelected(key, name)
        }
        SearchResultKind.ALBUM -> {
            val title = item.albumNormalizedTitle ?: return
            val artist = item.albumNormalizedArtist ?: ""
            val summary = AlbumSummary(
                displayTitle = item.title,
                displayArtist = item.subtitle ?: "",
                normalizedTitle = title,
                normalizedArtist = artist,
                trackCount = 0,
                artworkUri = null
            )
            onAlbumSelected(summary)
        }
    }
}
```

**Rationale**: FR5 requires separate Artist/Album callbacks. Now `SearchScreen` can route collection selection directly to `LibraryArtistDetailScreen`/`LibraryAlbumDetailScreen`.

---

### Step 7: Update `MusicPlayerApp` to route collection callbacks

**File**: `app/src/main/java/com/anplak/androidmusic/ui/MusicPlayerApp.kt`

```kotlin
currentScreen is AppScreen.Search -> {
    MiniPlayerOverlayHost(
        showMiniPlayer = showMiniPlayer,
        miniPlayer = miniPlayerSlot
    ) {
        SearchScreen(
            onBackClick = { currentScreen = AppScreen.MainTabs },
            onTrackSelected = { tracks, index ->
                playbackViewModel.onTrackSelected(tracks, index)
                openNowPlaying()
            },
            onPlaylistSelected = { playlistId ->
                currentScreen = AppScreen.PlaylistDetail(playlistId)
            },
            onArtistSelected = { artistKey, displayName ->
                currentScreen = AppScreen.LibraryArtistDetail(artistKey, displayName)
            },
            onAlbumSelected = { album ->
                currentScreen = AppScreen.LibraryAlbumDetail(album)
            },
            onNavigateToLibrary = { query ->
                librarySearchHint = query
                currentTab = NavigationTab.Library
                currentScreen = AppScreen.MainTabs
            },
            viewModel = searchViewModel
        )
    }
}
```

**Rationale**: FR6 routes Artist/Album selection to existing destinations. Back navigation preserves Search results because `currentScreen` remains `AppScreen.Search`.

---

### Step 8: Update test tags

**File**: `app/src/main/java/com/anplak/androidmusic/ui/SearchScreen.kt`

```kotlin
items(section.items, key = { it.id }) { item ->
    ListItem(
        // ...
        .testTag(
            when (item.kind) {
                SearchResultKind.TRACK -> "search_result_track"
                SearchResultKind.HISTORY -> "search_result_history"
                SearchResultKind.PLAYLIST -> "search_result_playlist"
                SearchResultKind.ARTIST -> "search_result_artist"
                SearchResultKind.ALBUM -> "search_result_album"
            }
        )
    )
}
```

**Rationale**: NFR5 adds stable tags for Artist/Album rows.

---

## Test Scenarios

### Unit — Matching
- Title-only track match returns track only (no false artist/album)
- Artist partial match (case-insensitive) returns artist
- Album partial match (case-insensitive) returns album
- Artist match does not return artist's albums unless album title also matches

### Unit — Identity
- `normalizeArtist("The Beatles") == normalizeArtist("the beatles ")` → deduplicated
- Same album title by different artists → two distinct results

### Unit — Navigation Payload
- `libraryQueryForItem(artistItem)` returns `artist:<normalizedKey>`
- `libraryQueryForItem(albumItem)` returns `album:<normalizedTitle>|<normalizedArtist>`

### Compose/UI
- Grouped sections visible (Tracks, Artists, Albums, Playlists, History)
- Artist row test tag `search_result_artist`
- Album row test tag `search_result_album`
- Loading state appears during search
- No-result state shows when no sections have items

### E2E
1. Library → Search → type "Beatles" → tap Artist → opens `LibraryArtistDetailScreen` for Beatles
2. Library → Search → type "Abbey Road" → tap Album → opens `LibraryAlbumDetailScreen` for Abbey Road
3. Back from detail returns to Search results with query preserved

### Regression
- Track selection starts playback
- Playlist selection opens playlist detail
- History selection replays track

### Performance
- 10,000 tracks library: rapid typing produces no main-thread stalls
- Stale results do not flash during new query
