### User story

As a user, I want a “For You”/Discovery experience that surfaces recommended tracks and mixes from my offline library, similar to Spotify’s home recommendations but fully on-device.

### Goals & scope (2–3 days)

- **In scope**
  - **For You screen**:
    - New top-level screen or tab that shows:
      - “Because you listen to X” rows (track-based or artist-based).
      - “Quick Mix” / “Daily Mix”-style rows (small curated sets).
      - “Continue Listening” row (recent albums/playlists/queues).
  - **Simple recommendation logic (heuristic)**:
    - Use combinations of:
      - Play counts and recency from history.
      - Favorites.
      - Co-occurrence (tracks often played in the same sessions).
    - Example: choose a seed track/artist from recent favorites and propose similar tracks (same artist, album, or similar tags if available).
  - **Interaction model**:
    - Tapping a recommended row either:
      - Starts a mix/queue.
      - Or opens a detail screen (e.g., “Because you listen to X” mix).
- **Out of scope**
  - True ML training or embeddings-based similarity.
  - Cloud sync or cross-device personalization.

### Functional requirements

- **FR1**: The “For You” screen loads from local data only (no network).
- **FR2**: Recommended rows are non-empty when the user has at least some history and favorites.
- **FR3**: Tapping a recommendation starts playback or navigates to a detail playlist-like screen.
- **FR4**: Recommendations refresh periodically (e.g., on app start or via pull-to-refresh).

### Non-functional requirements

- **NFR1**: Recommendation generation must be fast enough to avoid noticeable lag (use background threads).
- **NFR2**: Logic should be deterministic enough that issues are debuggable (good for early iterations).

### UX notes

- **Layout**:
  - Card-based horizontal rows (like Spotify) for mixes and “because you listened to” sections.
- **Feedback**:
  - Simple empty-state text if insufficient data (“Listen to more music to get personalized recommendations”).
- **Consistency**:
  - Use existing cover art or generated placeholders to keep the UI visually rich.

### Testing & validation

- Scenarios:
  - New user with almost no history (empty/near-empty recommendations but graceful UI).
  - Power user with lots of history and favorites (diverse rows).
  - Validate that recommended tracks actually exist and are playable.

---

### Overall app state after this story

The app now provides a **Spotify-like “For You” discovery surface**, with locally generated recommendations and mixes derived from the user’s offline listening data.

### Value added after this story

Users experience a **personalized home screen** that helps them rediscover and explore their own library in a smarter, more engaging way, without any online account or cloud backend.

---

## Implementation plan

**Estimate:** 2–3 days. **Prerequisite:** library scan + history/favorites/stats (stories 1–6) already in place.

### Phase overview

| Phase | Deliverable |
|-------|-------------|
| 1 | `RecommendationRow` models + `PlayHistoryDao` co-occurrence query + `RecommendationEngine` |
| 2 | `RecommendationRepository` + `DiscoveryViewModel` (IO, refresh) |
| 3 | `ForYouScreen` (horizontal rows) + `RecommendationDetailScreen` |
| 4 | Navigation tab + `AppScreen` wiring + playback hooks |
| 5 | Unit tests for engine; one instrumented smoke path |

---

### Key decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Entry point | **New bottom-nav tab** `ForYou` (first position) | Matches “home” discovery pattern; keeps Library as full catalog without crowding it. |
| Generation style | **One-shot snapshot** in `viewModelScope` + manual refresh | Deterministic, easy to debug (NFR2); avoids recomputing heuristics on every `Flow` emission. |
| Logic location | **`RecommendationEngine`** (pure Kotlin) + thin **`RecommendationRepository`** | Same split as `AutoMixGenerator` + `PlaylistRepository`; engine is unit-testable without Room/Compose. |
| Row tap | **Detail screen** for multi-track rows; **direct play** only for single-track shortcuts | Satisfies FR3; reuses `SmartPlaylistDetailScreen` patterns (`Play All`, track list). |
| Mix construction | **Reuse `AutoMixGenerator` + `SmartShuffleGenerator`** | Story 8 already encodes “same artist + filler”; avoids duplicate mix logic. |
| Albums in “Continue Listening” | **Defer album entity**; use playlists + last session tracks | No album browse UI exists; `TrackInfo.album` is enough for “Because you listen…” similarity only. |
| Co-occurrence | **SQL on `sessionId`** in `PlayHistoryDao` | Session IDs already recorded by `PlaybackViewModel`; no schema migration. |
| Daily Mix stability | **Seed from `LocalDate` + top artist** | Same mix all day (Spotify-like), refreshes at midnight without ML. |

---

### 1. Data layer

**New DAO query** (co-occurrence for a seed track):

```kotlin
// PlayHistoryDao.kt
@Query("""
    SELECT ph2.trackId AS trackId, COUNT(*) AS coCount
    FROM play_history ph1
    INNER JOIN play_history ph2
        ON ph1.sessionId = ph2.sessionId
        AND ph1.trackId != ph2.trackId
    WHERE ph1.trackId = :seedTrackId
      AND ph1.sessionId IS NOT NULL
    GROUP BY ph2.trackId
    ORDER BY coCount DESC
    LIMIT :limit
""")
suspend fun getCoPlayedTrackIds(seedTrackId: Long, limit: Int): List<TrackPlayCountResult>
```

**Optional helper** — last session for “Continue Listening”:

```kotlin
@Query("""
    SELECT DISTINCT trackId FROM play_history
    WHERE sessionId = (
        SELECT sessionId FROM play_history
        WHERE sessionId IS NOT NULL
        ORDER BY playedAt DESC LIMIT 1
    )
    ORDER BY playedAt DESC
    LIMIT :limit
""")
suspend fun getLastSessionTrackIds(limit: Int): List<Long>
```

**Repository** loads inputs once per refresh:

```kotlin
// RecommendationRepository.kt
interface RecommendationRepository {
    suspend fun loadInputs(): RecommendationInputs
}

data class RecommendationInputs(
    val library: List<TrackInfo>,
    val favorites: Set<Long>,
    val topArtists30d: List<String>,      // from PlayHistoryRepository.getTopArtistsSince
    val topTracks30d: List<Long>,
    val recentHistory: List<PlayHistoryEntry>,
    val coOccurrenceBySeed: Map<Long, List<Long>>, // precomputed for top N seeds only
    val userPlaylists: List<PlaylistSummary>       // id, name, trackCount, lastPlayedAt?
)
```

*Rationale:* Batch DB reads in repository; engine stays synchronous over in-memory lists.

---

### 2. Domain — `RecommendationEngine`

**Row model** (UI-agnostic):

```kotlin
// data/RecommendationModels.kt
enum class RecommendationRowType {
    BECAUSE_YOU_LISTEN,
    QUICK_MIX,
    DAILY_MIX,
    CONTINUE_LISTENING
}

data class RecommendationRow(
    val id: String,                    // stable key for navigation
    val type: RecommendationRowType,
    val title: String,                 // e.g. "Because you listen to Radiohead"
    val subtitle: String? = null,
    val seedTrack: TrackInfo? = null,
    val tracks: List<TrackInfo>        // preview chips + detail payload
)
```

**Core build** (deterministic ordering):

```kotlin
// data/RecommendationEngine.kt
class RecommendationEngine(
    private val autoMixGenerator: AutoMixGenerator
) {
    suspend fun buildRows(inputs: RecommendationInputs): List<RecommendationRow> {
        if (inputs.library.isEmpty()) return emptyList()

        val rows = mutableListOf<RecommendationRow>()

        // Continue Listening — last session or recent smart-playlist entry point
        buildContinueListening(inputs)?.let { rows += it }

        // Daily Mix — stable per calendar day (suspend: uses AutoMixGenerator)
        rows += buildDailyMix(inputs)

        // Because you listen to {artist|track} — up to 3 rows
        rows += buildBecauseRows(inputs)

        // Quick Mix — smaller sets from favorites / co-occurrence
        rows += buildQuickMixes(inputs)

        return rows.distinctBy { it.id }.filter { it.tracks.isNotEmpty() }
    }

    private suspend fun buildDailyMix(inputs: RecommendationInputs): RecommendationRow {
        val daySeed = java.time.LocalDate.now().toEpochDay()
        val artist = inputs.topArtists30d.firstOrNull()
            ?: inputs.library.random(Random(daySeed)).artist
        val tracks = autoMixGenerator.fromFavoriteArtist(artist, inputs.library, limit = 15)
        return RecommendationRow(
            id = "daily_mix_$daySeed",
            type = RecommendationRowType.DAILY_MIX,
            title = "Daily Mix",
            subtitle = artist,
            tracks = tracks
        )
    }
}
```

**“Because you listen to…”** heuristic (v1):

```kotlin
private fun similarTracks(
    seed: TrackInfo,
    library: List<TrackInfo>,
    coPlayed: List<Long>,
    limit: Int
): List<TrackInfo> {
    val byCo = coPlayed.mapNotNull { id -> library.find { it.id == id } }
    val sameAlbum = library.filter {
        it.album.isNotBlank() && it.album == seed.album && it.id != seed.id
    }
    val sameArtist = library.filter {
        it.artist.equals(seed.artist, ignoreCase = true) && it.id != seed.id
    }
    return (byCo + sameAlbum + sameArtist)
        .distinctBy { it.id }
        .take(limit)
}
```

*Rationale:* Priority co-occurrence → album → artist mirrors spec; no tags/ML. Limit co-occurrence queries to top 3–5 seed tracks (from favorites or top tracks) to keep refresh fast (NFR1).

---

### 3. ViewModel

```kotlin
// ui/DiscoveryViewModel.kt
sealed interface ForYouUiState {
    data object Loading : ForYouUiState
    data class Content(val rows: List<RecommendationRow>) : ForYouUiState
    data object Empty : ForYouUiState   // insufficient history
    data class Error(val message: String) : ForYouUiState
}

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<ForYouUiState>(ForYouUiState.Loading)
    val uiState: StateFlow<ForYouUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ForYouUiState.Loading
            runCatching {
                val inputs = recommendationRepository.loadInputs()
                val rows = recommendationEngine.buildRows(inputs)
                _uiState.value = when {
                    rows.isEmpty() -> ForYouUiState.Empty
                    else -> ForYouUiState.Content(rows)
                }
            }.onFailure { _uiState.value = ForYouUiState.Error(it.message ?: "Unknown error") }
        }
    }
}
```

*Rationale:* Mirrors `HistoryViewModel` (`loadHistory` / `refresh`); FR4 satisfied via `init` + pull-to-refresh on screen.

---

### 4. UI

**`ForYouScreen`** — vertical `LazyColumn` of sections; each section = title + horizontal `LazyRow` of track cards (title/artist, placeholder “album art” circle using `MaterialTheme` color + first letter).

```kotlin
// ForYouScreen.kt — section skeleton
@Composable
fun RecommendationRowSection(
    row: RecommendationRow,
    onRowClick: () -> Unit,
    onTrackClick: (TrackInfo) -> Unit
) {
    Column {
        Text(row.title, style = MaterialTheme.typography.titleMedium)
        row.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        LazyRow {
            items(row.tracks.take(12), key = { it.id }) { track ->
                RecommendationTrackCard(track, onClick = { onTrackClick(track) })
            }
        }
        TextButton(onClick = onRowClick) { Text("See all") }
    }
}
```

**`RecommendationDetailScreen`** — clone `SmartPlaylistDetailScreen`: top bar title from row, `Play All` → `onPlayAll(tracks, 0)`, track list with `onTrackSelected`.

**Empty state copy:** `Listen to more music to get personalized recommendations.` (FR2 graceful degrade)

**Pull-to-refresh:** Material3 `pullRefresh` + `viewModel.refresh()` (same as `HistoryScreen`).

---

### 5. Navigation & playback

```kotlin
// MusicPlayerApp.kt
enum class NavigationTab(...) {
    ForYou(Icons.Default.Explore, R.string.for_you),  // first tab
    Library(...),
    ...
}

sealed class AppScreen {
    ...
    data class RecommendationDetail(val rowId: String) : AppScreen()
}

// MainTabsContent
NavigationTab.ForYou -> ForYouScreen(
    onRowSelected = { row -> currentScreen = AppScreen.RecommendationDetail(row.id) },
    onPlayRow = { tracks -> playbackViewModel.startSmartShuffleFromPlaylist(tracks); ... },
    onTrackSelected = onTrackSelected
)
```

Store `rowId → RecommendationRow` in `DiscoveryViewModel` cache or pass `tracks` via saved state; *prefer ViewModel `getRow(id)`* so back stack stays small.

**Playback:** Row-level “Play mix” calls existing `PlaybackViewModel.startSmartShuffleFromPlaylist(row.tracks)` — weighted order without new player APIs.

---

### 6. Files to add/change

| Action | File |
|--------|------|
| Add | `data/RecommendationModels.kt` |
| Add | `data/RecommendationEngine.kt` |
| Add | `data/RecommendationRepository.kt` |
| Add | `ui/DiscoveryViewModel.kt` |
| Add | `ui/ForYouScreen.kt` |
| Add | `ui/RecommendationDetailScreen.kt` |
| Modify | `data/db/PlayHistoryDao.kt` (queries) |
| Modify | `ui/MusicPlayerApp.kt` (tab + screens) |
| Modify | `res/values/strings.xml` (`for_you`, row titles) |
| Modify | `README.md` (feature bullet + roadmap) |

No Room migration if only new `@Query` methods.

---

### 7. Test scenarios (not full spec)

| Scenario | Expect |
|----------|--------|
| Cold start, empty history | For You shows empty state; no crash |
| Favorites + 10+ plays | At least one “Because you listen…” and one Quick/Daily mix row |
| Same day, two refreshes | Daily Mix track set unchanged |
| Next calendar day | Daily Mix `id`/contents may change |
| Tap row → detail → Play All | Queue starts; tracks exist in library |
| Tap horizontal card | Playback from that track in row context |
| Pull-to-refresh | Rows regenerate; loading indicator shown |
| Co-occurrence | Two tracks in same `sessionId` → second appears in seed’s mix |
| Missing files | Rows only contain IDs resolved via `TrackDao`; skip stale IDs |

**Unit tests (high value):** `RecommendationEngineTest` — fixed `RecommendationInputs` → assert row count, titles, deterministic Daily Mix with fixed `LocalDate` (inject clock interface if needed).

---

### 8. Explicitly out of scope (reminder)

- ML / embeddings
- Network / account sync
- Album grid or new “album” entity for Continue Listening (v1 uses sessions + playlists only)
- Cover art from MediaStore (optional follow-up; placeholders OK per UX notes)


