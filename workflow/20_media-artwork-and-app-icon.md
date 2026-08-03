### User story

As a user, I want **real album/artist artwork** (with a stylish fallback) and an **app icon that reads as a music player** so lists and the launcher feel like a music app, not a generic utility with letter tiles.

### Description

For You cards and Library artist/album rows use **first-letter circles**; Now Playing shows a music-note placeholder; track rows have no leading art. MediaStore album art is never loaded, and the launcher is a **purple plus** (`ic_launcher_foreground`). This story adds a shared artwork pipeline + composable, wires it into key surfaces (including mini player leading slot from story **17**), and ships a music-meaningful adaptive icon. Styling should follow story **19** tokens when available; otherwise theme-neutral fallbacks.

**Prerequisite:** stories 02 / 11 (library catalog). Story **19** recommended for placeholder styling. Story **17** recommended so mini player can show art.

### Goals & scope (2–3 days)

- **In scope**
  - **Artwork URI**: Resolve album art from MediaStore (e.g. `ALBUM_ID` → `content://media/external/audio/albumart/{id}` or equivalent) during sync or at display time; persist what is needed for fast list bind.
  - **Shared `MediaArtwork` composable**: Loads art asynchronously; on miss, shows a **minimal geometric / monogram** fallback (not a loud random color chip).
  - **Wire into**: Now Playing, For You track cards, Library Artists/Albums rows, mini player leading image (if 17 present); optionally track list leading thumb where density allows.
  - **Adaptive launcher icon**: Replace plus-mark with a simple mark that implies audio/playback (waveform, note, or disc silhouette) aligned with the geek-dark brand.
- **Out of scope**
  - Downloading covers from the network / MusicBrainz / internet scrapers.
  - User-picked custom artwork.
  - Full grid “album browser” redesign (keep existing list/row layouts).
  - Artist photos distinct from album art (v1 may reuse a representative album cover per artist).
  - Notification large-icon polish beyond passing `artworkUri` into Media3 metadata if already straightforward — nice-to-have, not required.

### Acceptance criteria

- **AC1**: For tracks/albums with MediaStore art, Now Playing and For You cards show that image instead of letter-only placeholders.
- **AC2**: When art is missing, UI shows the shared styled fallback (monogram or abstract tile) — still minimal and on-brand, not empty broken images.
- **AC3**: Library Artists and Albums tabs use `MediaArtwork` (representative art or fallback) instead of bare `InitialsAvatar` only.
- **AC4**: Scrolling a long Library / For You list remains smooth; art loads without blocking bind (placeholders first).
- **AC5**: Launcher / adaptive icon on the home screen communicates **music** at a glance (not a generic “+” / Material sample mark).
- **AC6**: No crash or permanent empty hole when a content URI fails to decode.

### Functional requirements

- **FR1**: Extend track/album model as needed (`albumId`, `artworkUri`, or similar) from MediaStore during existing sync path — additive only.
- **FR2**: Implement `MediaArtwork(uri, contentDescription, modifier, fallbackLabel)` used by list and player surfaces.
- **FR3**: Choose an image loader approach already acceptable for the project (e.g. Coil **or** `rememberAsyncImagePainter` / platform APIs) — one approach app-wide.
- **FR4**: Artist row art = cover of a representative album/track for that artist when available.
- **FR5**: Update `ic_launcher_foreground` / adaptive XML and background color to match story-19 brand accent (or neutral dark if 19 not yet merged).
- **FR6**: If story 17 mini player exists, leading slot uses `MediaArtwork` at small size (~40 dp).

### Non-functional requirements

- **NFR1**: Image decode/cache off main thread; list scroll target stays jank-free on mid-range devices for 1,000+ rows with art.
- **NFR2**: Disk/memory cache bounded; no unbounded bitmap retention.
- **NFR3**: Sync time regression for 5,000 tracks stays within existing story-11 budget (artwork fields cheap; no full bitmap decode during sync).
- **NFR4**: Fallbacks are pure Compose (no extra asset per letter required).

### UX design

- **Lists**: Squircle / rounded-rect thumbs (not heavy cards); consistent corner radius from theme Dimens.
- **Fallback**: Dark tile + single initial or simple hash-stable pattern; accent used only as a thin edge or letter — matches geek-minimal direction from story 19.
- **Now Playing**: Large artwork plane (edge-conscious, not a tiny inset card); fallback scales gracefully.
- **Icon**: Simple, legible at 48 dp; works as adaptive foreground with safe zone; reads “offline music” not “add item”.
- **Do not**: Collage grids, floating stickers on art, or multi-cover stacks in v1.

### Testing & validation

- **Unit**: URI builder / albumId mapping; null-safe fallback key.
- **Compose**: `MediaArtwork` with null URI shows fallback; with fake URI failure shows fallback.
- **Manual**: Device library with mixed art-rich and art-less albums — For You, Library, Now Playing, mini player.
- **Manual**: Install build — launcher icon distinct from previous purple-plus; check round/adaptive shapes.
- **Regression**: Play/Like/row actions still hit correctly with leading art present (hit targets not covered).

### Out of scope

- Embedded ID3 APIC extraction beyond what MediaStore already exposes (unless MediaStore URI is empty and a tiny local parse is trivially available — not required).
- Themed dynamic icon packs.
- Store feature-graphic / Play listing screenshots (release chore).

---

### Overall app state after this story

Key music surfaces show album art with on-brand fallbacks, and the launcher mark matches the product’s function — completing the Look and Feel refine set (stories **17–20**).

### Value added after this story

Replaces meaningless letter tiles and the generic launcher with visuals that communicate “this is my music library,” as requested in `features_to_refine.md`.

---

## Implementation plan

### Current implementation notes

- Library sync currently persists `TrackInfo` / `TrackEntity` without `MediaStore.Audio.Media.ALBUM_ID`; Room is at schema version 7.
- Coil or another image loader is not present. Add one shared loader instead of decoding bitmaps in composables.
- Story 17 and story 19 are present: the mini player already has a 40 dp leading slot, and `MusicTheme` provides Ink / Graphite / Phosphor styling.
- Now Playing, For You, and Library currently render independent note/initial placeholders.
- Media3 metadata currently includes title, artist, and album only; `setArtworkUri` is available in the project's Media3 version.
- The adaptive launcher icon still uses `#6200EE` behind a white plus.

### Step 1 — Persist the cheap MediaStore artwork key (FR1, NFR3)

Modify:

- `player/TrackInfo.kt`
- `data/db/Entities.kt`
- `data/MusicLibraryRepository.kt`
- `data/FavoritesRepository.kt` (entity/domain mappers)
- `data/db/AppDatabase.kt`

Add `MediaStore.Audio.Media.ALBUM_ID` to the existing sync projection and persist only `albumId: Long?`; do not open or decode artwork during sync. Give the new domain field a default so existing test fixtures and call sites remain source-compatible.

```kotlin
data class TrackInfo(
    // existing fields
    val dateAddedSec: Long? = null,
    val albumId: Long? = null,
) {
    val artworkUri: Uri?
        get() = AlbumArtworkUri.forAlbumId(albumId)
}
```

Add nullable `albumId` to `TrackEntity`, bump Room from 7 to 8, and register:

```kotlin
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN albumId INTEGER")
    }
}
```

Existing rows safely receive `null` and show fallbacks until the next MediaStore sync.

**Rationale:** a `Long?` is cheap to query and store, keeps the database independent of a derived URI string, and avoids bitmap I/O in the 5,000-track sync path.

---

### Step 2 — Centralize album-art URI construction

Add `data/AlbumArtworkUri.kt`:

```kotlin
object AlbumArtworkUri {
    private val baseUri = Uri.parse(
        "content://media/external/audio/albumart"
    )

    fun forAlbumId(albumId: Long?): Uri? =
        albumId
            ?.takeIf { it > 0L }
            ?.let { ContentUris.withAppendedId(baseUri, it) }
}
```

Read the cursor value null-safely and treat zero/negative IDs as unavailable:

```kotlin
val albumId = cursor
    .getLong(albumIdColumn)
    .takeIf { it > 0L }
```

**Rationale:** one builder gives sync, UI, aggregation, playback metadata, and tests identical null/error behavior. URI construction remains pure and independently testable.

---

### Step 3 — Add one asynchronous image pipeline (FR3, NFR1–NFR2)

Add Coil Compose through `gradle/libs.versions.toml` and `app/build.gradle.kts`, using a version compatible with the current Kotlin/Compose toolchain. Use Coil app-wide for content-URI decoding, request sizing, memory cache, and disk cache.

Do not add a custom `ContentResolver` + coroutine bitmap loader. Coil already performs decode off the main thread and prevents every row from retaining its own full-size bitmap.

For each surface, constrain the request through a fixed-size modifier before loading:

```kotlin
Modifier
    .size(Dimens.listArtworkSize)
    .clip(RoundedCornerShape(Dimens.artworkCornerRadius))
```

**Rationale:** a single maintained loader satisfies asynchronous bind and bounded caching requirements with less lifecycle and cancellation risk than a custom implementation.

---

### Step 4 — Build shared `MediaArtwork` and fallback (FR2, AC2, AC6)

Add `ui/MediaArtwork.kt` with a stable, reusable API:

```kotlin
@Composable
fun MediaArtwork(
    uri: Uri?,
    contentDescription: String?,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.artworkCornerRadius),
) {
    Box(modifier = modifier.clip(shape).testTag("media_artwork")) {
        ArtworkFallback(
            label = fallbackLabel,
            modifier = Modifier.matchParentSize(),
        )
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
```

The fallback stays underneath the image, so loading and decode failures reveal the styled tile instead of an empty hole. `ArtworkFallback` should:

- derive one uppercase initial, using `?` for a blank label;
- use `MaterialTheme.colorScheme.surfaceContainer` / Graphite;
- use Phosphor only for the glyph or a subtle 1 dp edge;
- avoid random bright colors, circles, and per-letter assets.

Add to `theme/Dimens.kt`:

```kotlin
val artworkCornerRadius = 8.dp
val listArtworkSize = 40.dp
val forYouArtworkSize = 72.dp
```

Keep `nowPlayingArtworkSize` and `miniPlayerArtworkSize` already defined by story 17.

**Rationale:** rendering fallback first makes null, loading, and failed decode states visually identical and safe. A rounded rectangle matches the story's artwork language and replaces the current unrelated circle styles.

---

### Step 5 — Provide representative Library artwork (FR4, AC3)

Extend `ArtistSummary` and `AlbumSummary` in `data/LibraryBrowseAggregator.kt` with `artworkUri: Uri?`.

- Album: select the first valid artwork URI in the deterministic group order.
- Artist: select the first valid URI across that artist's tracks; otherwise retain the artist-name fallback.
- Keep current normalization/group keys unchanged; artwork must not affect collection identity or sorting.

```kotlin
val artworkUri = groupedTracks
    .asSequence()
    .mapNotNull(TrackInfo::artworkUri)
    .firstOrNull()
```

If the existing aggregate input order is not guaranteed, sort by stable track ID before selection. Avoid a second database query per row.

**Rationale:** deriving representative art during the existing aggregate rebuild is O(n), deterministic, and avoids N+1 MediaStore/Room lookups during list composition.

---

### Step 6 — Wire the shared composable into required surfaces (AC1–AC4)

Apply in this order:

1. `NowPlayingScreen.kt`: replace the note placeholder with a large `MediaArtwork`; use track title as fallback label and artwork content description.
2. `ForYouScreen.kt`: replace the circular initial in `RecommendationTrackCard` with the track's artwork.
3. `LibraryScreen.kt`: replace `InitialsAvatar` in Artist and Album rows with summary artwork; remove the helper if no call sites remain.
4. `MiniPlayerBar.kt`: accept `artworkUri` / fallback label and render `MediaArtwork` in the existing 40 dp leading slot.
5. `MusicPlayerApp.kt`: pass selected-track artwork to Now Playing and `BoundMiniPlayerBar`.

Representative call:

```kotlin
MediaArtwork(
    uri = track.artworkUri,
    contentDescription = "${track.album} artwork",
    fallbackLabel = track.title,
    modifier = Modifier.size(Dimens.miniPlayerArtworkSize),
)
```

Keep artwork non-clickable so row, title, favorite, and playback hit targets continue to own input. Preserve existing semantic tags where E2E tests depend on them; add `media_artwork` as the shared tag.

Track-list leading thumbnails remain optional. Add them only if the row still preserves title width and 48 dp action targets after the required surfaces are complete.

**Rationale:** passing a URI/value down keeps `MediaArtwork` stateless and reusable; image fetching stays out of ViewModels and list bind remains asynchronous.

---

### Step 7 — Pass artwork to Media3 metadata (nice-to-have)

In `service/MusicPlaybackService.kt`, extend the existing metadata builder:

```kotlin
val metadata = MediaMetadata.Builder()
    .setTitle(track.title)
    .setArtist(track.artist)
    .setAlbumTitle(track.album)
    .setArtworkUri(track.artworkUri)
    .build()
```

Only include this after the persisted URI path is working. A missing or unreadable URI must not affect playback.

**Rationale:** this reuses the same source for system playback UI without adding notification-specific bitmap handling; detailed notification polish remains out of scope.

---

### Step 8 — Replace the adaptive launcher mark (FR5, AC5)

Modify:

- `res/drawable/ic_launcher_foreground.xml`
- `res/values/colors.xml`

Replace the plus paths with a simple disc plus play-triangle silhouette (or three waveform bars) inside the adaptive safe zone. Use Mist/Phosphor foreground on Ink (`#0E1114`) or Graphite background; keep detail thick enough to read at 48 dp.

The manifest and existing adaptive/round mipmap XML can remain unchanged because they already reference `ic_launcher_foreground` and `ic_launcher_background`.

**Rationale:** one bold music symbol survives launcher masks and small sizes better than a detailed note/collage, while Ink + restrained accent aligns with story 19.

---

### Files summary

**Add**

- `app/src/main/java/com/anplak/androidmusic/data/AlbumArtworkUri.kt`
- `app/src/main/java/com/anplak/androidmusic/ui/MediaArtwork.kt`
- focused URI-builder and Compose artwork tests

**Modify**

- `gradle/libs.versions.toml`, `app/build.gradle.kts`
- `TrackInfo.kt`, `Entities.kt`, `AppDatabase.kt`
- `MusicLibraryRepository.kt`, `FavoritesRepository.kt`, `LibraryBrowseAggregator.kt`
- `Dimens.kt`
- `NowPlayingScreen.kt`, `ForYouScreen.kt`, `LibraryScreen.kt`
- `MiniPlayerBar.kt`, `MusicPlayerApp.kt`
- optionally `MusicPlaybackService.kt`
- `ic_launcher_foreground.xml`, `colors.xml`

---

### Key decisions

| Decision | Rationale |
|----------|-----------|
| Persist `albumId`, derive `artworkUri` | Small additive schema field; no redundant URI strings or bitmap work during sync |
| Nullable Room column + migration 7→8 | Existing installations upgrade safely and fall back until re-sync |
| Coil Compose as the sole image loader | Async content-URI decode, cancellation, and bounded caches without custom lifecycle code |
| Fallback rendered beneath the image | Loading, null, and decode failure never produce an empty hole |
| Deterministic representative cover per aggregate | Artist/album rows need no per-row query and do not flicker between covers |
| Rounded-rect artwork on every surface | Consistent with real cover geometry and story 19's restrained visual system |
| Keep artwork non-clickable | Existing row and mini-player actions retain their semantics and hit targets |
| Launcher uses a disc/play or waveform mark | Clearly communicates music and remains legible under adaptive masks |
| Track-list thumbs and notification polish are secondary | Required surfaces and smooth scrolling take priority within the 2–3 day scope |

---

### Implementation checklist

- [x] Add `albumId` to MediaStore projection, domain/entity models, mappers, and Room migration 7→8
- [x] Add the central null-safe album-art URI builder
- [x] Add Coil Compose and shared `MediaArtwork`
- [x] Add artwork dimensions and story-19 fallback styling
- [x] Add deterministic representative art to artist/album summaries
- [x] Wire Now Playing, For You, Library Artists/Albums, and mini player
- [x] Optionally pass artwork URI to Media3 metadata
- [x] Replace purple-plus launcher resources with the music mark
- [ ] Validate mixed artwork/missing-art libraries and long-list scrolling

---

### Test scenarios

**Unit**

- Valid album ID builds `content://media/external/audio/albumart/{id}`; null, zero, and negative IDs return null.
- Sync cursor maps `ALBUM_ID` into `TrackInfo` / `TrackEntity`; existing rows survive migration 7→8.
- Album aggregate chooses its representative cover; artist aggregate skips missing covers and chooses a deterministic available cover.

**Compose / UI**

- `MediaArtwork` with null URI shows the shared fallback.
- An unreadable content URI leaves the fallback visible and does not crash.
- Now Playing, For You, Library Artists/Albums, and mini player expose artwork without covering existing click targets.

**Manual**

- Browse and play a device library containing both art-rich and art-less albums across all required surfaces.
- Fast-scroll a large Library / For You list: placeholders appear immediately and images load without visible bind stalls.
- Install the app and inspect adaptive and round launcher masks at normal launcher size; the icon reads as music, not a plus.

**Regression**

- Play, pause, favorite, row navigation, mini-player expansion, and tab actions remain reachable.
- Playback and cached-library startup work before and after the Room migration.
