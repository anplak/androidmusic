### User story

As a user, I want **less duplicated chrome** at the top of main tabs so more of the screen shows music content, without losing the ability to tell where I am or reach Search / Library Index actions.

### Description

Every main tab currently repeats its name in both the **bottom navigation label** and a full **Material TopAppBar** (`primaryContainer`), consuming a large upper band. Library also uses the long label **“Your Library”**, which does not fit cleanly on one line in the nav. This story **densifies navigation chrome**: slim or remove redundant tab titles, keep essential actions, and rename Library for fit — without redesigning color/typography (story 19) or artwork (story 20).

**Prerequisite:** stories 01–13 (existing tab screens and Library sub-tabs). Story **17** (mini player) recommended so bottom padding stays correct after chrome changes.

### Goals & scope (2–3 days)

- **In scope**
  - **Remove or collapse redundant TopAppBars** on main tabs where the bottom nav already identifies the destination (For You, Library, Favorites, Playlists, History).
  - **Preserve actions**: Library Search and Library Index (and any other tab actions today) remain reachable — via a compact top action row, toolbar without large title, or equivalent.
  - **Rename** bottom-nav / short label from “Your Library” → **“Library”** (`strings.xml`); keep longer title only where a full header is still needed (e.g. Library Index screen).
  - **Consistent content top inset** using existing `Dimens` / scaffold padding so lists align across tabs.
- **Out of scope**
  - Full dark/geek theme tokens (story 19).
  - Mini player behavior (story 17).
  - Artwork / logo (story 20).
  - Changing the set of bottom tabs or tab order.
  - Redesigning Library Index popup/snackbar copy (Search & playlist management feature set).

### Acceptance criteria

- **AC1**: On each main tab, the page name is **not** shown twice as a large TopAppBar title plus identical bottom-nav label.
- **AC2**: User can still identify the active tab via bottom navigation selection state (and any remaining compact header/actions).
- **AC3**: Library Search and Library Index remain reachable in ≤ 1 tap from the Library tab.
- **AC4**: Bottom nav label for Library reads **“Library”** and fits one line at default font scale on a phone-width device.
- **AC5**: Detail / overlay screens that need a back affordance keep a clear top bar with title (artist, album, playlist, Search, etc.) — this story does not strip those.
- **AC6**: Usability parity: no primary tab action that existed before is removed without an equivalent control.

### Functional requirements

- **FR1**: Refactor main-tab scaffolds (`ForYouScreen`, `LibraryScreen`, `FavoritesScreen`, `PlaylistsScreen`, `HistoryScreen`) to drop full-title `TopAppBar` duplication or replace with a compact action bar.
- **FR2**: Update `NavigationTab.Library` string resource to short **Library**; audit other UI copy that assumed “Your Library” as the tab name.
- **FR3**: Library sub-tabs (Tracks / Artists / Albums) remain; they may sit at the top of content without an extra large title above them.
- **FR4**: Apply window/scaffold insets so content is not drawn under status bar after TopAppBar removal.
- **FR5**: For You section headers inside the feed (e.g. “Daily Mix 1”) stay — they are content structure, not chrome duplication.

### Non-functional requirements

- **NFR1**: Chrome change must not add measurable jank on tab switch (same composition cost band as today).
- **NFR2**: Support font scale up to 1.3 without clipping the Library nav label.
- **NFR3**: No new navigation library; keep existing `currentTab` / `AppScreen` model.

### UX design

- **Principle**: Bottom nav is the primary wayfinding; main tabs favor **content-first** layout.
- **Actions**: Icon buttons (Search, Index, overflow) aligned trailing in a slim top row (~48–56 dp) or integrated next to Library `TabRow` — not a second headline.
- **Playlists**: Keep in-content section labels (“Smart Playlists”, “Your Playlists”) once the large “Playlists” app bar title is gone.
- **Geek-minimal direction (layout only)**: Prefer flat surfaces and hairline separators over tall tinted app bars; color system still default until story 19.
- **Safe areas**: Status-bar padding mandatory after removing `TopAppBar`.

### Testing & validation

- **Compose UI / screenshot**: Each main tab — no duplicate large title; actions present on Library.
- **Manual**: Phone + small width / large font — Library label single-line; Search and Index still open.
- **Regression**: Detail screens still show back + title; mini player (if story 17 shipped) still clears content correctly.
- **Accessibility**: TalkBack still announces selected tab via nav items.

### Out of scope

- Rewriting empty-state marketing copy.
- Bottom-nav icon redesign.
- Collapsing Library Tracks/Artists/Albums into a different IA.
- Indexing progress UI redesign (owned by Search & playlist management refine set).

---

### Overall app state after this story

Main tabs reclaim vertical space; Library labeling fits the nav; wayfinding relies on bottom navigation plus compact actions.

### Value added after this story

Addresses “lots of space in the upper part with duplicated page titles” from `features_to_refine.md` while keeping the same task reachability.

---

## Implementation plan

### Current state (relevant)

- Each main tab (`ForYouScreen`, `LibraryScreen`, `FavoritesScreen`, `PlaylistsScreen`, `HistoryScreen`) owns a nested `Scaffold` with a full Material3 `TopAppBar` (`primaryContainer`) whose title duplicates the bottom-nav label.
- Tab actions live in those TopAppBars:
  - **For You**: Refresh (`for_you_refresh`)
  - **Library**: Library Index + Search (`open_library_index`, `open_search`)
  - **Playlists**: Search (`open_search`); FAB for create stays
  - **Favorites / History**: title only — no actions
- `NavigationTab.Library` uses `R.string.your_library` → **"Your Library"** (long for nav width / font scale).
- Overlay / detail screens (`Search`, `LibraryIndex`, artist/album/playlist/recommendation detail, Now Playing, Insights) already use titled TopAppBars with back — **leave unchanged** (AC5).
- Story 17 mini player is in `MainTabsContent.bottomBar`; tab chrome changes must not disturb bottom padding.

No navigation-model or ViewModel API changes (NFR3).

---

### Step 1 — Rename Library nav label (FR2, AC4, NFR2)

In `strings.xml`:

```xml
<string name="library">Library</string>
<!-- keep your_library only if any remaining non-tab copy needs it; otherwise remove after audit -->
```

Point nav at the short string:

```kotlin
enum class NavigationTab(val icon: ImageVector, val labelResId: Int) {
    ForYou(Icons.Default.Explore, R.string.for_you),
    Library(Icons.Default.LibraryMusic, R.string.library),
    // ...
}
```

Audit: only `NavigationTab.Library` and the Library TopAppBar title use `your_library` today. After TopAppBar removal, nav is the sole consumer of the short label. Leave `library_index`, empty-state, and “Filter your library…” copy alone (out of scope).

**Rationale:** Short **Library** fits one line at default width and ~1.3 font scale (AC4, NFR2) without changing tab order or icons.

---

### Step 2 — Shared compact top chrome helper (FR1, FR4, UX)

Add a small reusable row (same package as screens, e.g. in `MainTabChrome.kt` or next to `Dimens`):

```kotlin
@Composable
fun CompactTabActions(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.compactTabActionsHeight) // 48.dp
                .statusBarsPadding()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
        HorizontalDivider()
    }
}
```

Add `Dimens.compactTabActionsHeight = 48.dp`.

**Rationale:** One density token for tabs that still need actions; hairline separator matches “geek-minimal / layout only” UX without story-19 color work. `statusBarsPadding()` on the action row (or on content when there is no row) satisfies FR4 after TopAppBar removal.

Alternative if Scaffold already feeds top inset via `paddingValues` with no `topBar`: omit `statusBarsPadding()` here and rely on inner Scaffold padding — verify once under edge-to-edge; pick **one** inset owner to avoid double padding.

---

### Step 3 — Tabs with no actions: drop TopAppBar (FR1, AC1, AC2)

**Favorites** and **History**: remove `topBar` entirely; keep content `Scaffold { paddingValues -> … }` (or flatten to a `Box` + outer padding if Scaffold becomes empty). Ensure top inset via Step 2 rule.

```kotlin
// FavoritesScreen / HistoryScreen — no topBar
Scaffold(modifier = modifier) { paddingValues ->
    Box(Modifier.fillMaxSize().padding(paddingValues)) {
        // existing content states unchanged
    }
}
```

**Rationale:** Bottom nav already identifies the tab (AC2). Removing a title-only bar is the largest vertical win and meets AC1 with zero action migration (AC6).

---

### Step 4 — For You & Playlists: compact actions, no title (FR1, AC6)

Replace titled `TopAppBar` with `CompactTabActions` (or a title-less `TopAppBar` with `colors = surface` / transparent — prefer the compact row for flat look).

**For You:**

```kotlin
Scaffold(
    topBar = {
        CompactTabActions {
            IconButton(
                onClick = { viewModel.refresh() },
                modifier = Modifier.testTag("for_you_refresh")
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh_recommendations)
                )
            }
        }
    },
    modifier = modifier
) { paddingValues -> /* unchanged body; section headers stay (FR5) */ }
```

**Playlists:** same pattern for Search (`open_search`); keep FAB + in-content “Smart Playlists” / “Your Playlists” headers (UX).

**Rationale:** Preserves ≤1-tap reachability (AC6) without a second “Playlists” / “For You” headline (AC1). Existing test tags stay so E2E helpers keep working.

---

### Step 5 — Library: TabRow + trailing actions (FR1, FR3, AC3)

Drop the “Your Library” TopAppBar. Put Index + Search in a slim trailing row above or aligned with `LibraryBrowseTabs` (Tracks / Artists / Albums):

```kotlin
Scaffold(
    snackbarHost = { /* unchanged scan snackbar */ },
    modifier = modifier
) { paddingValues ->
    Column(Modifier.fillMaxSize().padding(paddingValues)) {
        CompactTabActions {
            IconButton(
                onClick = onOpenLibraryIndex,
                modifier = Modifier.testTag("open_library_index")
            ) { /* FilterList + library_index CD */ }
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.testTag("open_search")
            ) { /* Search + search CD */ }
        }
        LibraryBrowseTabs(/* unchanged */)
        // refresh indicator / sync retry / browse content unchanged
    }
}
```

Optional denser variant: single `Row` with `TabRow(Modifier.weight(1f))` + the two `IconButton`s on the trailing edge (no second horizontal band). Prefer that if the action row + TabRow feels tall on small phones; either way actions remain ≤1 tap (AC3).

**Rationale:** Sub-tabs stay as primary Library chrome (FR3); Search / Index stay on the Library tab with the same tags (AC3, AC6). No large duplicated title.

---

### Step 6 — Insets & consistency pass (FR4)

| Surface | Top inset owner |
|---------|-----------------|
| Main tabs with `CompactTabActions` | Action row (`statusBarsPadding` **or** Scaffold `paddingValues`, not both) |
| Favorites / History (no actions) | Scaffold `paddingValues` / content `statusBarsPadding` |
| Overlays with TopAppBar | Unchanged — TopAppBar consumes status bar (AC5) |
| Mini player + nav | Unchanged (story 17) |

Spot-check all five tabs: first list row / TabRow aligns to a consistent top band; content not under status bar; no double top gap after removing `primaryContainer` bars.

**Rationale:** FR4 without introducing a new inset framework; keeps story 17 bottom padding intact.

---

### Step 7 — Leave detail chrome alone (AC5)

Do **not** strip TopAppBars from:

- `SearchScreen`, `LibraryIndexScreen`
- `LibraryCollectionDetailScreen`, `PlaylistDetailScreen`, `SmartPlaylistDetailScreen`, `RecommendationDetailScreen`
- `NowPlayingScreen`, `InsightsScreen`

**Rationale:** Those titles are destination identity + back affordance, not bottom-nav duplicates (AC5).

---

### Key decisions

| Decision | Rationale |
|----------|-----------|
| Remove title-only TopAppBars on Favorites / History | AC1 with no action migration; max content reclaim |
| Compact action row (not titled TopAppBar) on For You / Playlists / Library | AC3 / AC6 — keep Refresh, Search, Index; drop duplicate headlines |
| Library actions next to / above browse `TabRow` | FR3 — sub-tabs become primary chrome; fits “content-first” UX |
| New `library` string for nav; drop TopAppBar use of `your_library` | FR2, AC4 — short label without rewriting Index / empty-state copy |
| Flat surface + `HorizontalDivider` | UX “geek-minimal layout only”; colors remain default until story 19 |
| Keep overlay / detail TopAppBars | AC5 — back + title still required |
| Preserve existing test tags (`open_search`, `open_library_index`, `for_you_refresh`, `nav_*`) | Avoid churn in E2E helpers; tags move with the IconButtons |
| No Navigation Compose / tab model change | NFR3 |
| Do not touch mini-player host | Story 17 ownership; only ensure top chrome changes do not fight bottom padding |

---

### Implementation checklist

- [x] `R.string.library` = `"Library"`; `NavigationTab.Library` uses it; audit/remove `your_library` if unused
- [x] `Dimens.compactTabActionsHeight` (+ shared `CompactTabActions` if used on ≥2 screens)
- [x] Favorites + History: remove TopAppBar; status-bar inset OK
- [x] For You: Refresh in compact chrome; no large title; section headers intact
- [x] Playlists: Search in compact chrome; FAB + section headers intact
- [x] Library: Search + Index reachable; browse TabRow without large “Your Library” title
- [x] Confirm overlays/detail TopAppBars unchanged
- [ ] Manual: five tabs + font scale ~1.3 Library label; Search/Index from Library; mini player still clears lists

---

### Test scenarios

**Compose / UI**
- Each main tab: no large TopAppBar title matching the bottom-nav label (AC1).
- Library: `open_search` and `open_library_index` still present and open Search / Library Index (AC3).
- For You: `for_you_refresh` still present; feed section titles (e.g. Daily Mix) still shown (FR5).
- Playlists: Search action + create FAB still present; “Smart Playlists” / “Your Playlists” headers remain.

**Manual**
- Phone width + font scale ~1.3: bottom-nav **Library** single-line, no clip (AC4, NFR2).
- After TopAppBar removal: content not under status bar on all five tabs (FR4).
- Active tab still clear from nav selection (AC2).

**Regression**
- Detail / Search / Library Index: back + title TopAppBar unchanged (AC5).
- Mini player (if present): lists still clear the bar; tab switch not jankier than today (NFR1).
- Existing E2E paths that click `nav_library`, `open_search`, `open_library_index`, `for_you_refresh` still find those tags.
