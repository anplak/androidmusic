# Player can open the play queue, including songs just played

## Description

On the player, someone can go to the next or previous song and can see a position such as which track this is, but they cannot open the play queue and see what is coming or what they just heard. They want a way from the player into the queue, including the last few songs played before the current one.

**Assumption:** “Player view” is the Now Playing screen. The queue is the current listening session, not the History tab and not the Recently Played list.

**Assumption:** “The last few” means up to the five songs played before the current one in this session. If fewer than five were played, only those are shown.

**Assumption:** The user can tap a song in that queue (upcoming or just played) to play it. Reordering or removing songs is not included.

**Assumption:** One clear Queue control on the player is enough. Playback continues while the queue is open.

## Goals and scope

### In scope

- From the player, the user can open the current play queue.
- The queue shows what is playing now, what is coming up, and the last few songs played in this session.
- The user can tap a listed song to play it.
- Closing the queue returns to the player without stopping playback.

### Out of scope

- Reordering, removing, or adding songs from the queue.
- The full play history, insights, or the Recently Played playlist.
- Showing the queue on the lock screen or in the notification.
- Restoring the queue after the app is force-stopped.
- Changing what Smart Shuffle puts in the queue.

## Acceptance criteria

- [ ] From the player, the user can open the play queue in one action.
- [ ] The queue marks the song that is playing now.
- [ ] Songs still to come appear after the current song, in the order they will play.
- [ ] Up to the last five songs played earlier in this session appear as recently played. Fewer are shown when fewer were played.
- [ ] Those recent songs are from this session’s queue, not a separate history of older days.
- [ ] Tapping an upcoming song plays it and makes the following songs the new upcoming list.
- [ ] Tapping a recently played song plays it.
- [ ] Opening and closing the queue does not pause, skip, or restart the current song.
- [ ] With only the current song and nothing played before it, the user still opens the queue and sees that nothing else is lined up and nothing was just played.

## Functional requirements

- The player offers a visible way to open the play queue whenever a song session is active.
- The queue lists three parts: recently played in this session (up to five), the current song, and up next.
- Order of “up next” matches the order Next will use.
- Choosing a song in the queue starts that song and continues from there.
- The queue updates when the song changes, including when a song ends on its own or the user skips.
- If there is no upcoming song, or no recent song, that part is empty rather than filled with unrelated tracks.

## Non-functional requirements

- Opening the queue does not interrupt audio.
- The Queue control and the current song have names a screen reader can announce.
- The list stays usable on a small phone screen: the current song is identifiable without scrolling past a long history.

## UX design

The player gains a Queue action (the existing track position may also open it, but a labeled Queue action must be present).

The queue is a list with:

- **Recently played** — the last few songs of this session, most recent first, hidden when there are none.
- **Now playing** — the current song, clearly marked.
- **Up next** — the rest of the queue. If there is nothing next, show short copy: “Nothing up next”.

Tapping a row plays that song. A back or close control returns to the player. No confirmation dialog. Playback keeps going while the list is open.

## Testing & validation

- Play several songs in a row, open the queue from the player, and confirm the current song, the songs still to come, and up to five songs just played.
- Confirm a sixth-back song from this session is not required to appear.
- Tap an upcoming song and confirm it plays and the old “up next” order shifts with it.
- Tap a recently played song and confirm it plays.
- Open and close the queue mid-song and confirm the song does not pause or restart.
- Start playback of a single song with no prior play in the session and confirm the queue opens with an empty recent section and nothing up next.
- Confirm the History tab is unchanged.

## Value added and overall app state after this story

From the player, the user can see and jump around the session queue: what just played, what is playing, and what comes next. They no longer have to guess the queue from Next and Previous alone.
