### User story

As a user, I want fast search and powerful filters across my library, playlists, and history so I can quickly find the right track or mix for the moment.

### Goals & scope (2–3 days)

- **In scope**
  - **Global search**:
    - Search by track title, artist, album, and optionally folder/path.
    - Search across:
      - Entire library.
      - Playlists (by name and by tracks inside).
      - History (recent plays).
  - **Filters**:
    - Filter library by:
      - Favorites only.
      - Duration ranges (short/medium/long).
      - Recently added.
    - Optional: Simple “mood” tags derived from heuristics (e.g., tempo proxy, energy from loudness if metadata is available).
  - **Search entry points**:
    - Persistent search icon in main toolbar.
    - Search bar on library and playlists screens.
- **Out of scope**
  - Full-text search in lyrics or comments.
  - Fuzzy search sophistication beyond basic partial matches.

### Functional requirements

- **FR1**: User can open a search screen and see recent searches and suggestions (e.g., top artists).
- **FR2**: Typing a query filters results in real time (with debouncing).
- **FR3**: Tapping any search result navigates to:
  - Track → starts playback (queue from relevant context).
  - Playlist → opens playlist detail.
  - History item → replays that track.
- **FR4**: Filter controls on the library screen narrow the visible set accordingly.

### Non-functional requirements

- **NFR1**: Search must remain responsive even for large libraries (use indexed DB queries where appropriate).
- **NFR2**: Filters must apply quickly without visible UI blocking.

### UX notes

- **Unified search results**:
  - Grouped sections (Tracks, Artists, Albums, Playlists, History) with headers.
- **Empty/search states**:
  - Friendly message for no results; hints to broaden search or remove filters.
- **Filter chips**:
  - Use chips or toggles at the top of the library for quick filters (Favorites, Recently Added, etc.).

### Testing & validation

- Validate:
  - Case-insensitive matching.
  - Performance on libraries with thousands of tracks.
  - Interactions between filters and search inputs.

---

### Overall app state after this story

The app now features a **rich search and filtering experience** across library, playlists, and history, making it easy for users to quickly access any part of their offline collection.

### Value added after this story

Users can **instantly locate music for any context**, further aligning the app with the usability expectations set by modern streaming apps like Spotify, while remaining fully offline.

---

## Implementation plan

**Estimate:** 2–3 days. **Prerequisites:** Room `tracks` cache populated by `MusicLibraryRepository`, playlists/history/favorites (stories 1–7).

### Phase overview

| Phase | Deliverable |
|-------|-------------|
| 1 | `SearchModels` + DAO queries (`TrackDao`, `PlaylistDao`, `PlayHistoryDao`) + `SearchRepository` |
| 2 | `SearchEngine` (grouped results, debounced query) + `SearchViewModel` |
| 3 | `SearchScreen` (grouped sections, recent/suggestions, empty states) |
| 4 | Library filter chips + `LibraryViewModel` filter pipeline |
| 5 | Navigation (`AppScreen.Search`), toolbar entry points, Playlists inline search |
| 6 | Unit tests for engine/filters; E2E smoke for search + library filters |

---

### Key decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Global search UI | **`AppScreen.Search` overlay** (back returns to prior tab) | FR1 toolbar icon without a 6th bottom-nav tab; mirrors `RecommendationDetail` / `Insights` overlay pattern. |
| Library-only filters | **`LibraryViewModel` + filter chips on `LibraryScreen`** | FR4 is scoped to library; filters apply to cached `currentTracks` (NFR2, no extra DB round-trips). |
| Search logic location | **`SearchEngine` (pure Kotlin) + `SearchRepository` (Room)** | Same split as `RecommendationEngine` / `RecommendationRepository`; engine unit-testable. |
| Track matching | **Room `LIKE` on `tracks` (title, artist, album, path)** | NFR1: indexed table already exists post-scan; avoids loading full library into memory for every keystroke. |
| Playlist / history matching | **DAO `LIKE` + JOINs** | Playlist name + member tracks; history via `play_history` ⋈ `tracks`. |
| Artist / album rows | **Derived from track hits** (distinct artist/album strings) | No new entities; satisfies grouped UX without album browse feature. |
| Match style | **Case-insensitive substring** (`COLLATE NOCASE`, trim query) | Spec FR2; explicitly out of scope: fuzzy/Levenshtein. |
| Debounce | **300 ms** on query `StateFlow` | FR2 real-time feel without query storm on large libraries. |
| Recent searches | **`DataStore` list (max 10)** | FR1; lightweight, no Room migration. |
| Suggestions | **Top artists (30d) from `PlayHistoryRepository`** | Reuses existing aggregation; shown when query empty. |
| Library search bar | **In-place filter** of library list (optional shortcut to global via toolbar only) | Spec lists both toolbar icon and library search bar; in-place bar = FR4-friendly; global = cross-source FR1. |
| Path / folder | **Search `TrackEntity.path`** | Optional in spec; path already cached in Room. |
| Mood tags | **Defer to follow-up** | Rare loudness/tempo metadata offline; heuristics would be misleading. |
| Mood / lyrics FTS | **Out of scope** | Per story boundaries. |

---

### 1. Data layer

**Extend `TrackDao`** — primary search surface (NFR1):

```kotlin
// data/db/TrackDao.kt
@Query("""
    SELECT * FROM tracks
    WHERE title LIKE '%' || :query || '%' COLLATE NOCASE
       OR artist LIKE '%' || :query || '%' COLLATE NOCASE
       OR album LIKE '%' || :query || '%' COLLATE NOCASE
       OR path LIKE '%' || :query || '%' COLLATE NOCASE
    ORDER BY title ASC
    LIMIT :limit
""")
suspend fun searchTracks(query: String, limit: Int = 200): List<TrackEntity>

@Query("""
    SELECT * FROM tracks
    WHERE firstSeenAt >= :sinceMs
    ORDER BY firstSeenAt DESC
""")
suspend fun getTracksAddedSince(sinceMs: Long): List<TrackEntity>
```

*Rationale:* `LIMIT` caps UI work; path column enables folder queries without extending `TrackInfo`. Optional: `@Index` on `title`, `artist` if profiling shows slow scans (no migration if using existing table).

**`PlaylistDao`** — name + tracks inside playlist:

```kotlin
@Query("""
    SELECT DISTINCT p.id, p.name, p.createdAt,
           (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount
    FROM playlists p
    LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId
    LEFT JOIN tracks t ON pt.trackId = t.id
    WHERE p.name LIKE '%' || :query || '%' COLLATE NOCASE
       OR t.title LIKE '%' || :query || '%' COLLATE NOCASE
       OR t.artist LIKE '%' || :query || '%' COLLATE NOCASE
    ORDER BY p.createdAt DESC
    LIMIT :limit
""")
suspend fun searchPlaylists(query: String, limit: Int = 20): List<PlaylistWithTrackCount>
```

**`PlayHistoryDao`** — recent plays matching metadata:

```kotlin
@Query("""
    SELECT ph.id, ph.trackId, ph.playedAt, ph.duration, ph.sessionId,
           t.title, t.artist, t.album, t.duration AS trackDuration
    FROM play_history ph
    INNER JOIN tracks t ON ph.trackId = t.id
    WHERE t.title LIKE '%' || :query || '%' COLLATE NOCASE
       OR t.artist LIKE '%' || :query || '%' COLLATE NOCASE
       OR t.album LIKE '%' || :query || '%' COLLATE NOCASE
    ORDER BY ph.playedAt DESC
    LIMIT :limit
""")
suspend fun searchHistory(query: String, limit: Int = 30): List<PlayHistoryWithTrack>
```

**`SearchRepository`** — parallel IO, single entry for ViewModel:

```kotlin
// data/SearchRepository.kt
interface SearchRepository {
    suspend fun searchAll(query: String): SearchRawResults
    suspend fun getSuggestions(): List<String>  // top artist names
}

data class SearchRawResults(
    val tracks: List<TrackInfo>,
    val playlists: List<Playlist>,
    val history: List<PlayHistoryEntry>
)
```

*Rationale:* One suspend call from ViewModel; repositories map entities → domain types via existing `toTrackInfo()` / `PlayHistoryEntry` mappers.

**Recent searches** (FR1):

```kotlin
// data/RecentSearchStore.kt — DataStore Preferences
suspend fun addRecentQuery(query: String)
fun recentQueries(): Flow<List<String>>
```

---

### 2. Domain — `SearchEngine`

**Result models** (UI-ready, grouped):

```kotlin
// data/SearchModels.kt
enum class SearchResultKind { TRACK, ARTIST, ALBUM, PLAYLIST, HISTORY }

data class SearchResultItem(
    val id: String,              // stable: "track:123", "playlist:4", "history:99"
    val kind: SearchResultKind,
    val title: String,
    val subtitle: String? = null,
    val trackId: Long? = null,
    val playlistId: Long? = null,
    val historyId: Long? = null
)

data class SearchSection(
    val header: String,          // "Tracks", "Artists", …
    val items: List<SearchResultItem>
)

data class GroupedSearchResults(
    val sections: List<SearchSection>,
    val totalCount: Int
)
```

**Grouping + caps** (keeps list scannable):

```kotlin
// data/SearchEngine.kt
class SearchEngine {
    fun buildGrouped(query: String, raw: SearchRawResults): GroupedSearchResults {
        if (query.isBlank()) return GroupedSearchResults(emptyList(), 0)

        val artists = raw.tracks
            .map { it.artist.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(8)
            .map { SearchResultItem("artist:${it}", ARTIST, it) }

        val albums = raw.tracks
            .filter { it.album.isNotBlank() }
            .distinctBy { "${it.artist}|${it.album}".lowercase() }
            .take(8)
            .map { SearchResultItem("album:${it.album}", ALBUM, it.album, subtitle = it.artist) }

        val sections = listOfNotNull(
            section("Tracks", raw.tracks.take(50).map { trackItem(it) }),
            section("Artists", artists).takeIf { it.items.isNotEmpty() },
            section("Albums", albums).takeIf { it.items.isNotEmpty() },
            section("Playlists", raw.playlists.take(10).map { playlistItem(it) }),
            section("History", raw.history.take(15).map { historyItem(it) })
        )
        return GroupedSearchResults(sections, sections.sumOf { it.items.size })
    }
}
```

*Rationale:* Spec UX “grouped sections”; artist/album are navigation shortcuts (tap artist → library filter or search screen with prefilled query) without new schema.

**Library filters** (separate from global search):

```kotlin
// data/LibraryFilter.kt
data class LibraryFilter(
    val favoritesOnly: Boolean = false,
    val recentlyAdded: Boolean = false,
    val durationBucket: DurationBucket? = null  // SHORT <3m, MEDIUM 3–8m, LONG >8m
)

enum class DurationBucket { SHORT, MEDIUM, LONG }

object LibraryFilterEngine {
    fun apply(
        tracks: List<TrackInfo>,
        filter: LibraryFilter,
        favoriteIds: Set<Long>,
        recentlyAddedIds: Set<Long>   // from TrackDao.getTracksAddedSince(7.days)
    ): List<TrackInfo> = tracks.filter { track ->
        (!filter.favoritesOnly || track.id in favoriteIds) &&
        (!filter.recentlyAdded || track.id in recentlyAddedIds) &&
        (filter.durationBucket == null || track.matches(filter.durationBucket))
    }
}
```

*Rationale:* Filters combine with **library** search bar text in ViewModel: `LibraryFilterEngine.apply(currentTracks, filter, …).filter { matchesQuery(it, query) }`. Global `SearchScreen` unchanged.

---

### 3. ViewModels

**`SearchViewModel`** — debounced global search:

```kotlin
// ui/SearchViewModel.kt
sealed interface SearchUiState {
    data object Idle : SearchUiState                    // suggestions + recent
    data object Searching : SearchUiState
    data class Results(val grouped: GroupedSearchResults) : SearchUiState
    data object NoResults : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(...) : AndroidViewModel(application) {
    private val query = MutableStateFlow("")
    private val recentStore: RecentSearchStore = ...

    init {
        viewModelScope.launch {
            query
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { q ->
                    when {
                        q.isBlank() -> _uiState.value = SearchUiState.Idle
                        q.length < 2 -> _uiState.value = SearchUiState.Idle  // optional min length
                        else -> search(q)
                    }
                }
        }
    }

    fun onQueryChange(text: String) { query.value = text }
    fun onSubmit(query: String) { viewModelScope.launch { recentStore.addRecentQuery(query) } }
}
```

*Rationale:* Mirrors `DiscoveryViewModel` refresh pattern; `collectLatest` cancels stale searches when user types quickly.

**Extend `LibraryViewModel`** — filters + optional local query:

```kotlin
// ui/LibraryViewModel.kt — extend Content
data class Content(
    val tracks: List<TrackInfo>,           // filtered view
    val favoriteIds: Set<Long>,
    val filter: LibraryFilter = LibraryFilter(),
    val localQuery: String = "",
    val showNoFilterResults: Boolean = false  // library has tracks but filter/query empty set
)

fun setFilter(filter: LibraryFilter) { applyFilters() }
fun setLocalQuery(query: String) { applyFilters() }

private fun applyFilters() {
    val filtered = LibraryFilterEngine.apply(currentTracks, _filter, favoriteIds, recentlyAddedIds)
        .let { list -> if (localQuery.isBlank()) list else list.filter { matchesLocal(it, localQuery) } }
    _uiState.value = when {
        currentTracks.isEmpty() -> LibraryUiState.Empty
        filtered.isEmpty() -> LibraryUiState.Content(..., showNoFilterResults = true)
        else -> LibraryUiState.Content(tracks = filtered, ...)
    }
}
```

*Rationale:* Reuses existing `currentTracks` cache (```44:44:app/src/main/java/com/anplak/androidmusic/ui/LibraryViewModel.kt```); favorites Flow already updates `favoriteIds`.

---

### 4. UI

**`SearchScreen`** — full-screen overlay:

```kotlin
// ui/SearchScreen.kt
Scaffold(
    topBar = {
        TopAppBar(
            navigationIcon = { IconButton(onBack) },
            title = {
                SearchBar(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = { viewModel.onSubmit(it) },
                    modifier = Modifier.testTag("search_field")
                )
            }
        )
    }
) {
    when (state) {
        Idle -> RecentAndSuggestions(recent, suggestions)
        Searching -> CircularProgressIndicator(Modifier.testTag("search_loading"))
        Results -> LazyColumn {
            state.grouped.sections.forEach { section ->
                item { SectionHeader(section.header) }
                items(section.items, key = { it.id }) { item ->
                    SearchResultRow(item, onClick = { onResultClick(item) })
                }
            }
        }
        NoResults -> EmptySearchState()  // no_search_results + hint strings
    }
}
```

**`LibraryScreen`** — filter chips + search field under TopAppBar:

```kotlin
// LibraryScreen.kt — above TrackList
FilterChipRow(
    favorites = filter.favoritesOnly,
    recentlyAdded = filter.recentlyAdded,
    duration = filter.durationBucket,
    onFilterChange = viewModel::setFilter,
    modifier = Modifier.testTag("library_filter_chips")
)
OutlinedTextField(
    value = localQuery,
    onValueChange = viewModel::setLocalQuery,
    placeholder = { Text(stringResource(R.string.search_library_hint)) },
    modifier = Modifier.testTag("library_search_field")
)
```

*Rationale:* Reuse existing `TrackList` / `TrackItem` (```156:184:app/src/main/java/com/anplak/androidmusic/ui/LibraryScreen.kt```) with **filtered** `tracks`; add `library_no_filter_results` empty state distinct from `no_music_found`.

**`PlaylistsScreen`** — optional `OutlinedTextField` filtering `PlaylistsUiState` list in `PlaylistsViewModel` (in-memory on playlist names; no new screen).

**Toolbar search icon** — on main tabs scaffold (or per-screen TopAppBar):

```kotlin
IconButton(
    onClick = onOpenSearch,
    modifier = Modifier.testTag("open_search")
) { Icon(Icons.Default.Search, ...) }
```

Wire from `MainTabsContent` / shared top area so FR1 is visible on Library and Playlists at minimum.

---

### 5. Navigation & result actions (FR3)

```kotlin
// MusicPlayerApp.kt
sealed class AppScreen {
    ...
    data object Search : AppScreen()
}

// when Search result tapped:
when (item.kind) {
    TRACK -> onTrackSelected(listOf(track), 0)  // resolve trackId → TrackInfo
    PLAYLIST -> currentScreen = AppScreen.PlaylistDetail(item.playlistId!!)
    HISTORY -> resolve history → onTrackSelected(singleTrackList, 0)
    ARTIST -> { /* set Library tab + filter/query prefilled */ currentScreen = MainTabs; currentTab = Library }
    ALBUM -> { /* same or open Search with query = album */ }
}
```

*Rationale:* Reuses existing playback (`onTrackSelected`) and `PlaylistDetail` navigation; artist/album taps land user on Library with filters/query applied (no new album grid).

**Playback queue context:** Track/history results use result list or single track as queue; playlist opens detail first (user taps Play All there).

---

### 6. Strings (`strings.xml`)

Add `<!-- Search -->` block following existing empty-state pairs:

- `search`, `search_hint`, `search_library_hint`
- `search_recent`, `search_suggestions`
- `no_search_results`, `no_search_results_description`
- `filter_favorites`, `filter_recently_added`, `filter_duration_short|medium|long`
- `library_no_filter_results`, `library_no_filter_results_description`
- Section headers: `search_section_tracks`, `search_section_artists`, …

---

### 7. Files to add/change

| Action | File |
|--------|------|
| Add | `data/SearchModels.kt` |
| Add | `data/SearchEngine.kt` |
| Add | `data/LibraryFilter.kt` (+ `LibraryFilterEngine`) |
| Add | `data/SearchRepository.kt` |
| Add | `data/RecentSearchStore.kt` |
| Add | `ui/SearchViewModel.kt` |
| Add | `ui/SearchScreen.kt` |
| Modify | `data/db/TrackDao.kt`, `PlaylistDao.kt`, `PlayHistoryDao.kt` |
| Modify | `data/MusicLibraryRepository.kt` (optional `searchTracks` delegate) |
| Modify | `ui/LibraryViewModel.kt`, `LibraryScreen.kt` |
| Modify | `ui/PlaylistsScreen.kt`, `PlaylistsViewModel.kt` (playlist name filter) |
| Modify | `ui/MusicPlayerApp.kt` (`AppScreen.Search`, toolbar, callbacks) |
| Modify | `res/values/strings.xml` |
| Modify | `README.md` (remove “Search functionality” from out-of-scope; feature bullet) |
| Test | `SearchEngineTest.kt`, `LibraryFilterEngineTest.kt`, extend `LibraryViewModelTest.kt` |

No Room schema version bump if only new `@Query` methods on existing tables.

---

### 8. Test scenarios

| Scenario | Expect |
|----------|--------|
| Empty query on Search | Recent searches (if any) + top-artist suggestions |
| Type "beat" (debounced) | Grouped sections update; case-insensitive ("Beatles" / "beatles") |
| No matches | `NoResults` empty state with broadening hint |
| Submit search | Query appears in recent list (max 10, deduped) |
| Tap track result | Playback starts; navigates to Now Playing |
| Tap playlist result | `PlaylistDetail` opens |
| Tap history result | Track replays |
| Library: Favorites chip | Only favorited tracks visible |
| Library: Recently added + duration Medium | Intersection of filters applies |
| Library: filter + local search bar | Both narrow list; empty → `library_no_filter_results` |
| Library truly empty | Still `no_music_found` (not filter empty) |
| Playlists: type in search field | Playlist list filters by name |
| Large library (1k+ tracks) | Search completes without ANR; DAO `LIMIT` respected |
| Clear filters | Full library list returns |

**Unit tests (high value):** `SearchEngineTest` with fixed `SearchRawResults`; `LibraryFilterEngineTest` for duration buckets and combined filters.

---

### 9. Explicitly out of scope (reminder)

- Lyrics/comments search, fuzzy ranking, ML
- Mood/heuristic tags (defer)
- Sort options (separate story per README)
- Album/artist dedicated browse screens (search may deep-link to Library only)


