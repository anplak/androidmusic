package com.anplak.androidmusic.player

/**
 * Owns transport and queue mutations for playback, including stats-tracker start markers.
 */
class PlaybackController(
    private val audioPlayer: AudioPlayer,
    private val playbackStatsTracker: PlaybackStatsTracker,
) {
    var queue: PlaybackQueue = PlaybackQueue.EMPTY
        private set

    fun connect() {
        audioPlayer.connect()
    }

    fun onTrackSelected(
        tracks: List<TrackInfo>,
        selectedIndex: Int,
    ) {
        playbackStatsTracker.markExplicitStart()
        queue = PlaybackQueue.fromLibrary(tracks, selectedIndex)
        audioPlayer.setQueue(tracks, selectedIndex)
    }

    fun clearQueueAndStop() {
        queue = PlaybackQueue.EMPTY
        audioPlayer.stop()
    }

    fun onPlayPause(isPlaying: Boolean) {
        if (isPlaying) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }

    fun onNext() {
        playbackStatsTracker.markAutoAdvance()
        audioPlayer.next()
    }

    fun onPrevious() {
        playbackStatsTracker.markAutoAdvance()
        audioPlayer.previous()
    }

    fun onSeek(position: Long) {
        audioPlayer.seekTo(position)
    }

    fun onErrorDismissed() {
        audioPlayer.clearError()
    }

    fun release() {
        audioPlayer.release()
    }

    fun recentTrackIds(limit: Int): Set<Long> =
        queue.tracks
            .take(limit)
            .map { it.id }
            .toSet()
}
