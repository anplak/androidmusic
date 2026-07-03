# Android Music Player

A minimal offline music player for Android. It discovers local audio via MediaStore, keeps your library and listening data on device, and plays in the background with standard Android media controls.

## Features

### Library & playback
- **Offline-first** — no network or account required
- **Your Library** — browse tracks with title, artist, album, and duration
- **Cached library** — last-known tracks show immediately; MediaStore sync runs in the background when you open Library or change index rules
- **Library index rules** — skip tracks over a duration limit; include or exclude folders; blocklist artists; review and revert exclusions from Library Index
- **Playback queue** — tap a track to play from that point in the list; next/previous with queue position
- **Background playback** — continues with screen off or app in background
- **System media controls** — notification, lock screen, and headset buttons via MediaSession

### Organization
- **Favorites** — heart tracks; dedicated tab
- **Playlists** — create, rename, reorder, bulk remove, duplicate/merge, auto-mix from seeds
- **Smart playlists** — Most Played, Recently Played, Recently Added
- **Smart shuffle** — weighted shuffle favoring favorites and frequently played tracks

### Discovery & search
- **For You** — on-device rows such as Daily Mix, Quick Mix, “Because you listen to…”, and Continue Listening
- **Global search** — tracks, playlists, and history from Library or Playlists
- **Library filters** — favorites, recently added, duration chips; local search within Library

### Listening data
- **History** — timeline of plays with timestamps; tap an entry to play again
- **Insights** — today’s and this week’s listening time; top tracks and artists
- **Play statistics** — counts and completions feed smart playlists and shuffle
- **Retention** — play history older than 90 days is removed automatically

### UI
- **Jetpack Compose** with Material Design 3
- **Bottom navigation** — For You, Your Library, Favorites, Playlists, History
- **Permissions** — runtime access to audio files (API 33+ uses `READ_MEDIA_AUDIO`)

## Requirements

| | |
|---|---|
| Minimum SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |
| JDK (build) | 17–21 recommended |
| Formats | MP3, M4A, FLAC, and others supported by Media3 |

## Getting started

1. Clone the repository
2. Open in Android Studio (or use Gradle from the command line)
3. Sync Gradle
4. Run on an emulator or device with local audio files

Grant media permission when prompted. On first visit to **Your Library** with an empty cache, a full scan runs once; later visits show cached tracks while sync updates in the background.

## Testing

**Unit tests** (ViewModels, repositories, Room):

```bash
./gradlew test
```

Use JDK 17 or 21 if your default Java is newer (e.g. Java 26 breaks the Kotlin/Gradle toolchain).

**On-device E2E** (UI flows under `com.anplak.androidmusic.ui`):

```bash
./scripts/run-e2e-wifi.sh
```

Requires a connected device (`adb devices`). Optional: pass a serial or set `ANDROID_SERIAL`.

**Integration tests** (MediaStore on a real device):

```bash
./gradlew :app:connectedDebugAndroidTest --tests "*MusicLibraryIntegrationTest*"
```

## Known limitations

- No repeat-one / repeat-all modes (smart shuffle only)
- Queue is not restored after force-stop
- No dedicated Artists or Albums browse views yet
- No exclude-from-library entry point on artist detail screen yet (Library Index only)
- No cloud sync, lyrics, or equalizer
- Insights are simple aggregates, not exportable reports

## Planned direction

Rough backlog (see `.cursor/workflow/` for story specs):

- Artists / albums tabs in Library
- Artist detail screen with “Exclude artist from library” action
- Smarter favorite/shuffle ranking (explicit plays, skips, recency)
- Richer auto-generated mixes (decade, add date, playlists, metadata)

## License

Personal pet project for learning and experimentation.
