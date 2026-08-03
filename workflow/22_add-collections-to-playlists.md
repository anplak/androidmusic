### User story

As a user viewing an Artist or Album, I want to add the collection's tracks to a playlist in one action so I do not have to add every track individually.

### Description

Artist and Album detail screens already resolve their tracks and allow per-track “Add to playlist.” This story adds a collection-level action, generalizes the playlist picker to accept multiple tracks, and performs the update as one safe bulk operation. Existing playlist order is preserved and tracks already present are not duplicated.

**Estimate:** 2–3 days.

**Prerequisites:** stories **04** (persistent playlists) and **13** (Artist/Album detail). Story **21** is recommended but not required.

### Acceptance criteria

- **AC1:** Artist detail exposes an **Add to playlist** action that targets all tracks currently belonging to that artist.
- **AC2:** Album detail exposes the same action for that exact album-and-artist collection.
- **AC3:** The user can choose an existing playlist or create a new playlist from the collection action.
- **AC4:** Tracks already in the selected playlist are skipped; no duplicate playlist entries are created.
- **AC5:** New tracks are appended in the same deterministic order shown on the collection detail screen.
- **AC6:** A success message reports a human-readable outcome, such as “12 tracks added” or “Already in playlist.”
- **AC7:** Dismissing the picker or a failed write leaves the playlist unchanged.
- **AC8:** Existing single-track Add to playlist behavior continues to work.

### Functional requirements

- **FR1:** Add one collection-level action to `LibraryArtistDetailScreen` and `LibraryAlbumDetailScreen`; it may be a top-bar action or a clearly labeled button near **Play all**.
- **FR2:** Replace the single-track-only dialog contract with a reusable selection model that can represent one or many track IDs plus a display label.
- **FR3:** Extend `PlaylistRepository` with a bulk append operation that:
  - deduplicates incoming IDs while preserving order;
  - excludes IDs already in the destination;
  - appends after the current last position;
  - executes atomically.
- **FR4:** Creating a playlist from this flow inserts the collection tracks in the same transaction or cleans up the empty playlist if insertion fails.
- **FR5:** `PlaylistsViewModel` exposes operation state or an event containing added count, skipped count, and failure.
- **FR6:** The dialog must prevent submission for an empty collection and must not enqueue the operation twice on repeated taps.
- **FR7:** Keep per-track menu actions wired through the same generalized flow to avoid two divergent playlist pickers.

### Non-functional requirements

- **NFR1:** Adding a collection of 1,000 tracks must use bounded bulk database work, not one read/write transaction per track.
- **NFR2:** The operation must be atomic: observers see either the complete append or no append.
- **NFR3:** Playlist track ordering and uniqueness constraints remain valid under concurrent calls.
- **NFR4:** UI remains responsive while the database operation runs; disable or show progress on the committing action.
- **NFR5:** All user-facing text is resource-backed and pluralized correctly.

### UX design

- Place **Add to playlist** beside or near **Play all** using the same action hierarchy; do not hide the only collection action behind each track's overflow.
- Picker title includes context, for example **Add “Discovery” to playlist** or a subtitle such as **18 tracks**.
- Existing playlists show their name and track count; **New playlist** stays first.
- After selection, close the picker only when the operation is accepted, then show concise feedback near the current screen.
- For partial no-op outcomes, prefer “8 added · 3 already there” over technical database wording.
- Do not present hundreds of selected tracks inside the dialog; the collection name and count are sufficient.

### Testing/validation strategy

- **Unit — repository:** empty input, duplicate input IDs, some/all tracks already present, deterministic append positions, and rollback on injected failure.
- **Unit — ViewModel:** added/skipped counts, duplicate-submit protection, create-and-add behavior, and error state.
- **Compose/UI:** Artist and Album actions open the picker with the correct collection label/count; existing and new playlist paths work.
- **E2E:** add an album to an existing playlist, open playlist detail, verify order and count; repeat to confirm no duplicates.
- **E2E:** create a playlist from an artist, then verify every artist track appears once.
- **Regression:** single-track Add to playlist and existing playlist reorder/remove/merge tools remain functional.
- **Performance:** validate a seeded 1,000-track artist collection without UI blocking or visibly incremental inserts.

### Out of scope

- Selecting only some tracks from the Artist/Album before adding.
- Automatically creating one playlist per album or artist.
- Removing a whole Artist/Album from a playlist.
- Cloud sync, collaborative playlists, sharing, or export.
- Queue changes, smart shuffle, and auto-mix generation.

---

### Overall app state after this story

Artist and Album collections can be saved to new or existing playlists with one reliable bulk action.

### Value added after this story

Playlist curation becomes practical for collections while reusing and strengthening the existing single-track flow.
