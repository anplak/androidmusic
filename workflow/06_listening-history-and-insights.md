### User story

As a user, I want to see my listening history and simple stats so I can understand my habits and so the app can use this data for better recommendations.

### Goals & scope (2–3 days)

- **In scope**
  - **Listening history tracking**:
    - Log each play event with:
      - Track ID.
      - Timestamp.
      - Optional: session ID, duration listened.
  - **History views**:
    - “Recently Played” history timeline (per play, not just per track).
    - Ability to re-start playback from any history entry.
  - **Simple insights**:
    - Total play time today/this week.
    - Top tracks/artists for a recent period.
  - **Data retention**:
    - Basic policy for how much history to keep (e.g., last N entries or last N days).
- **Out of scope**
  - Complex charts/visualizations.
  - Export/sharing of stats.

### Functional requirements

- **FR1**: Every time a track is played to a threshold (e.g., 30 seconds or X% of its length), a history record is stored.
- **FR2**: User can open a “History” screen that shows a reverse-chronological list of plays.
- **FR3**: Tapping an item in History starts playback of that track.
- **FR4**: Simple stats such as:
  - Today’s listening time.
  - This week’s listening time.
  - Top N tracks in the last week.

### Non-functional requirements

- **NFR1**: History logging must be efficient and non-blocking.
- **NFR2**: DB queries for stats should be optimized (indexes as needed).

### UX notes

- **History navigation**:
  - Provide quick access from the main navigation (e.g., a “History” tab or entry).
- **Clarity**:
  - Show play time (e.g., “10:42 today”) and track details in the history list.
- **Privacy**:
  - Simple info in settings describing that history is stored locally only.

### Testing & validation

- Validate:
  - Entries are correctly added for repeated plays of the same track.
  - History truncation/cleanup works as intended.
  - Stats match manual expectations for test scenarios.

---

### Overall app state after this story

The app now maintains a **rich local listening history and basic insights**, feeding more accurate data into smart playlists and future recommendation logic.

### Value added after this story

Users get **visibility into their listening behavior** and the groundwork is laid for more personalized, Spotify-style recommendations based solely on on-device data.


