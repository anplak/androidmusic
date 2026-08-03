### User story

As a user, I want the app to feel polished, fast, and reliable—with a clean design and sensible defaults—so it’s ready to use as my primary offline music player.

### Goals & scope (2–3 days)

- **In scope**
  - **UI/UX polish**:
    - Consistent typography, spacing, and iconography across all screens.
    - Refined Now Playing layout (artwork, controls, queue peek).
    - Smooth transitions and animations where appropriate.
  - **Library management**:
    - By default, do not index or persist to the database audio longer than 10 minutes (configurable threshold optional if low effort).
    - Allow users to include or exclude specific folders and their subfolders from the tracks index (e.g. exclude podcasts/audiobooks trees, include only chosen music roots).
    - Re-scan respects folder rules and duration filter; excluded or over-limit tracks are removed or never written on refresh.
  - **Performance & stability**:
    - Profile and optimize slow queries and UI renders.
    - Fix known crashes and ANRs discovered during testing.
- **Out of scope**
  - Theming (light/dark/system) and general settings screen (start screen, shuffle defaults, history retention, etc.).
  - Onboarding and “What’s new” flows.
  - Full localization/internationalization (can be a future iteration).
  - Advanced accessibility beyond basic support (to be expanded later).

### Functional requirements

- **FR1**: During library scan/index, tracks with duration greater than 10 minutes are skipped by default and are not added to the app database.
- **FR2**: Users can mark folders (and all nested subfolders) as included or excluded from indexing; scan only considers allowed paths.
- **FR3**: Changing folder include/exclude rules or the max-duration default triggers a re-index (or equivalent refresh) so the library reflects the new rules.
- **FR4**: The app runs without crashes in normal flows across:
  - Library browsing and playback.
  - For You and smart playlists.
  - Playlist editing and auto-mix.
  - Search and history interactions.

### Non-functional requirements

- **NFR1**: App should feel responsive on mid-range devices with large libraries.
- **NFR2**: No obvious jank during common gestures (scroll, navigate, play/pause, next/prev).
- **NFR3**: Battery impact comparable to other offline players when used similarly.
- **NFR4**: Folder rules and duration filtering must not materially slow scan on typical libraries (filter during ingest, not on every UI read).

### UX notes

- **Visual identity**:
  - Choose a simple, distinctive color palette and app icon to feel “product ready”.
- **Consistency**:
  - Align navigation patterns (back behavior, bottom nav, and gestures).
- **Library rules UI**:
  - Surface folder include/exclude in a dedicated library or scan settings area (even if full Settings is out of scope).
  - Show clear feedback when scan completes (counts indexed vs skipped by duration vs excluded path).
- **Accessibility (baseline)**:
  - Respect system font size where reasonable.
  - Ensure sufficient contrast for primary text and controls.

### Testing & validation

- Run:
  - Manual regression pass over core flows (playback, queue, playlists, For You, search).
  - Library index tests: files longer than 10 min excluded by default; included/excluded folder trees honored on scan and re-scan.
  - Basic instrumentation tests for navigation and DB/DAO.
  - Ad-hoc long-session test (playback for 1–2 hours) to observe stability and battery.

---

### Overall app state after this story

The app is a **release-candidate-quality offline music player** with smart playlists, recommendations, history, search, controllable library indexing, and a polished UI/UX, ready for regular daily use.

### Value added after this story

Users now have a **highly capable, fully offline alternative to streaming apps**, offering Spotify-like personalization and playlist power solely from on-device data and without any network dependency. Library indexing stays focused on music (duration cap, folder control) instead of pulling in long-form audio or unwanted directories.

---

## Implementation plan

**Estimate:** 2–3 days · **Order:** library indexing (data → repo → UI) → polish → perf pass.

### Architecture (target)

```text
MediaStore scan
    → LibraryIndexPolicy (max duration + folder rules from prefs/Room)
    → LibraryIndexFilter (pure, unit-tested)
    → TrackDao insertAll + deleteStaleEntries(validIds)
    → LibraryViewModel / Search / Smart playlists (unchanged consumers of TrackDao)
```

**Prerequisite fix:** `TrackEntity.path` today stores `uri.toString()` (`FavoritesRepository.kt`). Indexing rules need a **filesystem path** from MediaStore (`DATA`, with `RELATIVE_PATH` fallback on API 29+), as in `MusicLibraryIntegrationTest`.

---

### Phase 1 — Library index policy & persistence (~0.5 day)

| Task | Files |
|------|--------|
| Constants + prefs for max duration (default 10 min) | `data/LibraryIndexPolicy.kt`, `data/LibraryIndexPreferences.kt` (DataStore or SharedPreferences, mirror `RecentSearchStore`) |
| Folder rules table + DAO | `data/db/IndexFolderRuleEntity.kt`, `IndexFolderRuleDao.kt`, `AppDatabase` v4 + `MIGRATION_3_4` |
| Pure path matcher | `data/LibraryIndexFilter.kt` |

**`LibraryIndexPolicy`**

```kotlin
data class LibraryIndexPolicy(
    val maxDurationMs: Long = DEFAULT_MAX_INDEX_DURATION_MS,
    val folderRules: List<FolderRule> = emptyList()
) {
    companion object {
        const val DEFAULT_MAX_INDEX_DURATION_MS = 10 * 60 * 1000L
    }
}

enum class FolderRuleMode { INCLUDE, EXCLUDE }

data class FolderRule(
    val path: String,           // normalized absolute path
    val mode: FolderRuleMode
)
```

**`LibraryIndexFilter`** (test without Android)

```kotlin
object LibraryIndexFilter {
    fun shouldIndex(filePath: String, durationMs: Long, policy: LibraryIndexPolicy): Boolean {
        if (durationMs <= 0 || durationMs > policy.maxDurationMs) return false
        val normalized = normalizePath(filePath)
        val excludes = policy.folderRules.filter { it.mode == FolderRuleMode.EXCLUDE }
        if (excludes.any { normalized.startsWith(it.path) }) return false
        val includes = policy.folderRules.filter { it.mode == FolderRuleMode.INCLUDE }
        if (includes.isEmpty()) return true
        return includes.any { normalized.startsWith(it.path) }
    }
}
```

**Room migration (sketch)**

```kotlin
// AppDatabase version 4
db.execSQL("""
    CREATE TABLE IF NOT EXISTS index_folder_rules (
        path TEXT NOT NULL PRIMARY KEY,
        mode TEXT NOT NULL
    )
""")
```

**Policy load**

```kotlin
class LibraryIndexPolicyRepository(
    private val prefs: LibraryIndexPreferences,
    private val folderRuleDao: IndexFolderRuleDao
) {
    suspend fun loadPolicy(): LibraryIndexPolicy = LibraryIndexPolicy(
        maxDurationMs = prefs.getMaxDurationMs(),
        folderRules = folderRuleDao.getAll().map { /* entity → FolderRule */ }
    )
}
```

---

### Phase 2 — Repository ingest & scan summary (~0.75 day)

| Task | Files |
|------|--------|
| Extend MediaStore projection with path | `MusicLibraryRepository.kt` |
| Inject policy repo; filter during cursor loop | same |
| Return scan stats | `data/LibraryScanResult.kt` |
| Wire path into `TrackEntity` | `TrackInfo` + `toEntity(filePath)` |

**Interface change**

```kotlin
interface MusicLibraryRepository {
    suspend fun scanMusicDirectories()
    suspend fun syncLibrary(): LibraryScanResult  // replaces “load all then filter in UI”
}

data class LibraryScanResult(
    val tracks: List<TrackInfo>,
    val indexedCount: Int,
    val skippedDurationCount: Int,
    val skippedFolderCount: Int
)
```

**Ingest loop** (inside existing `getAllTracks` / new `syncLibrary`)

```kotlin
val policy = policyRepository.loadPolicy()
var skippedDuration = 0
var skippedFolder = 0

while (cursor.moveToNext()) {
    val duration = cursor.getLong(durationColumn)
    val filePath = resolveFilePath(cursor) // DATA or RELATIVE_PATH + display name

    if (!LibraryIndexFilter.shouldIndex(filePath, duration, policy)) {
        if (duration > policy.maxDurationMs) skippedDuration++
        else skippedFolder++
        continue
    }
    // build TrackInfo + entities as today
}

trackDao?.let { dao ->
    dao.insertAll(entities)
    dao.deleteStaleEntries(tracks.map { it.id }) // only indexed IDs remain valid
}
return LibraryScanResult(tracks, tracks.size, skippedDuration, skippedFolder)
```

**`resolveFilePath`** — reuse pattern from `MusicLibraryIntegrationTest` (prefer `DATA`; on empty, build from `RELATIVE_PATH`).

**`LibraryViewModel`** — call `syncLibrary()`, expose last `LibraryScanResult` for snackbar; `refresh()` and rule changes reset `hasScanned` / `hasLoaded` (FR3).

---

### Phase 3 — Library index UI (~0.5 day)

| Task | Files |
|------|--------|
| Screen: max duration (read-only label v1 or stepper), folder rule list | `ui/LibraryIndexScreen.kt` |
| VM: CRUD rules, trigger re-sync | `ui/LibraryIndexViewModel.kt` |
| Entry from Library | `LibraryScreen.kt` top bar action; `MusicPlayerApp.kt` `AppScreen.LibraryIndex` |
| Post-scan feedback | Snackbar: “Indexed N · Skipped M (long) · K (folder)” |

**Navigation**

```kotlin
sealed class AppScreen {
    // ...
    data object LibraryIndex : AppScreen()
}
```

**Folder picker (MVP):** preset roots (`Music`, `Download`, `Documents`) + “Add folder” via `OpenDocumentTree` or list common paths under `/storage/`; store normalized absolute path. No full Settings screen.

**Strings / test tags:** `library_index`, `add_exclude_folder`, `scan_summary`.

---

### Phase 4 — UI/UX polish (~0.5 day)

| Area | Approach |
|------|----------|
| Typography / spacing | `ui/theme/Dimens.kt`, `Type.kt`; replace magic `dp` on top screens |
| Track rows | Extract `TrackListItem` used by Library, Favorites, playlist detail, search |
| Now Playing | Larger artwork slot, tighter control row, optional queue peek (`PlaybackViewModel` already has `queueSize` / position) |
| Transitions | `AnimatedContent` for tab switches where cheap; avoid animating full track lists |

**Queue peek (sketch)**

```kotlin
// NowPlayingScreen — below transport controls
if (queueSize > 1) {
    Text(
        text = stringResource(R.string.queue_position, queuePosition, queueSize),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.testTag("queue_peek")
    )
}
```

**Visual identity:** tighten `MaterialTheme` color roles in `MusicPlayerApp` (primary / surface / onSurface contrast per UX notes).

---

### Phase 5 — Performance & stability (~0.25 day)

| Item | Action |
|------|--------|
| Scan | Filter in single MediaStore pass (NFR4); gate `Log.d` per track behind `BuildConfig.DEBUG` |
| Room | `insertAll` in one transaction; verify indexes on `tracks.title`, `play_history.playedAt` if slow |
| Library load | After first sync, optional fast path: `trackDao.getAll()` → `TrackInfo` if MediaStore unchanged (defer if risky) |
| ANRs | Run StrictMode / manual trace on Library open with 5k+ files; fix main-thread hits |
| Regression | Fix crashes from existing E2E / manual pass only (no new feature scope) |

---

### Dependency wiring

```kotlin
// LibraryViewModel — add policy + sync
class LibraryViewModel(
    application: Application,
    private val repository: MusicLibraryRepository = /* inject policy repo */,
    ...
) {
    private val _scanSummary = MutableStateFlow<LibraryScanResult?>(null)
    // loadLibrary: repository.syncLibrary() → applyFilters + _scanSummary
}
```

Register `LibraryIndexPolicyRepository` / DAO in `AppDatabase.getInstance` factory used by ViewModels (same pattern as today).

---

### Unit test scenarios

**`LibraryIndexFilterTest`**
- Track ≤10 min under allowed path → indexed.
- Track longer than 10 min → rejected (duration).
- Path under excluded root (and nested subfolder) → rejected.
- Whitelist: only paths under INCLUDE roots indexed; sibling folder rejected.
- Path normalization: trailing slashes, case on case-insensitive FS.

**`LibraryIndexPolicyRepositoryTest`** (Robolectric + in-memory Room)
- Default max duration is 10 minutes.
- Add/remove folder rule persisted and reflected in `loadPolicy()`.

**`MusicLibraryRepositoryTest`** (extend existing)
- Cursor row over max duration not in returned list / not passed to `insertAll`.
- Excluded path skipped; included path kept.
- `deleteStaleEntries` called with only surviving IDs after re-sync.

**`LibraryViewModelTest`**
- `refresh()` after rule change triggers second sync.
- Scan summary exposed when sync completes.

---

### E2E / instrumentation scenarios

**`LibraryIndexE2ETest`** (device with fixture files via adb)
- Seed short track + 15 min track in Music → Library shows only short track.
- Exclude `Music/Podcasts` → tracks under that tree absent after refresh.
- Open Library index UI → add exclude → pull-to-refresh / refresh → library updates.

**Regression (existing suites)**
- `LibraryScreenE2ETest`, `PlaylistsE2ETest`, `SmartPlaylistsE2ETest`, search flow — all pass with filtered library.

**Manual**
- Large library scroll, 1–2 h playback, scan summary accuracy.

---

### Rollout checklist

- [ ] DB migration 3→4 on upgrade; fresh install creates `index_folder_rules`.
- [ ] Existing favorites/playlists reference track IDs — stale long-form IDs removed by `deleteStaleEntries` (CASCADE on FKs).
- [ ] README: library indexing section (duration default, folder rules).
- [ ] No theming / onboarding / full Settings (explicitly out of scope).

---

### Risk notes

| Risk | Mitigation |
|------|------------|
| `DATA` column restricted on some devices | `RELATIVE_PATH` + display name fallback; log path resolution failures |
| Whitelist vs blacklist confusion | Document in UI: empty INCLUDE list = “all except excludes” |
| Playlists referencing removed tracks | FK CASCADE already; UI may show fewer tracks until user refreshes playlists |
