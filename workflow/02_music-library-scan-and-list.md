### User story

As a user, I want the app to automatically discover and list all my offline music so I can quickly browse and start playback without manually picking files every time.

### Goals & scope (2–3 days)

- **In scope**
  - **Media scanning**: Use `MediaStore` (or modern equivalents) to scan device-local audio files.
  - **Metadata extraction**: Read title, artist, album, duration, and file path (when available).
  - **Library screen**:
    - Simple scrollable list of all tracks.
    - Each row shows at least title and artist (or file name if metadata missing).
  - **Integration with existing player**:
    - Tapping a track opens the existing “Now Playing” screen from iteration 01.
    - Selected track starts playback immediately.
  - **Empty / permission states**:
    - Handle “no music found” gracefully.
    - Handle missing permissions by re-prompting or explaining how to enable them in settings.
- **Out of scope**
  - Sorting/filtering (beyond a simple default sort like title or recently added).
  - Albums/artists views.
  - Queues, favorites, and playlists.

### Functional requirements

- **FR1**: On app launch (after permission is granted), the app scans device audio content using `MediaStore` or similar API.
- **FR2**: The user sees a list of tracks within a few seconds, with smooth scrolling.
- **FR3**: Selecting a track from the library:
  - Navigates to the “Now Playing” screen.
  - Starts playback using the existing player.
- **FR4**: If no audio files are found, show a clear empty state with basic guidance.

### Non-functional requirements

- **NFR1**: Scanning should be efficient and done off the main thread.
- **NFR2**: UI should remain responsive during scanning (e.g., show loading indicator).
- **NFR3**: Avoid re-scanning on every minor lifecycle change; cache results for the current session.

### UX notes

- **Single entry point**: App opens to the library list once permissions are resolved.
- **Track row design**: Keep it clean; focus on legibility of title/artist.
- **Tap behavior**: Whole row should be tappable to start playback.

### Testing & validation

- Test with:
  - Many tracks (hundreds+).
  - Very few or no tracks.
  - Mix of supported/unsupported formats.
- Verify:
  - List renders quickly.
  - Tapping various items correctly plays the chosen track.

---

### Overall app state after this story

The app is now a **basic offline music library + player**: it discovers the user’s local songs, lists them, and lets the user tap any track to play it on the existing “Now Playing” screen.

### Value added after this story

Users can treat the app as a **simple daily driver music player**, no longer needing to manually browse the filesystem for each track.


