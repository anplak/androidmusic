# Android Music Player

A minimal offline music player for Android that automatically discovers and plays local audio files without any network dependency.

## Features

- **Offline-first**: No network or account required
- **Automatic library scanning**: Discovers audio files on device via MediaStore
- **Library indexing rules**: By default skips tracks longer than 10 minutes; exclude (or include-only) folders and subfolders from the index
- **Music library browsing**: Scrollable list of tracks with title, artist, and duration
- **Favorites**: Mark tracks as favorites with a heart icon, view all favorites in a dedicated tab
- **For You**: Personalized discovery tab with Daily Mix, Quick Mix, “Because you listen to…”, and Continue Listening rows (fully on-device)
- **Search & filters**: Global search across library, playlists, and history; library filter chips (favorites, recently added, duration)
- **Playlists**: Create playlists, reorder tracks, bulk remove, duplicate/merge, and auto-mix from seeds
- **Smart playlists**: Auto-generated playlists including Most Played, Recently Played, and Recently Added
- **Smart shuffle**: Weighted shuffle that favors favorites and frequently played tracks
- **Play statistics**: Tracks play count and completion for smart features
- **Listening history**: Per-play history timeline with track details and duration listened
- **Simple insights**: Today and weekly play time, top tracks and artists
- **Data retention**: Automatic cleanup of history older than 90 days
- **Playback queue**: Selecting a track builds a queue from the library for continuous playback
- **Queue navigation**: Next/previous controls with queue position indicator
- **Background playback**: Music continues playing when app is in background or screen is off
- **Media notification**: Control playback from the notification shade with play/pause, next, previous
- **Lockscreen and headset controls**: MediaSession integration for system-wide media controls
- **Bottom navigation**: Five-tab navigation for For You, Library, Favorites, Playlists, and History
- **Persistent storage**: Favorites, playlists, and play statistics persist using Room database
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

### Database Layer

**AppDatabase** (`data/db/AppDatabase.kt`)
- Room database singleton for local persistence
- Stores track cache, favorites, and playlists

**Entities** (`data/db/Entities.kt`)
- `TrackEntity`: Cached track metadata from MediaStore with first-seen timestamp
- `FavoriteEntity`: Track ID reference with timestamp
- `PlaylistEntity`: Playlist name and creation time
- `PlaylistTrackCrossRef`: Links tracks to playlists with ordering
- `TrackStatsEntity`: Play count, last played timestamp, and completion count
- `PlayHistoryEntity`: Individual play event with timestamp, duration, and session ID

**DAOs** (`data/db/`)
- `TrackDao`: Track cache operations and recently added queries
- `FavoriteDao`: Favorite management
- `PlaylistDao`: Playlist CRUD and track management
- `TrackStatsDao`: Play statistics recording and queries
- `PlayHistoryDao`: Play history recording and analytics queries

### Repository Layer

**MusicLibraryRepository** (`data/MusicLibraryRepository.kt`)
- Queries MediaStore for device audio files
- Applies index policy (max duration, folder include/exclude rules) during sync
- Caches indexed tracks in Room for stable ID mapping
- Executes queries off main thread using coroutines

**LibraryIndexPolicyRepository** (`data/LibraryIndexPolicyRepository.kt`)
- Loads max-duration setting and folder rules from SharedPreferences and Room
- Persists folder include/exclude rules for re-scan on change

**FavoritesRepository** (`data/FavoritesRepository.kt`)
- Toggle favorite status for tracks
- Query all favorites with reactive Flow
- Expose favorite IDs for UI state

**PlaylistRepository** (`data/PlaylistRepository.kt`)
- Create, delete, rename playlists
- Add/remove tracks from playlists
- Query playlist tracks with ordering

**TrackStatsRepository** (`data/TrackStatsRepository.kt`)
- Record play and completion events
- Query play statistics per track
- Provide stats for smart shuffle weighting

**SmartPlaylistRepository** (`data/SmartPlaylistRepository.kt`)
- Query most played tracks by play count
- Query recently played tracks by timestamp
- Query recently added tracks by first-seen date

**PlayHistoryRepository** (`data/PlayHistoryRepository.kt`)
- Record individual play events with timestamp and duration
- Query play history with pagination
- Calculate total play time for time periods
- Get top tracks and artists for insights
- Data retention cleanup

### ViewModel Layer

**LibraryViewModel** (`ui/LibraryViewModel.kt`)
- Manages library screen state (Loading, Content, Empty)
- Exposes favorite status per track
- Session caching to avoid redundant scans

**PlaybackViewModel** (`ui/PlaybackViewModel.kt`)
- Manages playback UI state and queue operations
- Exposes favorite status for current track
- Coordinates between AudioPlayer and UI layer
- Records play statistics and history on track changes
- Tracks session ID for grouping plays
- Provides smart shuffle functionality

**FavoritesViewModel** (`ui/FavoritesViewModel.kt`)
- Manages favorites list state
- Handles toggle favorite action

**PlaylistsViewModel** (`ui/PlaylistsViewModel.kt`)
- Manages playlists list and detail state
- Handles create/delete playlist operations
- Manages add/remove tracks from playlists

**SmartPlaylistsViewModel** (`ui/SmartPlaylistsViewModel.kt`)
- Manages smart playlist detail state
- Loads tracks for Most Played, Recently Played, Recently Added

**HistoryViewModel** (`ui/HistoryViewModel.kt`)
- Manages history list state with pagination
- Handles data retention cleanup on startup
- Supports pull-to-refresh

**InsightsViewModel** (`ui/InsightsViewModel.kt`)
- Calculates today and weekly play time
- Loads top tracks and artists for time periods
- Fetches track metadata for display

### UI Layer (Compose)

- `MusicPlayerApp`: Root composable with bottom navigation and screen routing
- `LibraryScreen`: Scrollable list with favorite toggle and playlist menu
- `FavoritesScreen`: List of favorited tracks
- `PlaylistsScreen`: List of playlists with smart playlists section and create dialog
- `PlaylistDetailScreen`: Playlist tracks with reorder, bulk remove, duplicate/merge, and auto-mix
- `SmartPlaylistDetailScreen`: Read-only smart playlist tracks with play all
- `NowPlayingScreen`: Playback UI with favorite toggle, add-to-playlist, and smart shuffle
- `HistoryScreen`: Reverse chronological list of play history with pagination
- `InsightsScreen`: Today and weekly stats with top tracks and artists
- `AddToPlaylistDialog`: Modal for adding tracks to playlists
- `PermissionRationaleScreen`: Permission request UI
- `TimeFormatter`: Utility for formatting timestamps and durations

**Permission Handling** (`ui/PermissionHandler.kt`)
- Runtime permission management for media access
- API level-aware: uses `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE` on older versions

### Data Flow

1. User grants media permission
2. App scans device audio via MediaStore (off main thread)
3. Tracks are cached in Room database for stable IDs
4. Library screen displays list of discovered tracks with favorite status
5. User can toggle favorites or add tracks to playlists
6. User taps a track to build a queue and start playback
7. MusicPlaybackService starts playback in foreground
8. Play event recorded to history with timestamp and session ID
9. Playback state flows to UI via MediaController and StateFlow
10. History entry updated with duration when track completes or changes
11. Bottom navigation allows switching between Library, Favorites, Playlists, and History
12. Favorites, playlists, and history persist across app restarts
13. History older than 90 days is automatically cleaned up

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with unidirectional data flow
- **Audio**: Jetpack Media3 1.2.1 (ExoPlayer + MediaSession)
- **Database**: Room 2.6.1 with KSP annotation processing
- **Concurrency**: Kotlin Coroutines and Flow
- **Build**: Gradle with Kotlin DSL and version catalog
- **Testing**: JUnit, Mockito, Robolectric, Room Testing, Coroutines Test

## Current Limitations

The following features are intentionally out of scope for the current iteration:

- Repeat mode (basic shuffle available via smart shuffle)
- Persistent queue across app restarts
- Albums/artists dedicated browse views
- Sorting options
- Export/sharing of listening stats
- Complex charts/visualizations

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

4. **Favorites**
   - Tap heart icon on a track to add to favorites
   - Verify heart fills in and track appears in Favorites tab
   - Tap heart again to remove from favorites
   - Verify empty state in Favorites tab when no favorites

5. **Playlists**
   - Navigate to Playlists tab
   - Create a new playlist using the FAB
   - Add tracks to playlist from library or Now Playing
   - Open playlist detail and verify tracks appear
   - Reorder tracks with drag handles
   - Remove tracks (single or multi-select)
   - Duplicate or merge playlists into a new one
   - Generate an auto-mix from a seed and save it
   - Use "Smart shuffle play" from the playlist menu to start a weighted shuffle of that playlist
   - Delete playlist

6. **Smart playlists**
   - Navigate to Playlists tab
   - Verify smart playlists section appears (Most Played, Recently Played, Recently Added)
   - Tap "Most Played" - verify tracks ordered by play count
   - Tap "Recently Played" - verify tracks ordered by last played time
   - Tap "Recently Added" - verify tracks ordered by when first seen
   - Tap "Play All" on any smart playlist to start playback

7. **Smart shuffle**
   - Start playing any track
   - Open the more menu (three dots) on Now Playing
   - Tap "Smart Shuffle"
   - Verify playback continues with shuffled queue
   - Play several tracks to build play history
   - Use smart shuffle again - favorites and frequently played should appear more often

8. **Search & filters**
   - Library tab: use filter chips (Favorites, Recently added, duration) and the library search field
   - Tap the search icon on Library or Playlists for global search (tracks, playlists, history)
   - Tap a track or history result to play; tap a playlist to open detail
   - On-device E2E: `./scripts/run-e2e-search-filter-wifi.sh` (requires adb + Wi-Fi debugging)

9. **Listening history**
   - Navigate to History tab
   - Verify empty state when no history
   - Play a track from the library
   - Navigate back to History tab
   - Verify play entry appears with track title, artist, and timestamp
   - Verify "today" or specific time is shown
   - Play more tracks - verify entries appear in reverse chronological order
   - Tap a history entry - verify playback starts from that track
   - Scroll down to load more history (if many entries exist)

9. **Simple insights**
   - Play several tracks to accumulate listening time
   - View insights (Today's play time, This week's play time)
   - Verify play time updates after playing more tracks
   - Check top tracks list - verify most played tracks appear
   - Check top artists list - verify artists with most plays appear

10. **Track selection and queue**
    - Tap a track to start playback
    - Verify Now Playing screen shows correct track info
    - Verify queue position indicator shows (e.g., "3 / 10")

11. **Queue navigation**
    - Next button advances to next track
    - Previous button goes to previous track
    - Buttons are disabled at queue boundaries

12. **Playback controls**
    - Play/pause functionality
    - Seek bar interaction
    - Time display accuracy

13. **Background playback**
    - Start playback and press home button
    - Verify music continues playing
    - Lock screen and verify music continues

14. **Notification controls**
    - Verify media notification appears during playback
    - Test play/pause from notification
    - Test next/previous from notification
    - Tap notification to return to Now Playing

15. **Lockscreen and headset controls**
    - Verify lockscreen shows media controls
    - Test play/pause from lockscreen
    - Test headset button controls (if available)

16. **Navigation**
    - Bottom navigation switches between Library, Favorites, Playlists, and History
    - Back button returns from Now Playing to current tab
    - Playback continues when navigating between screens

17. **Persistence**
    - Add favorites and create playlists
    - Play some tracks to generate statistics and history
    - Force stop the app
    - Relaunch and verify favorites/playlists/stats/history persist

18. **Data retention**
    - History entries older than 90 days are automatically cleaned up
    - Verify cleanup happens on app startup

19. **Error handling**
    - If a track fails to play, verify error dialog appears

20. **Configuration changes**
    - Rotate device during playback
    - Verify state persists (ViewModel survives)

21. **App lifecycle**
    - Swipe app away from recents while playing
    - Verify playback stops and notification clears

## Project Structure

```
app/src/main/java/com/anplak/androidmusic/
├── MainActivity.kt                    # App entry point
├── data/
│   ├── MusicLibraryRepository.kt     # MediaStore access
│   ├── FavoritesRepository.kt        # Favorites data access
│   ├── PlaylistRepository.kt         # Playlists data access
│   ├── TrackStatsRepository.kt       # Play statistics data access
│   ├── SmartPlaylistRepository.kt    # Smart playlists queries
│   ├── PlayHistoryRepository.kt      # Play history data access
│   └── db/
│       ├── AppDatabase.kt            # Room database singleton
│       ├── Entities.kt               # Database entities
│       ├── TrackDao.kt               # Track cache DAO
│       ├── FavoriteDao.kt            # Favorites DAO
│       ├── PlaylistDao.kt            # Playlists DAO
│       ├── TrackStatsDao.kt          # Play statistics DAO
│       └── PlayHistoryDao.kt         # Play history DAO
├── player/
│   ├── AudioPlayer.kt                # MediaController bridge
│   ├── PlaybackQueue.kt              # Queue model
│   ├── PlaybackState.kt              # Playback state model
│   ├── PlayerError.kt                # Error types
│   ├── AutoMixGenerator.kt           # Auto-mix playlist generation
│   ├── SmartShuffleGenerator.kt      # Weighted shuffle algorithm
│   └── TrackInfo.kt                  # Track metadata model
├── service/
│   └── MusicPlaybackService.kt       # Background playback service
└── ui/
    ├── MusicPlayerApp.kt             # Root composable with navigation
    ├── LibraryScreen.kt              # Library list UI
    ├── LibraryViewModel.kt           # Library state management
    ├── FavoritesScreen.kt            # Favorites list UI
    ├── FavoritesViewModel.kt         # Favorites state management
    ├── PlaylistsScreen.kt            # Playlists list with smart section
    ├── PlaylistDetailScreen.kt       # Playlist detail UI
    ├── PlaylistsViewModel.kt         # Playlists state management
    ├── SmartPlaylistDetailScreen.kt  # Smart playlist detail UI
    ├── SmartPlaylistsViewModel.kt    # Smart playlists state management
    ├── HistoryScreen.kt              # History timeline UI
    ├── HistoryViewModel.kt           # History state management
    ├── InsightsScreen.kt             # Listening insights UI
    ├── InsightsViewModel.kt          # Insights state management
    ├── TimeFormatter.kt              # Time formatting utilities
    ├── AddToPlaylistDialog.kt        # Add to playlist modal
    ├── PlaybackViewModel.kt          # Playback state management
    ├── PermissionHandler.kt          # Permission management
    ├── NowPlayingScreen.kt           # Now playing UI with smart shuffle
    └── PermissionRationaleScreen.kt  # Permission rationale UI
```

## Future Roadmap

Planned workflow stories (see `.cursor/workflow/` for specs):

- **Story 10**: Polish, performance, and release candidate

**Stories 7–9** (recommendations, playlist curation, search & filters) are **implemented**.

Still planned:

## License

This is a personal pet project for learning and experimentation.
