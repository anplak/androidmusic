### User story

As a user, I want more powerful playlist tools—like reordering, merging, and auto-mixes—so I can curate my library with flexibility similar to Spotify’s playlist management.

### Goals & scope (2–3 days)

- **In scope**
  - **Playlist editing enhancements**:
    - Drag-and-drop reordering of tracks inside a playlist.
    - Remove multiple tracks at once (multi-select).
  - **Playlist utilities**:
    - Duplicate an existing playlist.
    - Merge two playlists into a new one.
  - **Auto-mix creation**:
    - Generate a new playlist using a seed:
      - From a favorite track or artist.
      - From a smart playlist (e.g., “Most Played”) snapshot.
    - Allow the user to save that generated mix as a regular playlist for future editing.
  - **Smart queue from playlist**:
    - Option to start a playlist with smart shuffle + queue logic tailored to its contents.
- **Out of scope**
  - Cross-device sync or sharing of playlists.
  - Collaborative playlists.

### Functional requirements

- **FR1**: User can reorder tracks within a playlist and have that order persist.
- **FR2**: User can select multiple tracks in a playlist to remove them in a single action.
- **FR3**: User can duplicate or merge playlists into a new playlist (with deduping of duplicate tracks).
- **FR4**: Auto-mix generation:
  - Produces a coherent subset of tracks based on a chosen seed or smart playlist.
  - Can be previewed and then saved as a normal playlist.

### Non-functional requirements

- **NFR1**: Reordering interactions must feel smooth, with visual feedback (drag handles, animations).
- **NFR2**: Playlist DB operations should be efficient even for large playlists.

### UX notes

- **Interaction design**:
  - Use a clear drag handle icon on playlist rows.
  - Provide a contextual menu with “Duplicate”, “Merge into…”, and “Generate Mix”.
- **Safety**:
  - Confirm before destructive changes (e.g., bulk removal).
- **Discoverability**:
  - Explain (briefly) what auto-mix does the first time a user tries it.

### Testing & validation

- Test:
  - Reordering on small and large playlists.
  - Merging playlists with overlapping tracks.
  - Creating and saving auto-mixes based on different seeds.

---

### Overall app state after this story

The app now has **robust playlist management and auto-mix tools**, approaching Spotify-like flexibility for organizing and generating listening experiences from an offline library.

### Value added after this story

Users gain **fine-grained control and higher-level automation** for playlist curation, making the app feel powerful for both casual and power users.


