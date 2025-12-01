# Android Music Player

A minimal offline music player for Android that allows users to select and play local audio files without any network dependency.

## Features

- **Offline-first**: No network or account required
- **File picker integration**: Select audio files from device storage using the system file picker
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

**PlaybackViewModel** (`ui/PlaybackViewModel.kt`)
- Manages UI state and business logic
- Coordinates between AudioPlayer and UI layer
- Extracts track metadata from URIs
- Lifecycle-aware: automatically releases player resources

**UI Layer** (Compose)
- `MusicPlayerApp`: Root composable that orchestrates app flow
- `EmptyStateScreen`: Shown when no track is selected
- `NowPlayingScreen`: Main playback UI with controls
- `PermissionRationaleScreen`: Permission request UI

**Permission Handling** (`ui/PermissionHandler.kt`)
- Runtime permission management for media access
- API level-aware: uses `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE` on older versions

**File Selection** (`ui/AudioFilePicker.kt`)
- System file picker integration using `ACTION_OPEN_DOCUMENT`
- Returns content URIs compatible with scoped storage

### Data Flow

1. User grants media permission
2. User selects audio file via system picker
3. ViewModel receives URI and creates TrackInfo
4. AudioPlayer loads and prepares media
5. Playback state flows to UI via StateFlow
6. UI recomposes to reflect current state

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with unidirectional data flow
- **Audio**: ExoPlayer 2.19.1
- **Concurrency**: Kotlin Coroutines and Flow
- **Build**: Gradle with Kotlin DSL and version catalog

## Current Limitations (Story 1 Scope)

This is the foundational implementation (Story 1). The following features are intentionally out of scope:

- Library scanning and browsing
- Background playback and notification controls
- Playlists and favorites
- Listening history
- Recommendations and discovery
- Search functionality

These features are planned for future iterations.

## Manual Testing

To validate the app:

1. **Permission scenarios**
   - First launch: permission should be requested
   - Deny permission: rationale screen should appear
   - Grant permission: file picker should become accessible

2. **File selection**
   - Select different audio formats (MP3, M4A, FLAC)
   - Verify track title displays correctly

3. **Playback controls**
   - Play/pause functionality
   - Seek bar interaction
   - Time display accuracy

4. **Error handling**
   - Select corrupted/invalid file
   - Verify error dialog appears with clear message

5. **Configuration changes**
   - Rotate device during playback
   - Verify state persists (ViewModel survives)

## Project Structure

```
app/src/main/java/com/anplak/androidmusic/
├── MainActivity.kt                    # App entry point
├── player/
│   ├── AudioPlayer.kt                # ExoPlayer wrapper
│   ├── PlaybackState.kt              # Playback state model
│   ├── PlayerError.kt                # Error types
│   └── TrackInfo.kt                  # Track metadata model
└── ui/
    ├── MusicPlayerApp.kt             # Root composable
    ├── PlaybackViewModel.kt          # ViewModel
    ├── PermissionHandler.kt          # Permission management
    ├── AudioFilePicker.kt            # File picker integration
    ├── EmptyStateScreen.kt           # Empty state UI
    ├── NowPlayingScreen.kt           # Now playing UI
    └── PermissionRationaleScreen.kt  # Permission rationale UI
```

## Future Roadmap

Planned features for upcoming stories:

- **Story 2**: Music library scan and browsing
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
