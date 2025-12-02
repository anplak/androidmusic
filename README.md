# Android Music Player

A minimal offline music player for Android that automatically discovers and plays local audio files without any network dependency.

## Features

- **Offline-first**: No network or account required
- **Automatic library scanning**: Discovers all audio files on device via MediaStore
- **Music library browsing**: Scrollable list of tracks with title, artist, and duration
- **Audio playback**: Play, pause, and seek through audio tracks
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Error handling**: Clear feedback when playback issues occur
- **Scoped storage support**: Compatible with Android 10+ privacy requirements

## Requirements

- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Supported formats**: MP3, M4A, FLAC, and other formats supported by ExoPlayer

## Project Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Run the app on an emulator or physical device

## Architecture

The app follows **MVVM architecture** with unidirectional data flow:

### Core Components

**AudioPlayer** (`player/AudioPlayer.kt`)
- Wraps ExoPlayer for audio playback management
- Emits playback state via Kotlin StateFlow
- Handles lifecycle-aware player management
- Provides play, pause, seek, and error handling

**MusicLibraryRepository** (`data/MusicLibraryRepository.kt`)
- Queries MediaStore for device audio files
- Extracts track metadata (title, artist, album, duration)
- Executes queries off main thread using coroutines

**LibraryViewModel** (`ui/LibraryViewModel.kt`)
- Manages library screen state (Loading, Content, Empty)
- Session caching to avoid redundant scans
- Loads library on first access

**PlaybackViewModel** (`ui/PlaybackViewModel.kt`)
- Manages playback UI state and business logic
- Coordinates between AudioPlayer and UI layer
- Lifecycle-aware: automatically releases player resources

**UI Layer** (Compose)
- `MusicPlayerApp`: Root composable that orchestrates app flow
- `LibraryScreen`: Scrollable list of all tracks
- `NowPlayingScreen`: Playback UI with controls and back navigation
- `PermissionRationaleScreen`: Permission request UI

**Permission Handling** (`ui/PermissionHandler.kt`)
- Runtime permission management for media access
- API level-aware: uses `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE` on older versions

### Data Flow

1. User grants media permission
2. App scans device audio via MediaStore (off main thread)
3. Library screen displays list of discovered tracks
4. User taps a track from the library
5. App navigates to Now Playing screen and starts playback
6. Playback state flows to UI via StateFlow
7. User can navigate back to library while playback continues

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with unidirectional data flow
- **Audio**: ExoPlayer 2.19.1
- **Concurrency**: Kotlin Coroutines and Flow
- **Build**: Gradle with Kotlin DSL and version catalog
- **Testing**: JUnit, Mockito, Coroutines Test

## Current Limitations

The following features are intentionally out of scope for the current iteration:

- Background playback and notification controls
- Playlists and favorites
- Albums/artists views and filtering
- Sorting options
- Listening history
- Recommendations and discovery
- Search functionality

These features are planned for future iterations.

## Manual Testing

To validate the app:

1. **Permission scenarios**
   - First launch: permission should be requested
   - Deny permission: rationale screen should appear
   - Grant permission: library should load

2. **Library scanning**
   - Verify loading indicator appears during scan
   - Check that all device audio files appear in the list
   - Verify title, artist, and duration display correctly

3. **Empty state**
   - On a device with no music files, verify empty state message

4. **Track selection**
   - Tap a track to start playback
   - Verify Now Playing screen shows correct track info

5. **Playback controls**
   - Play/pause functionality
   - Seek bar interaction
   - Time display accuracy

6. **Navigation**
   - Back button returns to library
   - Playback pauses when returning to library

7. **Error handling**
   - If a track fails to play, verify error dialog appears

8. **Configuration changes**
   - Rotate device during playback
   - Verify state persists (ViewModel survives)

## Project Structure

```
app/src/main/java/com/anplak/androidmusic/
├── MainActivity.kt                    # App entry point
├── data/
│   └── MusicLibraryRepository.kt     # MediaStore access
├── player/
│   ├── AudioPlayer.kt                # ExoPlayer wrapper
│   ├── PlaybackState.kt              # Playback state model
│   ├── PlayerError.kt                # Error types
│   └── TrackInfo.kt                  # Track metadata model
└── ui/
    ├── MusicPlayerApp.kt             # Root composable
    ├── LibraryScreen.kt              # Library list UI
    ├── LibraryViewModel.kt           # Library state management
    ├── PlaybackViewModel.kt          # Playback state management
    ├── PermissionHandler.kt          # Permission management
    ├── NowPlayingScreen.kt           # Now playing UI
    └── PermissionRationaleScreen.kt  # Permission rationale UI
```

## Future Roadmap

Planned features for upcoming stories:

- **Story 3**: Background playback and notification controls
- **Story 4**: Persistent favorites and playlists
- **Story 5**: Auto-generated playlists and smart shuffle
- **Story 6**: Listening history and insights
- **Story 7**: Recommendations and discovery
- **Story 8**: Advanced playlist curation tools
- **Story 9**: Search and filter experience
- **Story 10**: Polish, performance, and release candidate

## License

This is a personal pet project for learning and experimentation.
