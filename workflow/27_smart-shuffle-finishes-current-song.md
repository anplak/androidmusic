# Smart Shuffle waits until the current song ends

## Description

Someone who is already listening taps Smart Shuffle (from the player, or “Smart shuffle play” on a playlist, or the same action started from a song) and the app cuts off the song that is playing and jumps to a new shuffled queue. They want that song to finish. Only after it ends should playback move into the Smart Shuffle queue.

**Assumption:** “From a song” means Smart Shuffle is started while a specific song is already playing, including from the player menu and from playlist “Smart shuffle play”. It does not add a new kind of shuffle.

**Assumption:** If nothing is playing, Smart Shuffle still starts playback immediately on the shuffled queue, as it does today.

**Assumption:** The song that was playing keeps its place in the song. It is not restarted from the beginning and it is not paused just because Smart Shuffle was chosen.

**Assumption:** If the user then taps Next or otherwise skips, that skip still leaves the current song. The following song comes from the Smart Shuffle queue. The protection is against the shuffle action itself interrupting the song, not against an explicit skip.

**Assumption:** The previous upcoming queue is replaced by the Smart Shuffle queue. Songs that were lined up before the shuffle are not kept after the current song.

## Goals and scope

### In scope

- Starting Smart Shuffle during playback lets the current song continue until it ends.
- When that song ends, playback continues with the Smart Shuffle queue.
- Starting Smart Shuffle while nothing is playing still starts the shuffled queue right away.
- An explicit skip after choosing Smart Shuffle moves into that shuffled queue.

### Out of scope

- Changing which songs Smart Shuffle prefers (favorites, play counts, avoiding recent repeats).
- Repeat one / repeat all.
- A new “shuffle from this song” that builds a mix only from songs similar to the current one.
- Restoring a queue after the app is force-stopped.

## Acceptance criteria

- [ ] While a song is playing, choosing Smart Shuffle does not change the song, does not restart it, and does not pause it.
- [ ] When that song reaches the end on its own, the next song is from the Smart Shuffle queue, not from the queue that was lined up before.
- [ ] The same behavior happens from the player’s Smart Shuffle action and from “Smart shuffle play” on a playlist while a song is playing.
- [ ] If playback is paused on a song and the user chooses Smart Shuffle, the paused song stays paused at the same position. After the user resumes and the song ends, the Smart Shuffle queue follows.
- [ ] If nothing is playing, Smart Shuffle starts playing the shuffled queue immediately.
- [ ] After Smart Shuffle was chosen, tapping Next leaves the current song and plays from the Smart Shuffle queue.

## Functional requirements

- Choosing Smart Shuffle while a track is active keeps that track as what is playing until it ends or the user skips it.
- The queue that follows that track is the Smart Shuffle queue for the same set of songs that action already used (the whole library, or the playlist or other list the action was started from).
- Playback moves to the first song of that shuffled queue only when the current song ends, or when the user skips forward.
- Choosing Smart Shuffle with no active track starts the shuffled queue immediately.
- Existing Smart Shuffle weighting and “avoid immediate repeats” behavior stay as they are.

## Non-functional requirements

- The handoff at the end of the song should feel like a normal track change, without a long silence, a repeated snippet of the song that just ended, or a second song starting on top of it.
- Choosing Smart Shuffle should not freeze the player or the rest of the app.

## UX design

No new control. The existing Smart Shuffle and “Smart shuffle play” actions stay where they are.

- Song playing: the user chooses Smart Shuffle; the same song keeps playing from the same position; when it ends, a shuffled song starts.
- Song paused: the same song stays paused; resuming continues it; the shuffle follows only after it ends or the user skips.
- Nothing playing: Smart Shuffle starts the shuffled queue immediately.
- No success message is required. The user hears the current song continue, which is the confirmation.

## Testing & validation

- Start a song, note the position, choose Smart Shuffle from the player, and confirm the same song is still playing past that position. Let it end and confirm the next song is a shuffled one, not the song that used to be next.
- Repeat while a playlist song is playing, using “Smart shuffle play”.
- Pause, choose Smart Shuffle, and confirm it stays paused on the same song until resumed.
- With nothing playing, choose Smart Shuffle and confirm playback starts at once.
- After choosing Smart Shuffle mid-song, tap Next and confirm the shuffled queue begins without waiting for the end.

## Value added and overall app state after this story

The user can decide “shuffle after this” without losing the rest of the song they are in. Smart Shuffle still starts a varied queue; it no longer cuts off whatever is already playing.
