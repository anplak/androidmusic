### User story

As a user, I want continuous playback with a queue, next/previous controls, and background/notification controls so I can use the app like a normal music player while doing other things on my phone.

### Goals & scope (2–3 days)

- **In scope**
  - **Queue model**:
    - Maintain an in-memory playback queue built from the library list.
    - Support basic operations: next, previous, jump to selected item.
  - **Player service**:
    - Move playback into a foreground service to support background audio.
    - Keep playback running when the app goes to background or screen turns off.
  - **Notification controls**:
    - Show a media-style notification with play/pause, next, previous.
    - Tapping notification brings user back to “Now Playing”.
  - **Lockscreen/MediaSession integration**:
    - Integrate with `MediaSession` for lockscreen and headset controls (play/pause, next/prev).
  - **Now Playing UI updates**:
    - Show “previous” and “next” buttons on the Now Playing screen.
    - Display current track index within the queue.
- **Out of scope**
  - Repeat modes and shuffle (beyond a very basic “play next in list”).
  - Persisting the queue across app restarts.

### Functional requirements

- **FR1**: Selecting a track from the library builds a queue (e.g., from that track onward) and starts playback.
- **FR2**: User can tap **Next** and **Previous** on the Now Playing screen to navigate the queue.
- **FR3**: When the app goes to background or screen turns off, playback continues without interruption.
- **FR4**: A media notification with play/pause and next/previous is visible while playing.
- **FR5**: Headset/lockscreen controls work with the same core actions (play/pause, next, previous).

### Non-functional requirements

- **NFR1**: Playback service must respect Android foreground service requirements (notification, channels).
- **NFR2**: Avoid multiple instances of the player running simultaneously.
- **NFR3**: Recover gracefully if the process is killed (e.g., stop playback and clear notification).

### UX notes

- **Seamless transitions**: Going from library to Now Playing, and then to background, should feel seamless.
- **Notification clarity**: Show track title and artist in the notification.
- **Queue assumption**: For this iteration, a simple queue (e.g., sorted by title or library order) is acceptable.

### Testing & validation

- Test:
  - Basic next/previous behavior.
  - Locking/unlocking screen during playback.
  - Swiping app away from recent apps and ensuring service behavior is correct.
  - Plugging/unplugging headphones if possible.

---

### Overall app state after this story

The app is now a **fully usable offline music player** with a queue and background playback, comparable to simple system music apps.

### Value added after this story

Users gain a **continuous listening experience** with proper background playback and media controls, making the app comfortable for real-world daily use.


