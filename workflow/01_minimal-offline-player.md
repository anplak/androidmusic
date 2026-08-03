### User story

As a user, I want a minimal offline music player so that I can open the app, select a few local audio files, and play them without any account, network, or setup.

### Goals & scope (2–3 days)

- **In scope**
  - **Project setup**: Create a new Android app project (Kotlin, modern Android, e.g. Jetpack Compose or modern XML).
  - **Permissions**: Request/read storage/media permissions needed to access local audio (scoped storage aware).
  - **File selection**: Simple system-driven file picker to select one or more local audio files.
  - **Playback core**: Integrate a stable audio playback engine (e.g. ExoPlayer) that can:
    - Play/pause a single selected track.
    - Display basic track position and duration.
    - Seek within the track with a slider.
  - **Basic UI**:
    - A single “Now Playing” screen with play/pause, seek bar, and track title.
    - Basic error states (e.g. can’t read file, unsupported format).
  - **No network**: Ensure the app has no network dependency and works offline.
- **Out of scope**
  - Library scanning.
  - Background playback and notification controls.
  - Playlists, favorites, recommendations.
  - Any form of personalization or analytics.

### Functional requirements

- **FR1**: User can launch the app and grant media/storage permission when prompted.
- **FR2**: User can open a file picker and choose at least one audio file from device storage.
- **FR3**: After selecting a file, the app shows a “Now Playing” screen with:
  - Track name (from metadata or file name).
  - Play/Pause button.
  - Seek bar showing progress and total duration.
- **FR4**: Playback must continue smoothly while the screen is on and app is in foreground.
- **FR5**: If playback fails (e.g., corrupt file), a simple, clear error message is shown.

### Non-functional requirements

- **NFR1**: App must not require network access at any point.
- **NFR2**: Startup from cold state should be under a few seconds on a mid-range device.
- **NFR3**: App should not crash on permission denial; instead, show a “permission needed” explanation.

### UX notes

- **Minimal friction**: On first launch, walk the user directly to permission + file selection.
- **Clear controls**: Use large, obvious Play/Pause button and an easily draggable seek bar.
- **Empty state**: If no file is selected yet, show a simple message and a prominent “Select music” button.

### Testing & validation

- **Manual tests**
  - Start app with permission granted / denied / revoked.
  - Select different file types (mp3, m4a, etc.).
  - Seek multiple times during playback.
  - Rotate device if orientation changes are supported and verify state is preserved or safely reset.
- **Basic instrumentation/unit tests (optional if time allows)**
  - Player wrapper tests for play/pause/seek behavior.

---

### Overall app state after this story

The app is a **minimal but usable offline music player**: users can open it, grant permissions, pick a local audio file, and play it on a simple “Now Playing” screen.

### Value added after this story

Users already gain **practical value**: they can play local tracks offline with a clean, focused UI, forming the foundation for all future library, playlist, and recommendation features.


