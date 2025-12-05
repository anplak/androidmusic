package com.anplak.androidmusic.player

/**
 * Immutable playback queue holding an ordered list of tracks and the current position.
 */
data class PlaybackQueue(
    val tracks: List<TrackInfo>,
    val currentIndex: Int = 0
) {
    val currentTrack: TrackInfo?
        get() = tracks.getOrNull(currentIndex)
    
    val size: Int
        get() = tracks.size
    
    val isEmpty: Boolean
        get() = tracks.isEmpty()
    
    fun hasNext(): Boolean = currentIndex < tracks.lastIndex
    
    fun hasPrevious(): Boolean = currentIndex > 0
    
    fun next(): PlaybackQueue {
        return if (hasNext()) {
            copy(currentIndex = currentIndex + 1)
        } else {
            this
        }
    }
    
    fun previous(): PlaybackQueue {
        return if (hasPrevious()) {
            copy(currentIndex = currentIndex - 1)
        } else {
            this
        }
    }
    
    fun jumpTo(index: Int): PlaybackQueue {
        return if (index in tracks.indices) {
            copy(currentIndex = index)
        } else {
            this
        }
    }
    
    companion object {
        val EMPTY = PlaybackQueue(emptyList(), 0)
        
        /**
         * Creates a queue from a library list, starting at the selected track.
         * The queue contains all tracks from the selected position onward.
         */
        fun fromLibrary(tracks: List<TrackInfo>, startIndex: Int): PlaybackQueue {
            if (tracks.isEmpty() || startIndex !in tracks.indices) {
                return EMPTY
            }
            return PlaybackQueue(tracks, startIndex)
        }
    }
}

