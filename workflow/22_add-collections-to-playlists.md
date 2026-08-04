### User story

As a user viewing an Artist or Album, I want to add the collection's tracks to a playlist in one action so I do not have to add every track individually.

### Description

Artist and Album detail screens already resolve their tracks and allow per-track “Add to playlist.” This story adds a collection-level action, generalizes the playlist picker to accept multiple tracks, and performs the update as one safe bulk operation. Existing playlist order is preserved and tracks already present are not duplicated.

**Estimate:** 2–3 days.

**Prerequisites:** stories **04** (persistent playlists) and **13** (Artist/Album detail). Story **21** is recommended but not required.

### Acceptance criteria

- **AC1:** Artist detail exposes an **Add to playlist** action that targets all tracks currently belonging to that artist.
- **AC2:** Album detail exposes the same action for that exact album-and-artist collection.
- **AC3:** The user can choose an existing playlist or create a new playlist from the collection action.
- **AC4:** Tracks already in the selected playlist are skipped; no duplicate playlist entries are created.
- **AC5:** New tracks are appended in the same deterministic order shown on the collection detail screen.
- **AC6:** A success message reports a human-readable outcome, such as “12 tracks added” or “Already in playlist.”
- **AC7:** Dismissing the picker or a failed write leaves the playlist unchanged.
- **AC8:** Existing single-track Add to playlist behavior continues to work.

### Functional requirements

- **FR1:** Add one collection-level action to `LibraryArtistDetailScreen` and `LibraryAlbumDetailScreen`; it may be a top-bar action or a clearly labeled button near **Play all**.
- **FR2:** Replace the single-track-only dialog contract with a reusable selection model that can represent one or many track IDs plus a display label.
- **FR3:** Extend `PlaylistRepository` with a bulk append operation that:
  - deduplicates incoming IDs while preserving order;
  - excludes IDs already in the destination;
  - appends after the current last position;
  - executes atomically.
- **FR4:** Creating a playlist from this flow inserts the collection tracks in the same transaction or cleans up the empty playlist if insertion fails.
- **FR5:** `PlaylistsViewModel` exposes operation state or an event containing added count, skipped count, and failure.
- **FR6:** The dialog must prevent submission for an empty collection and must not enqueue the operation twice on repeated taps.
- **FR7:** Keep per-track menu actions wired through the same generalized flow to avoid two divergent playlist pickers.

### Non-functional requirements

- **NFR1:** Adding a collection of 1,000 tracks must use bounded bulk database work, not one read/write transaction per track.
- **NFR2:** The operation must be atomic: observers see either the complete append or no append.
- **NFR3:** Playlist track ordering and uniqueness constraints remain valid under concurrent calls.
- **NFR4:** UI remains responsive while the database operation runs; disable or show progress on the committing action.
- **NFR5:** All user-facing text is resource-backed and pluralized correctly.

### UX design

- Place **Add to playlist** beside or near **Play all** using the same action hierarchy; do not hide the only collection action behind each track's overflow.
- Picker title includes context, for example **Add “Discovery” to playlist** or a subtitle such as **18 tracks**.
- Existing playlists show their name and track count; **New playlist** stays first.
- After selection, close the picker only when the operation is accepted, then show concise feedback near the current screen.
- For partial no-op outcomes, prefer “8 added · 3 already there” over technical database wording.
- Do not present hundreds of selected tracks inside the dialog; the collection name and count are sufficient.

### Testing/validation strategy

- **Unit — repository:** empty input, duplicate input IDs, some/all tracks already present, deterministic append positions, and rollback on injected failure.
- **Unit — ViewModel:** added/skipped counts, duplicate-submit protection, create-and-add behavior, and error state.
- **Compose/UI:** Artist and Album actions open the picker with the correct collection label/count; existing and new playlist paths work.
- **E2E:** add an album to an existing playlist, open playlist detail, verify order and count; repeat to confirm no duplicates.
- **E2E:** create a playlist from an artist, then verify every artist track appears once.
- **Regression:** single-track Add to playlist and existing playlist reorder/remove/merge tools remain functional.
- **Performance:** validate a seeded 1,000-track artist collection without UI blocking or visibly incremental inserts.

### Out of scope

- Selecting only some tracks from the Artist/Album before adding.
- Automatically creating one playlist per album or artist.
- Removing a whole Artist/Album from a playlist.
- Cloud sync, collaborative playlists, sharing, or export.
- Queue changes, smart shuffle, and auto-mix generation.

---

### Overall app state after this story

Artist and Album collections can be saved to new or existing playlists with one reliable bulk action.

### Value added after this story

Playlist curation becomes practical for collections while reusing and strengthening the existing single-track flow.

---

## Implementation plan

### Overview

Add collection-level "Add to playlist" action to Artist/Album detail screens. Generalize existing single-track `AddToPlaylistDialog` to support multiple tracks. Extend `PlaylistRepository` with bulk append operation.

**Key decisions:**
1. Reuse existing `AddToPlaylistDialog` with new contract for multiple tracks
2. Add `addTracksToPlaylist` to `PlaylistRepository` for atomic bulk operation
3. Create `PlaylistOperationResult` sealed class for operation outcome
4. Add `addCollectionToPlaylist` to `PlaylistsViewModel` with operation state

---

### 1. Data layer changes

#### 1.1 Extend `PlaylistDao` (no new SQL needed)

`PlaylistDao.addTracksToPlaylist()` already exists and handles bulk inserts atomically via `@Insert`.

**Required additions:**
```kotlin
@Query("""
    SELECT trackId FROM playlist_tracks
    WHERE playlistId = :playlistId AND trackId IN (:trackIds)
""")
suspend fun getExistingTrackIds(
    playlistId: Long,
    trackIds: List<Long>,
): List<Long>
```

**Rationale:** Efficiently check which tracks already exist in a single query instead of N individual checks.

#### 1.2 Extend `PlaylistRepository` interface

```kotlin
interface PlaylistRepository {
    // ... existing methods ...
    
    suspend fun addTracksToPlaylist(
        playlistId: Long,
        trackIds: List<Long>,
    ): PlaylistOperationResult
}
```

#### 1.3 Implement `addTracksToPlaylist` in `PlaylistRepositoryImpl`

```kotlin
data class PlaylistOperationResult(
    val addedCount: Int,
    val skippedCount: Int,
    val playlistId: Long,
)

class PlaylistRepositoryImpl(...) : PlaylistRepository {
    override suspend fun addTracksToPlaylist(
        playlistId: Long,
        trackIds: List<Long>,
    ): PlaylistOperationResult {
        if (trackIds.isEmpty()) return PlaylistOperationResult(0, 0, playlistId)
        
        val deduplicatedIds = trackIds.distinct()
        val existingIds = playlistDao.getExistingTrackIds(playlistId, deduplicatedIds)
        val newIds = deduplicatedIds.filter { it !in existingIds }
        
        if (newIds.isEmpty()) {
            return PlaylistOperationResult(0, deduplicatedIds.size, playlistId)
        }
        
        val maxPosition = playlistDao.getMaxPosition(playlistId) ?: -1
        val now = System.currentTimeMillis()
        val refs = newIds.mapIndexed { index, trackId ->
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = maxPosition + index + 1,
                addedAt = now,
            )
        }
        
        playlistDao.addTracksToPlaylist(refs)
        
        return PlaylistOperationResult(newIds.size, deduplicatedIds.size - newIds.size, playlistId)
    }
}
```

**Rationale:**
- Single query for existing tracks (NFR1: bounded bulk work)
- Atomic insert via `@Insert` with list (NFR2: atomic operation)
- Deduplicate input while preserving order (FR3 requirement)
- Returns both added and skipped counts for user feedback (FR5)

---

### 2. ViewModel layer changes

#### 2.1 Add new sealed class for operation state

```kotlin
sealed interface PlaylistOperationState {
    data object Idle : PlaylistOperationState
    data object Loading : PlaylistOperationState
    data class Success(val result: PlaylistOperationResult) : PlaylistOperationState
    data class Error(val message: String) : PlaylistOperationState
}
```

#### 2.2 Add state to `PlaylistsViewModel`

```kotlin
class PlaylistsViewModel(...) {
    // ... existing fields ...
    
    private val _operationState = MutableStateFlow<PlaylistOperationState>(PlaylistOperationState.Idle)
    val operationState: StateFlow<PlaylistOperationState> = _operationState.asStateFlow()
    
    // ... existing methods ...
    
    fun addCollectionToPlaylist(
        playlistId: Long?,
        collectionName: String,
        trackIds: List<Long>,
    ) {
        if (trackIds.isEmpty()) {
            _operationState.value = PlaylistOperationState.Error("No tracks to add")
            return
        }
        
        if (_operationState.value is PlaylistOperationState.Loading) return
        
        _operationState.value = PlaylistOperationState.Loading
        
        viewModelScope.launch {
            try {
                val result = if (playlistId != null) {
                    playlistRepository.addTracksToPlaylist(playlistId, trackIds)
                } else {
                    val newId = playlistRepository.createPlaylist(collectionName)
                    playlistRepository.addTracksToPlaylist(newId, trackIds)
                }
                _operationState.value = PlaylistOperationState.Success(result)
            } catch (e: Exception) {
                _operationState.value = PlaylistOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun clearOperationState() {
        _operationState.value = PlaylistOperationState.Idle
    }
}
```

**Rationale:**
- Prevent duplicate submits via loading state check (FR6)
- Clear operation state after user acknowledges (FR7: reuse same flow)
- Error state allows UI to show feedback without leaving loading (FR5)

---

### 3. UI layer changes

#### 3.1 Generalize `AddToPlaylistDialog` for multiple tracks

**Current signature:**
```kotlin
fun AddToPlaylistDialog(
    track: TrackInfo,
    onDismiss: () -> Unit,
    onPlaylistSelected: (playlistId: Long, trackId: Long) -> Unit,
    onCreatePlaylist: (name: String, trackId: Long) -> Unit,
    ...
)
```

**New signature:**
```kotlin
fun AddToPlaylistDialog(
    collectionName: String,
    trackCount: Int,
    onDismiss: () -> Unit,
    onPlaylistSelected: (playlistId: Long, trackIds: List<Long>) -> Unit,
    onCreatePlaylist: (name: String, trackIds: List<Long>) -> Unit,
    ...
)
```

**Dialog content changes:**
```kotlin
AlertDialog(
    onDismissRequest = onDismiss,
    title = {
        Text(text = stringResource(R.string.add_to_playlist))
    },
    text = {
        Column(...) {
            // Header with context
            Row(...) {
                Icon(Icons.Default.MusicNote, ...)
                Column(...) {
                    Text(collectionName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = pluralStringResource(R.plurals.tracks_count, trackCount, trackCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            
            // Existing playlist list (unchanged logic, pass trackIds list)
            // New playlist form (unchanged, pass trackIds list)
        }
    },
    // ... confirm/dismiss buttons
)
```

**Rationale:**
- Show collection name + track count instead of single track (UX requirement)
- Pass `trackIds` list instead of single `trackId`
- Reuse same dialog UI, just generalize contract (FR7: avoid two pickers)

#### 3.2 Update `AddToPlaylistDialog` implementation

```kotlin
@Composable
fun AddToPlaylistDialog(
    collectionName: String,
    trackCount: Int,
    trackIds: List<Long>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (playlistId: Long, trackIds: List<Long>) -> Unit,
    onCreatePlaylist: (name: String, trackIds: List<Long>) -> Unit,
    viewModel: PlaylistsViewModel = viewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    
    // ... dialog content with context header ...
    
    AlertDialog(
        // ...
        confirmButton = {
            if (showCreateNew) {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName, trackIds)
                            onDismiss()
                        }
                    },
                    enabled = newPlaylistName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.create))
                }
            } else if (playlists.isNotEmpty()) {
                // No confirm button needed when selecting existing playlist
                // Selection immediately triggers onPlaylistSelected
            }
        },
        // ...
    )
}
```

#### 3.3 Update `MusicPlayerApp.kt` to use new dialog

```kotlin
// Add state for collection dialog
var collectionForPlaylistDialog by remember {
    mutableStateOf<CollectionForPlaylistDialog?>(null)
}

data class CollectionForPlaylistDialog(
    val collectionName: String,
    val trackIds: List<Long>,
)

// Update LibraryArtistDetailScreen callback
LibraryArtistDetailScreen(
    // ...
    onAddToPlaylist = { track ->
        collectionForPlaylistDialog = CollectionForPlaylistDialog(
            collectionName = displayName,
            trackIds = viewModel.tracksForArtist(artistKey).map { it.id },
        )
    },
    // ...
)

// Update LibraryAlbumDetailScreen callback
LibraryAlbumDetailScreen(
    // ...
    onAddToPlaylist = { track ->
        collectionForPlaylistDialog = CollectionForPlaylistDialog(
            collectionName = album.displayTitle,
            trackIds = viewModel.tracksForAlbum(album).map { it.id },
        )
    },
    // ...
)

// Show collection dialog
collectionForPlaylistDialog?.let { collection ->
    AddToPlaylistDialog(
        collectionName = collection.collectionName,
        trackCount = collection.trackIds.size,
        trackIds = collection.trackIds,
        onDismiss = { collectionForPlaylistDialog = null },
        onPlaylistSelected = { playlistId, trackIds ->
            playlistsViewModel.addCollectionToPlaylist(
                playlistId = playlistId,
                collectionName = collection.collectionName,
                trackIds = trackIds,
            )
        },
        onCreatePlaylist = { name, trackIds ->
            playlistsViewModel.addCollectionToPlaylist(
                playlistId = null,
                collectionName = name,
                trackIds = trackIds,
            )
        },
        viewModel = playlistsViewModel,
    )
}

// Show operation result snackbar
val operationState by playlistsViewModel.operationState.collectAsState()
LaunchedEffect(operationState) {
    when (operationState) {
        is PlaylistOperationState.Success -> {
            val result = (operationState as PlaylistOperationState.Success).result
            val message = if (result.addedCount > 0) {
                if (result.skippedCount > 0) {
                    "${result.addedCount} added · ${result.skippedCount} already there"
                } else {
                    pluralStringResource(R.plurals.tracks_added, result.addedCount, result.addedCount)
                }
            } else {
                if (result.skippedCount > 0) {
                    "Already in playlist"
                } else {
                    "No tracks to add"
                }
            }
            snackbarHostState.showSnackbar(message)
            playlistsViewModel.clearOperationState()
        }
        is PlaylistOperationState.Error -> {
            snackbarHostState.showSnackbar((operationState as PlaylistOperationState.Error).message)
            playlistsViewModel.clearOperationState()
        }
        else -> Unit
    }
}
```

**Rationale:**
- Pass all collection tracks to dialog (FR1: collection-level action)
- Show operation result via snackbar (AC6, FR5)
- Clear state after showing result (prevent duplicate feedback)

#### 3.4 Add new strings to `strings.xml`

```xml
<string name="add_to_playlist">Add to playlist</string>
<string name="tracks_added">Added %d tracks</string>
<plurals name="tracks_added">
    <item quantity="one">Added %d track</item>
    <item quantity="other">Added %d tracks</item>
</plurals>
```

**Rationale:** Pluralization required by NFR5

#### 3.5 Add collection action to detail screens

**Update `LibraryCollectionDetailScreen.kt`:**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryCollectionDetailContent(
    title: String,
    subtitle: String,
    tracks: List<TrackInfo>,
    favoriteIds: Set<Long>,
    onBackClick: () -> Unit,
    onPlayAll: (List<TrackInfo>, Int) -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
    onAddCollectionToPlaylist: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onExcludeArtist: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // ... existing top bar ...
    
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = { onPlayAll(tracks, 0) },
            modifier = Modifier.testTag("library_detail_play_all"),
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.padding(end = 4.dp))
            Text(stringResource(R.string.play_all))
        }
        
        OutlinedButton(
            onClick = onAddCollectionToPlaylist,
            modifier = Modifier.testTag("library_detail_add_to_playlist"),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 4.dp))
            Text(stringResource(R.string.add_to_playlist))
        }
    }
    
    // ... existing track list ...
}
```

**Update screen wrappers:**

```kotlin
fun LibraryArtistDetailScreen(
    // ...
    onAddToPlaylist: (TrackInfo) -> Unit,
    onAddCollectionToPlaylist: () -> Unit,
    // ...
) {
    LibraryCollectionDetailContent(
        // ...
        onAddToPlaylist = onAddToPlaylist,
        onAddCollectionToPlaylist = onAddCollectionToPlaylist,
        // ...
    )
}

fun LibraryAlbumDetailScreen(
    // ...
    onAddToPlaylist: (TrackInfo) -> Unit,
    onAddCollectionToPlaylist: () -> Unit,
    // ...
) {
    LibraryCollectionDetailContent(
        // ...
        onAddToPlaylist = onAddToPlaylist,
        onAddCollectionToPlaylist = onAddCollectionToPlaylist,
        // ...
    )
}
```

**Rationale:**
- Add "Add to playlist" button beside "Play all" (UX requirement)
- Use `OutlinedButton` to distinguish from primary "Play all" action
- Pass collection action callback separately (FR1)

---

### 4. Test scenarios

#### Unit tests

**`PlaylistRepositoryTest`:**
- Empty input returns `(0, 0)`
- Duplicate input IDs are deduplicated
- Some tracks already present → correct `addedCount`/`skippedCount`
- All tracks already present → `(0, N)`
- Deterministic append positions (position = max + 1, +2, ...)
- Concurrent calls don't violate uniqueness constraint

**`PlaylistsViewModelTest`:**
- Empty track list → `Error` state with message
- Duplicate submit during loading → no second operation
- Successful existing playlist → `Success` with counts
- Successful new playlist creation → `Success` with counts
- Database failure → `Error` state
- State clears after `clearOperationState()`

#### UI tests

**`AddToPlaylistDialogTest`:**
- Dialog shows collection name + track count
- Existing playlist selection calls `onPlaylistSelected` with list
- New playlist creation calls `onCreatePlaylist` with list
- Empty collection prevents submission (disabled create button)
- Dismissing picker doesn't modify playlist

**`LibraryCollectionDetailScreenTest`:**
- Artist detail shows "Add to playlist" button
- Album detail shows "Add to playlist" button
- Button click opens dialog with correct context
- Operation success shows snackbar with counts

#### E2E tests

**`CollectionAddToPlaylistE2ETest`:**
1. Open artist detail → tap "Add to playlist" → select existing playlist
2. Open playlist detail → verify all artist tracks present in order
3. Repeat same artist → verify no duplicates
4. Create playlist from album → verify every album track appears once
5. Verify partial no-op message: "8 added · 3 already there"

---

### 5. Files to modify

| File | Changes |
|------|---------|
| `app/src/main/java/com/anplak/androidmusic/data/db/PlaylistDao.kt` | Add `getExistingTrackIds()` query |
| `app/src/main/java/com/anplak/androidmusic/data/PlaylistRepository.kt` | Add `addTracksToPlaylist()` method + `PlaylistOperationResult` |
| `app/src/main/java/com/anplak/androidmusic/ui/PlaylistsViewModel.kt` | Add `PlaylistOperationState`, `addCollectionToPlaylist()`, `clearOperationState()` |
| `app/src/main/java/com/anplak/androidmusic/ui/AddToPlaylistDialog.kt` | Generalize for multiple tracks |
| `app/src/main/java/com/anplak/androidmusic/ui/LibraryCollectionDetailScreen.kt` | Add collection action button |
| `app/src/main/java/com/anplak/androidmusic/ui/MusicPlayerApp.kt` | Wire collection dialog + show operation result |
| `app/src/main/res/values/strings.xml` | Add `tracks_added` string |
| `app/src/test/java/com/anplak/androidmusic/data/PlaylistRepositoryTest.kt` | Add bulk operation tests |
| `app/src/test/java/com/anplak/androidmusic/ui/PlaylistsViewModelTest.kt` | Add collection operation tests |
