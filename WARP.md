# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a minimal offline Android music player built with Kotlin and Jetpack Compose. The project follows MVVM architecture with unidirectional data flow and is designed as a learning pet project focused on clarity and maintainability.

**Package**: `com.anplak.androidmusic`  
**Min SDK**: 26 (Android 8.0)  
**Target SDK**: 34 (Android 14)

## Build Commands

### Building the App
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew installDebug           # Build and install debug APK on connected device
```

### Running on Device/Emulator
Use Android Studio's run configuration or:
```bash
./gradlew installDebug
adb shell am start -n com.anplak.androidmusic/.MainActivity
```

### Gradle Tasks
```bash
./gradlew tasks                  # List all available tasks
./gradlew clean                  # Clean build artifacts
./gradlew dependencies           # Show dependency tree
```

## Architecture Overview

### MVVM + Unidirectional Data Flow

The app uses a clear separation of concerns with data flowing in one direction:

```
User Action → ViewModel → AudioPlayer → StateFlow → UI (Recompose)
```

**Key Architectural Layers:**

1. **UI Layer** (`ui/` package)
   - Jetpack Compose screens and composables
   - Minimal logic; delegates to ViewModel
   - Observes StateFlow and recomposes on state changes

2. **ViewModel Layer** (`PlaybackViewModel`)
   - Coordinates between UI and AudioPlayer
   - Combines multiple StateFlows into unified UI state
   - Extracts metadata (track titles from URIs)
   - Lifecycle-aware: releases resources on `onCleared()`

3. **Player Layer** (`player/` package)
   - `AudioPlayer`: Wraps ExoPlayer, manages playback lifecycle
   - Emits `PlaybackState` via StateFlow
   - Handles ExoPlayer callbacks and error mapping
   - Coroutine-based position updates (100ms intervals)

4. **Models**
   - `PlaybackState`: Player state (position, duration, isPlaying, error)
   - `TrackInfo`: Track metadata (URI, title)
   - `PlayerError`: Sealed error types (FileNotFound, UnsupportedFormat, Unknown)
   - `PlaybackUiState`: Combined UI state from ViewModel

### Data Flow Details

- **Permission Flow**: `PermissionHandler` → rationale screen → system dialog → file picker enabled
- **File Selection Flow**: System picker → URI → ViewModel extracts metadata → AudioPlayer loads → StateFlow updates → UI recomposes
- **Playback Flow**: User controls → ViewModel → AudioPlayer → ExoPlayer → StateFlow → UI

### ExoPlayer Integration

The `AudioPlayer` class wraps ExoPlayer and:
- Translates ExoPlayer states/events to app-specific models
- Maps PlaybackException error codes to `PlayerError` types
- Manages coroutine-based position polling (starts/stops with play/pause)
- Ensures proper cleanup on release

## Development Guidelines

### Code Principles (from .cursor/rules.yml)

- **Single-module architecture**: All code in `:app` module unless there's a clear need to split
- **Kotlin-first**: Use idiomatic Kotlin (`val` by default, extension functions, suspend functions)
- **Clarity over optimization**: Optimize for learning and readability; measure before optimizing
- **Minimal documentation**: Only README.md; keep it updated after major changes
- **Compose-first UI**: All UI in Jetpack Compose; avoid XML layouts
- **Lifecycle awareness**: Use `viewModelScope`, `lifecycleScope`; never `GlobalScope`
- **Immutable data**: Prefer immutable data classes; use `StateFlow` for state management
- **Explicit error handling**: Use sealed types or `Result`; don't swallow exceptions

### Coroutines & Concurrency

- Always launch coroutines in proper scopes (`viewModelScope`, `lifecycleScope`)
- Use `suspend` functions with explicit dispatchers
- Avoid fire-and-forget launches unless documented
- `AudioPlayer` runs position updates in provided `CoroutineScope` (from ViewModel)

### Permissions

The app uses API-level-aware permission handling:
- API 33+: `READ_MEDIA_AUDIO`
- API 26-32: `READ_EXTERNAL_STORAGE`

`PermissionHandler.kt` abstracts this logic and provides a composable for runtime permission requests.

### File Handling

- Uses system file picker (`ACTION_OPEN_DOCUMENT`) for scoped storage compliance
- Returns content URIs (not file paths)
- `PlaybackViewModel` extracts display names via `ContentResolver`
- Fallback: URI path segment if cursor query fails

### State Management

- All state flows from ViewModels via StateFlow
- UI layer is stateless (state hoisting)
- `combine` operator merges multiple flows in ViewModel
- Position updates every 100ms during playback

## Dependency Management

Dependencies are managed via **version catalog** (`gradle/libs.versions.toml`):

**Key Dependencies:**
- Jetpack Compose (Material 3)
- ExoPlayer 2.19.1
- Lifecycle ViewModel Compose
- AndroidX Core KTX

To add dependencies, update `libs.versions.toml` first, then reference in `app/build.gradle.kts` using `alias(libs.*)`.

## Current Limitations (Story 1 Scope)

This is a foundational implementation. The following are **intentionally not implemented**:
- Music library scanning/browsing
- Background playback and media notifications
- Playlists or favorites
- Listening history
- Search functionality
- Recommendations

Future development is tracked in story-based roadmap (see README.md).

## No Tests Yet

Test directories (`test/`, `androidTest/`) do not currently exist. When adding tests:
- Prefer unit tests for ViewModels and business logic
- Use fakes/test doubles over heavy mocking
- Test key ViewModel state transformations and error handling
- Keep test setup simple; avoid heavy DI frameworks

## Manual Testing Approach

Since there are no automated tests, validate changes by:
1. **Permission flow**: Deny → rationale → grant → file picker accessible
2. **File selection**: Different formats (MP3, M4A, FLAC) display correctly
3. **Playback**: Play/pause, seek, time display
4. **Error handling**: Select invalid file → error dialog with message
5. **Configuration changes**: Rotate during playback → state persists

## Common Issues

### ExoPlayer errors
- Check `AudioPlayer.kt` listener for error code mapping
- Errors flow to `PlayerError` sealed class → UI shows dialog
- Call `audioPlayer.clearError()` to dismiss

### Metadata extraction failures
- `PlaybackViewModel.extractTrackTitle()` may fall back to "Unknown Track"
- Some URIs don't provide `DISPLAY_NAME` in cursor

### Permissions not working
- Verify AndroidManifest.xml has correct min/max SDK versions
- Check `PermissionHandler.kt` selects correct permission for API level
