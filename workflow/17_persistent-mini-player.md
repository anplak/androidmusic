### User story

As a user, I want a **persistent mini player** with the current track title, Play/Pause, and Like available from any screen so I can control playback and reopen Now Playing without losing my place in the app.

### Description

Today, leaving full-screen Now Playing returns to the previous tab with **no in-app playback chrome** — background audio continues via `MusicPlaybackService`, but the only way back to player controls is to start another track. This story adds a **global mini player bar** above the bottom navigation (and on full-screen overlays that are not Now Playing) whenever a track session is active.

**Prerequisite:** stories 01–03 (playback + Now Playing); story 04 (favorites / Like).

### Goals & scope (2–3 days)

- **In scope**
  - **Mini player bar**: Shows current track title (and artist when space allows), Play/Pause, Like; tap body → Now Playing.
  - **Global visibility**: Shown on main tabs and on overlay screens (Search, Library Index, artist/album/playlist/recommendation detail) whenever `selectedTrack != null`.
  - **Hidden on Now Playing**: Full-screen player remains the only player surface there.
  - **Wire existing actions**: Play/Pause and Like call existing `PlaybackViewModel` APIs; no new playback engine.
- **Out of scope**
  - Seek bar, next/previous, queue UI in the mini bar.
  - Notification / MediaSession artwork (story 20).
  - Theme redesign (story 19) — use current Material tokens; layout must still look correct after theme lands.
  - Header density / TopAppBar removal (story 18).

### Acceptance criteria

- **AC1**: With an active track, every main tab shows the mini player above the bottom nav.
- **AC2**: Opening Search, Library Index, or any detail overlay still shows the mini player (when a track is selected).
- **AC3**: Tapping the mini player body (not the action buttons) opens Now Playing; back returns to the previous screen/tab with the mini player still visible.
- **AC4**: Play/Pause in the mini player toggles playback and stays in sync with Now Playing and the system notification.
- **AC5**: Like in the mini player toggles favorite state and matches the heart state on Now Playing / track rows.
- **AC6**: When no track is selected (cold start, never played), the mini player is absent and bottom nav layout is unchanged.
- **AC7**: Mini player does not appear on the full-screen Now Playing route.

### Functional requirements

- **FR1**: Add a shared `MiniPlayerBar` composable bound to `PlaybackViewModel.uiState` (`selectedTrack`, `isPlaying`, `isFavorite`).
- **FR2**: Host the bar in the app shell (`MusicPlayerApp` / `MainTabsContent`) so all tabs share one instance; pass content padding so lists are not obscured.
- **FR3**: For overlay `AppScreen`s that use their own `Scaffold`, reserve bottom inset for the mini player (same bar instance or equivalent slot) so it remains reachable.
- **FR4**: Title uses single-line ellipsis; prefer `title` primary, optional `artist` as secondary when width allows.
- **FR5**: Action hit targets ≥ 48 dp; button taps do not open Now Playing.
- **FR6**: Persist visibility rule: show iff `selectedTrack != null` (including paused).

### Non-functional requirements

- **NFR1**: Mini player recomposition must not restart playback or rebind the service.
- **NFR2**: Layout change (show/hide bar) must not drop scroll position on the active list beyond normal scaffold padding updates.
- **NFR3**: No new dependencies; Compose Material3 icons only.
- **NFR4**: Accessible labels for Play/Pause and Like (content descriptions).

### UX design

- **Placement**: Full-width bar directly above `NavigationBar` on main tabs; same visual height band on overlays above the system gesture/nav area.
- **Content (LTR)**: Leading optional art placeholder (neutral icon or story-20 art later) → title (+ artist) → Like → Play/Pause.
- **Density**: Compact (~56–64 dp tall); no cards, no heavy elevation — thin top divider or subtle surface tint only.
- **States**: Playing / paused icon swap; filled/outline heart for favorite.
- **Motion**: Optional short slide/fade when the bar first appears after a track is selected; keep subtle.

### Testing & validation

- **Compose UI**: Mini player visible with fixture selected track on each tab; absent when `selectedTrack == null`.
- **Compose UI**: Tap body → Now Playing; tap Play/Pause and Like invoke ViewModel (mocked).
- **Manual**: Start track → leave Now Playing → switch all five tabs → open Search/detail → controls remain correct; kill/recreate activity → session restore still shows bar when track restored.
- **Regression**: Now Playing full controls unchanged; bottom nav selection still works with bar present.

### Out of scope

- Lock-screen / Bluetooth control changes.
- Queue peek from mini player.
- Customizable mini-player actions.
- Artwork loading pipeline (story 20 may replace the leading placeholder).

---

### Overall app state after this story

Users always have a one-tap path back to the player and essential controls from anywhere in the app while a track session is active.

### Value added after this story

Closes the largest Look-and-Feel usability gap from `features_to_refine.md`: “always have an option to switch to player view from any screen.”

---

## Implementation plan

### Current state (relevant)

- `MusicPlayerApp` swaps full-screen destinations with a `when`: overlays (`Search`, `LibraryIndex`, detail screens) **replace** `MainTabsContent`; `NowPlaying` replaces everything.
- `MainTabsContent` owns the only `NavigationBar` via `Scaffold(bottomBar = …)`.
- `PlaybackViewModel.uiState` already exposes `selectedTrack`, `isPlaying`, `isFavorite`; actions `onPlayPause()` / `toggleFavorite()` are shared with Now Playing.
- Leaving Now Playing always sets `currentScreen = AppScreen.MainTabs`, so back from player **drops overlay context** today — AC3 needs a fix.

No new playback / ViewModel APIs required (FR1, NFR1).

---

### Step 1 — `MiniPlayerBar` composable (FR1, FR4, FR5, NFR3, NFR4)

Add `app/src/main/java/com/anplak/androidmusic/ui/MiniPlayerBar.kt` — pure UI, no ViewModel.

```kotlin
@Composable
fun MiniPlayerBar(
    title: String,
    artist: String?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onBarClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().testTag("mini_player_bar")) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.miniPlayerHeight) // e.g. 56.dp or 64.dp
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(40.dp).padding(4.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onBarClick)
                    .padding(horizontal = 8.dp)
                    .testTag("mini_player_title")
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!artist.isNullOrBlank()) {
                    Text(
                        text = artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("mini_player_favorite")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites
                    )
                )
            }
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.pause else R.string.play
                    )
                )
            }
        }
    }
}
```

**Rationale:** Title/artist column is the only `clickable` region so Like / Play-Pause never open Now Playing (FR5, AC3). Leading `MusicNote` is a story-20 placeholder. Reuse existing favorite/play strings (NFR4). Material3 `IconButton` hit targets are ≥ 48 dp.

Add `Dimens.miniPlayerHeight` (56–64 dp per UX).

---

### Step 2 — Host on main tabs (FR2, FR6, AC1, AC6)

Change `MainTabsContent` `bottomBar` to stack mini player above nav when a track is selected:

```kotlin
@Composable
private fun MainTabsContent(
    // ...existing params...
    playbackUiState: PlaybackUiState,
    onOpenNowPlaying: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val showMiniPlayer = playbackUiState.selectedTrack != null

    Scaffold(
        bottomBar = {
            Column {
                AnimatedVisibility(visible = showMiniPlayer) {
                    val track = playbackUiState.selectedTrack!!
                    MiniPlayerBar(
                        title = track.title,
                        artist = track.artist,
                        isPlaying = playbackUiState.isPlaying,
                        isFavorite = playbackUiState.isFavorite,
                        onBarClick = onOpenNowPlaying,
                        onPlayPauseClick = onPlayPause,
                        onToggleFavorite = onToggleFavorite
                    )
                }
                NavigationBar { /* unchanged */ }
            }
        }
    ) { paddingValues ->
        // unchanged tab content; Scaffold padding grows with bar (FR2)
    }
}
```

Wire from `MusicPlayerApp` with `playbackViewModel::onPlayPause` / `::toggleFavorite` and `onOpenNowPlaying` (Step 4).

**Rationale:** One bar instance for all five tabs; show iff `selectedTrack != null` including paused (FR6, AC6). Scaffold `paddingValues` keeps lists clear of the bar without per-screen hacks (NFR2).

---

### Step 3 — Same bar on overlay screens (FR3, AC2)

Overlays each own a full-screen `Scaffold`. Avoid editing every screen: wrap overlay branches in a thin host that only adds a bottom slot.

```kotlin
@Composable
private fun MiniPlayerOverlayHost(
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = { if (showMiniPlayer) miniPlayer() }
    ) { padding ->
        Box(Modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
```

Usage for Search / Library Index / playlist / smart playlist / recommendation / artist / album detail:

```kotlin
currentScreen is AppScreen.Search -> {
    MiniPlayerOverlayHost(
        showMiniPlayer = uiState.selectedTrack != null,
        miniPlayer = { /* same MiniPlayerBar binding as tabs */ }
    ) {
        SearchScreen(/* unchanged */)
    }
}
```

Do **not** wrap `NowPlaying` or the permission screen (AC7).

Optional: extract a small `@Composable fun rememberMiniPlayerSlot(...): @Composable () -> Unit` so tab + overlay bindings stay identical and drift-free.

**Rationale:** Nested Scaffold (outer bottomBar only, inner topBar) is the smallest change that keeps overlays reachable without threading `PaddingValues` through every screen API. Equivalent visual band above system nav (UX). Story 18 chrome work stays independent.

---

### Step 4 — Open Now Playing + restore previous screen (AC3)

Today back always forces `MainTabs`, which breaks “return to Search/detail with mini player still visible.”

```kotlin
var screenBeforeNowPlaying by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }

fun openNowPlaying() {
    if (currentScreen !is AppScreen.NowPlaying) {
        screenBeforeNowPlaying = currentScreen
    }
    currentScreen = AppScreen.NowPlaying
}

// Replace every `currentScreen = AppScreen.NowPlaying` with openNowPlaying()
// NowPlaying onBackClick:
onBackClick = { currentScreen = screenBeforeNowPlaying }
```

**Rationale:** One-frame “stack” without Navigation Compose. Mini-player tap and track-selection share the same entry path so back always restores the prior tab/overlay (AC3).

---

### Step 5 — Visibility / lifecycle guards (AC7, NFR1)

| Rule | Implementation |
|------|----------------|
| Show when `selectedTrack != null` | Gate in tab `bottomBar` + `MiniPlayerOverlayHost` |
| Hide on Now Playing | Now Playing branch stays outside hosts |
| Cold start, never played | `selectedTrack == null` → no bar (AC6) |
| Process death / activity recreate | Existing `AudioPlayer` / service session already restores queue → `uiState.selectedTrack` repopulates → bar reappears; no new persistence |
| Recomposition must not rebind service | Bar only reads `uiState` and calls existing VM methods; do not call `connect()` from UI |

Optional UX: `AnimatedVisibility` slide/fade on first appear; keep duration short (~150–200 ms).

---

### Key decisions

| Decision | Rationale |
|----------|-----------|
| Pure `MiniPlayerBar` + existing `PlaybackViewModel` | FR1 — no new playback engine; Now Playing stays source of truth for state |
| Stack mini player **above** `NavigationBar` in one `Column` | UX placement; single Scaffold padding for tabs (FR2) |
| `MiniPlayerOverlayHost` instead of per-screen API changes | FR3 / AC2 with minimal churn; overlays keep their TopAppBars |
| Clickable title column only | FR5 — action buttons do not navigate |
| `screenBeforeNowPlaying` remember | AC3 — back from Now Playing restores overlay/tab |
| Hide only on Now Playing + permission | AC7; all other `AppScreen`s share the bar |
| Neutral `MusicNote` leading icon | Story 20 will replace with artwork; layout already reserved |
| Current Material3 tokens / `surfaceContainer` | Story 19 out of scope; layout survives theme swap |
| No seek / next / prev in bar | Explicit out of scope |

---

### Implementation checklist

- [x] `Dimens.miniPlayerHeight` (+ optional horizontal paddings if needed)
- [x] `MiniPlayerBar.kt` with test tags `mini_player_bar`, `mini_player_title`, `mini_player_favorite`, `mini_player_play_pause`
- [x] `MainTabsContent` bottomBar = mini player + `NavigationBar`; pass playback callbacks
- [x] `MiniPlayerOverlayHost` wrapping all overlay branches in `MusicPlayerApp`
- [x] `openNowPlaying()` + `screenBeforeNowPlaying` for all Now Playing entry points and back
- [x] Confirm Now Playing branch has **no** mini player (AC7)
- [ ] Manual pass: five tabs + Search + one detail overlay + pause/like sync with notification

---

### Test scenarios

**Compose UI — `MiniPlayerBar`**
- Fixture track → bar shows title; Play/Pause and Like icons match `isPlaying` / `isFavorite`.
- `selectedTrack == null` path (host) → bar absent; nav layout unchanged (AC6).
- Tap title region → `onBarClick`; tap Play/Pause or Like → only those callbacks (FR5).

**Compose / E2E — shell**
- Active track → bar visible on each of the five tabs (AC1).
- Open Search / Library Index / playlist or album detail with active track → bar still visible (AC2).
- Tap bar body → Now Playing; back → previous screen still showing bar (AC3).
- Play/Pause on mini player → icon + Now Playing + notification stay in sync (AC4).
- Like on mini player → heart matches Now Playing / favorites rows (AC5).
- On Now Playing route → `mini_player_bar` not in tree (AC7).

**Regression**
- Now Playing full controls / seek / queue buttons unchanged.
- Bottom nav selection still works with bar present.
- Track row → Now Playing still works; service not rebound on tab switches (NFR1).
