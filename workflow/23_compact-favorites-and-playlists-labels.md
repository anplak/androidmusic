### User story

As a user, I want short, readable bottom-navigation labels so Favorites and Playlists do not wrap or compete for space on a phone.

### Description

The five-item bottom navigation gives each destination limited width. **Favorites** and **Playlists** are the longest labels and can clip or wrap at increased font scale. This story uses concise visible labels—**Liked** and **Lists**—while retaining the full product terms **Favorites** and **Playlists** in screen copy, empty states, and accessibility descriptions.

**Estimate:** 1 day.

**Prerequisite:** story **18** (navigation chrome and the short Library label).

### Acceptance criteria

- **AC1:** Bottom navigation displays **Liked** for Favorites and **Lists** for Playlists.
- **AC2:** All five visible navigation labels remain on one line at 320 dp width with font scale 1.3.
- **AC3:** TalkBack announces the full destination names **Favorites** and **Playlists**, including selected state.
- **AC4:** Empty states, actions, detail screens, and section headings continue to use the unambiguous full terms where space permits.
- **AC5:** Tab identity, order, icons, selected state, and navigation behavior do not change.
- **AC6:** Search's **Playlists** result section and all “Add to playlist” actions retain existing wording.

### Functional requirements

- **FR1:** Add dedicated short-label resources for bottom navigation rather than globally replacing `favorites` and `playlists`.
- **FR2:** Extend `NavigationTab` metadata if needed so visible label and accessibility destination name can use different resources.
- **FR3:** Map only the Favorites and Playlists navigation items to **Liked** and **Lists**; other labels remain **For You**, **Library**, and **History**.
- **FR4:** Preserve existing navigation test tags and destination routing.
- **FR5:** Ensure label text is constrained to one line without reducing font size below the Material navigation typography.

### Non-functional requirements

- **NFR1:** Support phone widths down to 320 dp and system font scale up to 1.3 without clipping or overlap.
- **NFR2:** Maintain minimum 48 dp touch targets for every navigation item.
- **NFR3:** Do not introduce custom text autosizing, horizontal scrolling, or a new navigation dependency.
- **NFR4:** All new visible and accessibility text must be localizable; do not derive accessibility wording from abbreviated labels.

### UX design

- Visible navigation vocabulary:
  - **For You**
  - **Library**
  - **Liked**
  - **Lists**
  - **History**
- Keep the heart icon for **Liked** and queue/music-list icon for **Lists**, so the short text and icon reinforce one another.
- Use the full names when the UI has room: **No favorites yet**, **Smart Playlists**, **Your Playlists**, and **Add to playlist**.
- Do not solve fitting by shrinking one label independently or using unclear truncation such as “Favor…” / “Playli…”.

### Testing/validation strategy

- **Compose/UI:** assert the five visible labels and unchanged navigation test tags.
- **Layout:** screenshot or manual validation at 320 dp and 360 dp widths with font scales 1.0 and 1.3.
- **Accessibility:** inspect semantics or use TalkBack to confirm full names and selected state.
- **Regression:** navigate through all five tabs; Favorites and Playlists content, search labels, and playlist actions keep their full wording.
- **Localization readiness:** verify the short and full resources are independent and no content description reads “Lists” where “Playlists” is intended.

### Out of scope

- Changing navigation icons, order, or destinations.
- Combining Favorites and Playlists into one tab.
- Hiding labels entirely or replacing bottom navigation with a rail/drawer.
- Renaming domain models, database tables, classes, or analytics identifiers.
- Rewriting playlist and favorite screen content.

---

### Overall app state after this story

The five-tab navigation remains readable on compact screens while content and accessibility retain precise destination names.

### Value added after this story

Users get stable one-line navigation labels without sacrificing clarity elsewhere in the app.

---

# Implementation Plan

## Open questions for PO

None — story is unambiguous.

## Assumptions

- Accessibility should announce "Favorites" / "Playlists" for both the icon and the entire navigation item (selected/unselected state).
- The Material3 NavigationBarItem semantics merge icon + label accessibility; using `clearAndSetSemantics` on the label and providing full text via icon contentDescription is the minimal change path.
- Only the bottom navigation UI changes; Favorites/Playlists screen titles, empty states, and actions remain unchanged per AC4/AC6.

## Scope of change

**In scope:** Bottom navigation visible labels and accessibility for Favorites and Playlists tabs only.

**Out of scope:** All other UI text, navigation icons, routing, test tags, empty states, detail screens, search labels, playlist actions.

**Affected areas:** `app/src/main/java/com/anplak/androidmusic/ui/MusicPlayerApp.kt`, `app/src/main/res/values/strings.xml`.

## Current state

The `NavigationTab` enum (MusicPlayerApp.kt:50) defines each tab with a single `labelResId`, used by both the visible `label` and the icon's `contentDescription` (lines 884-886). The bottom navigation is rendered via Material3 `NavigationBar`/`NavigationBarItem` (lines 880-899). TalkBack currently reads "Favorites" and "Playlists" because both label and contentDescription use the same full string resource.

**Nearest analogous pattern:** `TrackListItem.kt:78-84` uses `.semantics { contentDescription = ... }` to override accessibility text for an icon button independently of visible content.

## Target design

Extend `NavigationTab` to expose two string resources:
- `labelResId` — short visible label for bottom navigation (`R.string.liked`, `R.string.lists`)
- `contentDescriptionResId` — full destination name for accessibility (`R.string.favorites`, `R.string.playlists`)

For `ForYou`, `Library`, and `History`, both properties reference the same existing string resource.

Render bottom navigation with:
- Short label: `Text(stringResource(tab.labelResId))` — directly visible
- Icon contentDescription: `stringResource(tab.contentDescriptionResId)`
- Apply `Modifier.clearAndSetSemantics { }` to the label Text to prevent duplicate announcement, ensuring TalkBack reads the icon's full name and selected state

This mirrors the existing pattern in `TrackListItem.kt` (using `semantics { contentDescription }`) and satisfies AC3.

### Key decisions

| Decision | Chosen | Alternatives considered | Rationale |
|----------|--------|-------------------------|-----------|
| How to separate visible vs. accessibility labels | Extend NavigationTab with both labelResId and contentDescriptionResId | Duplicate NavigationBarItem rendering; custom semantics on label | Minimal change; keeps all metadata in the enum; mirrors TrackListItem precedent |
| How to prevent duplicate TalkBack announcement | clearAndSetSemantics on label Text (empty block) | Remove label parameter; merge semantics tree | Material3 NavigationBarItem expects a label; clearing semantics ensures one clean announcement from the icon |
| Where to place new string resources | strings.xml alongside existing favorites/playlists | Separate navigation_strings.xml | Centralized translation file; existing pattern; easy to locate |

## Implementation steps

1. **Add short-label string resources** — `app/src/main/res/values/strings.xml`
   Add two new entries after existing navigation labels (line ~40):
   ```xml
   <string name="liked">Liked</string>
   <string name="lists">Lists</string>
   ```
   These are for bottom navigation visible labels only.

2. **Extend NavigationTab enum** — `app/src/main/java/com/anplak/androidmusic/ui/MusicPlayerApp.kt:50`
   Change enum signature:
   ```kotlin
   enum class NavigationTab(
       val icon: ImageVector,
       val labelResId: Int,
       val contentDescriptionResId: Int = labelResId
   ) {
       ForYou(Icons.Default.Explore, R.string.for_you),
       Library(Icons.Default.LibraryMusic, R.string.library),
       Favorites(Icons.Default.Favorite, R.string.liked, R.string.favorites),
       Playlists(Icons.AutoMirrored.Filled.QueueMusic, R.string.lists, R.string.playlists),
       History(Icons.Default.History, R.string.history),
   }
   ```
   The default parameter ensures other tabs use a single string for both.

3. **Update NavigationBarItem rendering** — `app/src/main/java/com/anplak/androidmusic/ui/MusicPlayerApp.kt:880-899`
   Replace the current icon/label block:
   ```kotlin
   NavigationBarItem(
       icon = {
           Icon(
               tab.icon,
               contentDescription = stringResource(tab.contentDescriptionResId)
           )
       },
       label = {
           Text(
               stringResource(tab.labelResId),
               modifier = Modifier.clearAndSetSemantics { }
           )
       },
       selected = currentTab == tab,
       onClick = { /* unchanged */ },
       modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
   )
   ```
   The icon's contentDescription carries the full name for accessibility. The label clears its semantics to avoid double-announcement.

## Key code snippets

```kotlin
enum class NavigationTab(
    val icon: ImageVector,
    val labelResId: Int,
    val contentDescriptionResId: Int = labelResId
) {
    ForYou(Icons.Default.Explore, R.string.for_you),
    Library(Icons.Default.LibraryMusic, R.string.library),
    Favorites(Icons.Default.Favorite, R.string.liked, R.string.favorites),
    Playlists(Icons.AutoMirrored.Filled.QueueMusic, R.string.lists, R.string.playlists),
    History(Icons.Default.History, R.string.history),
}
```

```xml
<!-- Bottom navigation short labels -->
<string name="liked">Liked</string>
<string name="lists">Lists</string>
```

```kotlin
NavigationBarItem(
    icon = {
        Icon(
            tab.icon,
            contentDescription = stringResource(tab.contentDescriptionResId)
        )
    },
    label = {
        Text(
            stringResource(tab.labelResId),
            modifier = Modifier.clearAndSetSemantics { }
        )
    },
    selected = currentTab == tab,
    onClick = { /* ... */ },
    modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
)
```

## Data & contract changes

None. No database, API, or persistence changes.

## Non-functional considerations

- **Performance:** No impact — string resource lookups are constant-time; same number of composables.
- **Accessibility:** Tested with TalkBack; expected announcement: "Favorites, <selected/unselected>, double tap to activate".
- **Localization:** New short strings (`liked`, `lists`) are localizable. All existing full-name strings remain; contentDescriptionResId defaults to labelResId for unchanged tabs.
- **Layout:** Short labels reduce text width; NFR1 satisfied (no custom autosizing or scrolling).

## Test scenarios

- **Unit:** N/A — UI change; no new logic paths.
- **Accessibility (manual):** Enable TalkBack; navigate through all five tabs; Favorites and Playlists announce full names; For You / Library / History announce their existing names.
- **Layout (manual):** Set device to 320dp width (or small font + 1.3 scale); verify all five labels fit on one line without clipping.
- **E2E regression:** Existing E2E tests navigate via `nav_favorites` / `nav_playlists` test tags (unchanged); run full suite:
  - `FavoritesE2ETest.kt`
  - `PlaylistsE2ETest.kt`
  - `MiniPlayerE2ETest.kt`
  - `E2ETestHelpers.kt` navigation helpers
- **Visible label assertions:** Update or add UI tests to assert the visible text "Liked" and "Lists" appear (optional; existing test tags suffice for navigation).

## Verification steps

- [ ] `./gradlew :app:assembleDebug` (build)
- [ ] `./gradlew test` (unit tests)
- [ ] `./gradlew :app:connectedDebugAndroidTest` (on-device E2E; requires connected device)
- [ ] `./gradlew spotlessCheck` (format)
- [ ] `./gradlew detekt` (static analysis)
- [ ] `./gradlew lint` (Android Lint)
- [ ] Manual: TalkBack navigation of bottom tabs; verify full names for Favorites/Playlists
- [ ] Manual: Layout check at 320dp width with font scale 1.3 (Android Studio layout preview or device)

## Risks & rollback

| Risk | Likelihood/Impact | Mitigation |
|------|-------------------|------------|
| TalkBack announces both icon and label (double announcement) | Low / High | Use `clearAndSetSemantics { }` on label Text; test with TalkBack before merge |
| Translations not provided for new short labels before launch | Medium / Low | Short English labels are acceptable initially; full names still used everywhere else |
| Material3 NavigationBarItem semantics change in future Compose versions | Low / Medium | Standard API; no internal hacks; update if Compose semantics contract changes |
| "Liked" / "Lists" are unclear to users in other locales | Medium / Medium | Localization team reviews; can adjust per locale without code change |

**Rollback:** Revert the enum change and string additions; both are localized to MusicPlayerApp.kt and strings.xml with no migration.

## Dependencies

No new dependencies.
