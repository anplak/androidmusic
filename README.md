# Android Music Player

A minimal offline music player for Android that automatically discovers and plays local audio files without any network dependency.

## Features

- **Offline-first**: No network or account required
- **Automatic library scanning**: Discovers all audio files on device via MediaStore
- **Music library browsing**: Scrollable list of tracks with title, artist, and duration
- **Playback queue**: Selecting a track builds a queue from the library for continuous playback
- **Queue navigation**: Next/previous controls with queue position indicator
- **Background playback**: Music continues playing when app is in background or screen is off
- **Media notification**: Control playback from the notification shade with play/pause, next, previous
- **Lockscreen and headset controls**: MediaSession integration for system-wide media controls
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Error handling**: Clear feedback when playback issues occur
- **Scoped storage support**: Compatible with Android 10+ privacy requirements

## Requirements

- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Supported formats**: MP3, M4A, FLAC, and other formats supported by Media3

## Project Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Run the app on an emulator or physical device

## Architecture

The app follows **MVVM architecture** with unidirectional data flow:

### Core Components

**MusicPlaybackService** (`service/MusicPlaybackService.kt`)
- Extends Media3 `MediaSessionService` for background audio playback
- Manages ExoPlayer instance and MediaSession
- Provides automatic media notification with playback controls
- Handles foreground service lifecycle

**AudioPlayer** (`player/AudioPlayer.kt`)
- Bridge between UI and MusicPlaybackService via MediaController
- Emits playback and queue state via Kotlin StateFlow
- Provides play, pause, seek, next, previous, and error handling

**PlaybackQueue** (`player/PlaybackQueue.kt`)
- Immutable data class representing the playback queue
- Supports next, previous, and jump-to operations
- Factory method to build queue from library at selected position

**MusicLibraryRepository** (`data/MusicLibraryRepository.kt`)
- Queries MediaStore for device audio files
- Extracts track metadata (title, artist, album, duration)
- Executes queries off main thread using coroutines

**LibraryViewModel** (`ui/LibraryViewModel.kt`)
- Manages library screen state (Loading, Content, Empty)
- Session caching to avoid redundant scans
- Loads library on first access

**PlaybackViewModel** (`ui/PlaybackViewModel.kt`)
- Manages playback UI state and queue operations
- Coordinates between AudioPlayer and UI layer
- Exposes queue position and navigation availability

**UI Layer** (Compose)
- `MusicPlayerApp`: Root composable that orchestrates app flow
- `LibraryScreen`: Scrollable list of all tracks
- `NowPlayingScreen`: Playback UI with play/pause, next/previous, seek, and queue indicator
- `PermissionRationaleScreen`: Permission request UI

**Permission Handling** (`ui/PermissionHandler.kt`)
- Runtime permission management for media access
- API level-aware: uses `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE` on older versions

### Data Flow

1. User grants media permission
2. App scans device audio via MediaStore (off main thread)
3. Library screen displays list of discovered tracks
4. User taps a track from the library
5. App builds a playback queue from that track onward
6. MusicPlaybackService starts playback in foreground
7. Playback state flows to UI via MediaController and StateFlow
8. User can navigate back to library while playback continues in background
9. User can control playback via notification or lockscreen controls

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with unidirectional data flow
- **Audio**: Jetpack Media3 1.2.1 (ExoPlayer + MediaSession)
- **Concurrency**: Kotlin Coroutines and Flow
- **Build**: Gradle with Kotlin DSL and version catalog
- **Testing**: JUnit, Mockito, Robolectric, Coroutines Test

## Current Limitations

The following features are intentionally out of scope for the current iteration:

- Repeat and shuffle modes
- Persistent queue across app restarts
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

4. **Track selection and queue**
   - Tap a track to start playback
   - Verify Now Playing screen shows correct track info
   - Verify queue position indicator shows (e.g., "3 / 10")

5. **Queue navigation**
   - Next button advances to next track
   - Previous button goes to previous track
   - Buttons are disabled at queue boundaries

6. **Playback controls**
   - Play/pause functionality
   - Seek bar interaction
   - Time display accuracy

7. **Background playback**
   - Start playback and press home button
   - Verify music continues playing
   - Lock screen and verify music continues

8. **Notification controls**
   - Verify media notification appears during playback
   - Test play/pause from notification
   - Test next/previous from notification
   - Tap notification to return to Now Playing

9. **Lockscreen and headset controls**
   - Verify lockscreen shows media controls
   - Test play/pause from lockscreen
   - Test headset button controls (if available)

10. **Navigation**
    - Back button returns to library
    - Playback continues when returning to library

11. **Error handling**
    - If a track fails to play, verify error dialog appears

12. **Configuration changes**
    - Rotate device during playback
    - Verify state persists (ViewModel survives)

13. **App lifecycle**
    - Swipe app away from recents while playing
    - Verify playback stops and notification clears

## Project Structure

```
app/src/main/java/com/anplak/androidmusic/
├── MainActivity.kt                    # App entry point
├── data/
│   └── MusicLibraryRepository.kt     # MediaStore access
├── player/
│   ├── AudioPlayer.kt                # MediaController bridge
│   ├── PlaybackQueue.kt              # Queue model
│   ├── PlaybackState.kt              # Playback state model
│   ├── PlayerError.kt                # Error types
│   └── TrackInfo.kt                  # Track metadata model
├── service/
│   └── MusicPlaybackService.kt       # Background playback service
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

- **Story 4**: Persistent favorites and playlists
- **Story 5**: Auto-generated playlists and smart shuffle
- **Story 6**: Listening history and insights
- **Story 7**: Recommendations and discovery
- **Story 8**: Advanced playlist curation tools
- **Story 9**: Search and filter experience
- **Story 10**: Polish, performance, and release candidate

## License

This is a personal pet project for learning and experimentation.
