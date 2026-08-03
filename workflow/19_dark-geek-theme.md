### User story

As a user, I want a **cohesive dark, geek-minimal look** so the app feels intentional and easy on the eyes for listening sessions, not like a stock Material light template.

### Description

The app wraps UI in a bare `MaterialTheme { }` (default light Material 3) with XML `Theme.Material.Light.NoActionBar`. Story **10** deferred theming. This story introduces a real **Compose design system** — dark-first “geek” aesthetic (monospace accents, restrained neon/phosphor accent, dense lists, flat surfaces) — and applies it across existing screens, including mini player and compact chrome from stories **17–18** when present.

**Prerequisite:** none hard; best after **18** so theme work is not redone on removed TopAppBars. Compatible with **17**.

### Goals & scope (2–3 days)

- **In scope**
  - **Design tokens**: `Color.kt`, `Type.kt`, `Theme.kt` under `ui/theme/` (extend existing `Dimens.kt`).
  - **Dark color scheme** as default; optionally follow system light/dark if cheap — dark must be first-class and polished.
  - **Typography**: Distinctive pair (e.g. clean sans for UI + monospaced for meta/timecodes/labels) — not the untouched default stack alone.
  - **Component pass**: Replace ad-hoc `primaryContainer` TopAppBar tints and letter-circle colors with theme roles; mini player / nav / lists read as one system.
  - **System bars**: Status/navigation bar colors match theme (edge-to-edge already enabled).
- **Out of scope**
  - User-facing theme picker beyond system follow (no in-app “accent color” settings).
  - Custom illustrated empty states or motion design system.
  - Launcher icon / wordmark (story 20).
  - Loading real album art (story 20) — theme the **placeholders** only.
  - Non-UI refactors.

### Acceptance criteria

- **AC1**: App launches into a dark (or system-dark) palette; no default purple-on-white Material look on main surfaces.
- **AC2**: All primary tab screens, Now Playing, Search, and detail screens use `MusicTheme` / shared `colorScheme` and typography — no screen stuck on unthemed defaults.
- **AC3**: Accent color is used sparingly (playing indicator, selected nav, primary CTAs) — not purple-washed full scaffolds.
- **AC4**: Text hierarchy is clear: title vs artist vs meta; meta may use monospace or subdued style consistently.
- **AC5**: Contrast meets readable standards for body text and icons on dark surfaces (WCAG AA for primary text where practical).
- **AC6**: XML application theme does not flash a bright white window background on cold start before Compose draws.

### Functional requirements

- **FR1**: Add `MusicTheme` composable; `MusicPlayerApp` uses it instead of bare `MaterialTheme`.
- **FR2**: Define semantic roles: background, surface, surfaceVariant, primary accent, onSurface, error, outline — mapped for dark (and light if supported).
- **FR3**: Update `themes.xml` / window background to a dark (or dayNight) base aligned with Compose.
- **FR4**: Centralize repeated list/row colors through theme; remove one-off hardcoded Material defaults where they fight the geek look.
- **FR5**: Now Playing, mini player (17), and compact headers (18) consume the same tokens.

### Non-functional requirements

- **NFR1**: Theme switch (if system-follow) must not recreate the activity in a way that drops playback (prefer Compose scheme change only).
- **NFR2**: No heavy runtime theme engines; static schemes only.
- **NFR3**: Keep dependency footprint unchanged (no new design-system libraries).

### UX design

- **Direction**: “Local library deck” — dark graphite/ink surfaces, one sharp accent (e.g. cool green or amber — **avoid** generic purple-indigo AI look), thin dividers, minimal elevation, iconography already Material but tinted neutrally.
- **Geek patterns**: Monospace for durations, queue index (`3 / 12`), optional small uppercase section labels; restrained grid rhythm; no glow soup or neon gradients.
- **Now Playing**: Large calm focus area; controls as clear icon buttons, not floating glass cards.
- **Letter placeholders** (until story 20): Themed geometric tiles (muted surface + accent letter), not loud `secondaryContainer` chips.
- **Motion**: Prefer opacity/position on screen enter; no decorative particle effects.

### Testing & validation

- **Manual**: Walk all tabs + Now Playing + Search + one detail screen in dark mode; check status bar icon contrast.
- **Manual (if light supported)**: Toggle system light/dark; playback continues; no white flash on resume.
- **Compose**: Smoke test that `MusicTheme` provides expected `colorScheme.background` in a small screenshot or assert.
- **Regression**: Favorites heart, selected nav item, and error banners remain visible on dark surfaces.

### Out of scope

- Dynamic Material You / wallpaper-derived colors as a requirement (optional later).
- Per-playlist themes.
- Rebranding app name or store listing assets beyond in-app theme.

---

### Overall app state after this story

The product has a deliberate dark geek-minimal visual system instead of stock Material light defaults.

### Value added after this story

Delivers the “consistent geek design patterns and dark themes” ask from `features_to_refine.md` as a reusable token layer for later artwork and branding work.

---

## Implementation plan

### Current state (baseline)

| Piece | Today |
|-------|--------|
| Compose entry | `MusicPlayerApp` wraps in bare `MaterialTheme { }` → default **light** M3 purple |
| XML | `Theme.Material.Light.NoActionBar` — white window before Compose paints (AC6 risk) |
| Tokens | Only `Dimens.kt`; no `Color` / `Type` / `Theme` |
| Hotspots fighting “geek dark” | `primaryContainer` TopAppBars on overlays; `secondaryContainer` letter circles (`LibraryScreen.InitialsAvatar`, For You tiles); Now Playing artwork `primaryContainer` |
| Already token-friendly | Most text/icons use `MaterialTheme.colorScheme.*` / `typography.*` — they pick up a real scheme once `MusicTheme` is wired |
| Mini player / compact chrome (17–18) | Use `surfaceContainer` / dividers / defaults — survive a scheme swap if roles are mapped |

---

### Step 1 — Design tokens: `Color.kt` (FR2, AC1, AC3, AC5)

Add `app/src/main/java/com/anplak/androidmusic/ui/theme/Color.kt` with a **graphite + phosphor-green** palette (not purple/indigo).

```kotlin
// Dark (first-class)
val Ink = Color(0xFF0E1114)           // scaffold / window
val Graphite = Color(0xFF161A1F)      // surface
val GraphiteElevated = Color(0xFF1E242B) // surfaceVariant / bars
val Mist = Color(0xFFE6E9ED)          // onSurface
val MistDim = Color(0xFF9AA3AD)       // onSurfaceVariant
val Phosphor = Color(0xFF3DDC97)      // primary accent — sparse use
val PhosphorDim = Color(0xFF1F3D32)   // primaryContainer (muted)
val OutlineMute = Color(0xFF2C333C)
val Danger = Color(0xFFFF6B6B)

// Optional light scheme (system-follow) — same accent, light neutrals
val Paper = Color(0xFFF4F6F8)
val PaperInk = Color(0xFF12161A)
// ...
```

Map into `darkColorScheme` / `lightColorScheme`:

| Role | Dark mapping | Use |
|------|--------------|-----|
| `background` | Ink | Scaffold / `Surface` |
| `surface` / `surfaceContainer*` | Graphite family | Lists, mini player, nav |
| `primary` | Phosphor | Selected nav, play CTA, favorite filled |
| `primaryContainer` | PhosphorDim | TopAppBars / placeholders — **muted**, not neon wash |
| `onPrimary` | near-black | Contrast on accent buttons |
| `secondaryContainer` | elevated graphite | Letter tiles (not loud chips) |
| `error` / `outline` | Danger / OutlineMute | Banners, dividers |

**Rationale:** AC1/AC3 — one sharp accent; surfaces stay ink/graphite. Reusing M3 role names means existing `MaterialTheme.colorScheme.*` call sites inherit the look with minimal rewrites (FR4).

---

### Step 2 — Typography: `Type.kt` (FR2, AC4)

Add `Type.kt`. Keep Material 3 scale sizes; swap families:

```kotlin
val GeekSans = FontFamily.SansSerif
val GeekMono = FontFamily.Monospace

val MusicTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = GeekSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.25).sp
    ),
    titleMedium = TextStyle(fontFamily = GeekSans, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = GeekSans, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = GeekMono, fontSize = 12.sp), // durations / meta
    labelMedium = TextStyle(
        fontFamily = GeekMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.08.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GeekMono,
        fontSize = 11.sp,
        letterSpacing = 0.06.sp
    )
    // …fill remaining slots from M3 defaults so nothing falls back oddly
)
```

Apply monospace styles at meta call sites that still use plain `bodySmall` without intending mono (optional polish in Step 5):

- Now Playing seek times (`current_time` / `duration_time`)
- Queue label (`queue_position` → `labelMedium` already good once typography is set)
- `TrackListItem` duration trailing text
- History duration / “position / total” strings

**Rationale:** Platform `SansSerif` + `Monospace` — distinctive pair, **zero** font assets / deps (NFR3). Meta in mono reads “deck / console” without glow UI.

---

### Step 3 — `MusicTheme` + wire root (FR1, NFR1, system bars)

Add `Theme.kt`:

```kotlin
@Composable
fun MusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkMusicColorScheme else LightMusicColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val bars = WindowCompat.getInsetsController(window, view)
            bars.isAppearanceLightStatusBars = !darkTheme
            bars.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MusicTypography,
        content = content
    )
}
```

Wire at the **highest** Compose entry so permission UI is themed too:

```kotlin
// MainActivity.setContent — or MusicPlayerAppWithPermissionCheck
MusicTheme {
    MusicPlayerAppWithPermissionCheck() // or MusicPlayerApp()
}

// MusicPlayerApp: replace bare MaterialTheme { … }
MusicTheme {  // or omit if already wrapped above — only one wrapper
    Surface(color = MaterialTheme.colorScheme.background) { … }
}
```

Prefer **one** wrapper in `MainActivity` / `MusicPlayerAppWithPermissionCheck`, and delete the inner bare `MaterialTheme` in `MusicPlayerApp` (keep `Surface` + `background`).

**System-follow vs dark-only:** Default `isSystemInDarkTheme()` is cheap and satisfies “optionally follow system.” Dark scheme must be polished first; light is a second-class twin of the same accent (AC1). No `AppCompatDelegate` / activity recreate — Compose recomposes only (NFR1).

**Rationale:** FR1 in one place; transparent system bars match edge-to-edge already in `MainActivity`; icon contrast flips with scheme (manual AC).

---

### Step 4 — XML window theme (FR3, AC6)

```xml
<!-- values/themes.xml -->
<style name="Theme.AndroidMusic" parent="android:Theme.Material.NoActionBar">
    <item name="android:windowBackground">@color/window_background</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```

```xml
<!-- values/colors.xml -->
<color name="window_background">#FF0E1114</color> <!-- same as Ink -->

<!-- values-night/colors.xml (if using DayNight parent) -->
<color name="window_background">#FF0E1114</color>

<!-- values/colors.xml light variant only if DayNight -->
```

Simplest AC6 path: **always dark window background** aligned with Ink (app is dark-first). If using `Theme.Material.DayNight.NoActionBar`, provide light `window_background` in `values/` and dark in `values-night/`.

Do **not** change `ic_launcher_background` here (story 20).

**Rationale:** Cold-start flash is XML, not Compose — matching Ink removes the white pop before first frame.

---

### Step 5 — Component pass (FR4, FR5, AC2–AC4)

No new design-system widgets. Retarget hotspots to semantic roles:

| Hotspot | Change |
|---------|--------|
| Overlay TopAppBars (`primaryContainer`) | Keep role, but `primaryContainer` is now PhosphorDim — bars read as muted chrome, not purple slabs. Optional: switch to `surface` + `onSurface` for flatter “geek” bars |
| `InitialsAvatar` / For You letter tiles | `secondaryContainer` / `onSecondaryContainer` → muted graphite + Mist letter (scheme map); optional square `RoundedCornerShape(4.dp)` instead of loud circle |
| Now Playing artwork placeholder | `surfaceVariant` + `primary` tinted `MusicNote` (or muted `primaryContainer`) — calm focus, not container wash |
| Mini player | Keep `surfaceContainer`; ensure scheme maps `surfaceContainer` → GraphiteElevated |
| Nav bar | Default `NavigationBar` + `primary` selected indicator from scheme |
| Section labels (Playlists / For You) | Prefer `labelMedium` / `labelSmall` (mono + tracking) + `onSurfaceVariant` |
| Durations | `bodySmall` / mono as in Step 2 |

Shared helper (optional, if ≥2 call sites):

```kotlin
@Composable
fun ArtworkPlaceholder(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) { /* surfaceVariant + content */ }
```

Leave layout/structure from 17–18 untouched — only colors/type.

**Rationale:** FR4/FR5 — one system across tabs, mini player, compact chrome, Now Playing; accent stays on CTAs / selection / hearts (AC3).

---

### Step 6 — Contrast & permission shell (AC5)

- Verify Phosphor-on-Ink and Mist-on-Ink for body (≥ ~4.5:1 where practical).
- Favorite heart `primary` on dark surface must remain visible (regression).
- Error text/banners use `error`, not primary.
- Theme `PermissionRequestScreen` via root `MusicTheme` (Step 3) so grant flow is not a light island.

---

### Key decisions

| Decision | Rationale |
|----------|-----------|
| Phosphor green accent on graphite (not amber, not purple) | UX “geek” + avoid AI purple-indigo; green reads “terminal / phosphor” |
| Static `darkColorScheme` / `lightColorScheme` only | NFR2 — no Material You / runtime engine |
| Follow system via `isSystemInDarkTheme()` | Cheap dual support; dark is polished first (AC1) |
| Compose-only scheme switch | NFR1 — playback survives; no activity recreate |
| Reuse M3 role names | Existing screens already call `colorScheme.*` — max impact, min churn (AC2) |
| `FontFamily.SansSerif` + `Monospace` | AC4 distinctive pair; NFR3 no font packs |
| Dark XML `windowBackground` = Ink | AC6 cold-start flash |
| Soften (don’t invent new) TopAppBar / avatar roles | FR4 — component pass without layout rewrite |
| Leave launcher / real artwork to story 20 | Explicit out of scope |
| Single `MusicTheme` at activity entry | Permission + app share tokens; avoid nested conflicting themes |

---

### Implementation checklist

- [x] `Color.kt` — Ink / Graphite / Phosphor (+ light twin if system-follow)
- [x] `Type.kt` — `MusicTypography` (sans UI + mono meta)
- [x] `Theme.kt` — `MusicTheme` + system bar `SideEffect`
- [x] Wire `MusicTheme` at Compose root; remove bare `MaterialTheme` in `MusicPlayerApp`
- [x] `themes.xml` + `window_background` dark (DayNight optional)
- [x] Hotspot pass: TopAppBars, letter tiles, Now Playing placeholder, durations/queue meta
- [x] Spot-check mini player + compact chrome + nav selection on dark surfaces
- [ ] Manual: cold start (no white flash), all tabs + Now Playing + Search + one detail

---

### Test scenarios

**Compose**
- `MusicTheme(darkTheme = true)` → `MaterialTheme.colorScheme.background` equals Ink (smoke assert or screenshot).
- Root / app under `MusicTheme`: main surface is not default light purple scaffold.

**Manual**
- Cold start: window is dark before first Compose frame (AC6).
- Walk five tabs + Now Playing + Search + one detail: cohesive dark surfaces, accent only on selection / play / hearts (AC1–AC3).
- Titles vs artists vs durations: clear hierarchy; durations/queue meta read mono or subdued (AC4).
- Status / nav bar icons readable on dark (and light if system-follow).
- If light supported: toggle system theme mid-playback — audio continues, UI flips without restart (NFR1).

**Regression**
- Favorite heart, selected bottom-nav item, error banners visible on dark.
- Mini player + compact tab actions still layout correctly (stories 17–18).
- Existing test tags unchanged; wrap instrumented UI tests in `MusicTheme` where they currently use bare `MaterialTheme`.
