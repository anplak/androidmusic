package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo

data class LibraryFilter(
    val favoritesOnly: Boolean = false,
    val recentlyAdded: Boolean = false,
    val durationBucket: DurationBucket? = null
)

enum class DurationBucket {
    SHORT,
    MEDIUM,
    LONG
}

object LibraryFilterEngine {
    private const val SHORT_MAX_MS = 3 * 60 * 1000L
    private const val MEDIUM_MAX_MS = 8 * 60 * 1000L

    fun apply(
        tracks: List<TrackInfo>,
        filter: LibraryFilter,
        favoriteIds: Set<Long>,
        recentlyAddedIds: Set<Long>
    ): List<TrackInfo> {
        return tracks.filter { track ->
            (!filter.favoritesOnly || track.id in favoriteIds) &&
                (!filter.recentlyAdded || track.id in recentlyAddedIds) &&
                (filter.durationBucket == null || track.matches(filter.durationBucket))
        }
    }

    fun matchesLocalQuery(track: TrackInfo, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return track.title.contains(q, ignoreCase = true) ||
            track.artist.contains(q, ignoreCase = true) ||
            track.album.contains(q, ignoreCase = true)
    }

    private fun TrackInfo.matches(bucket: DurationBucket): Boolean {
        return when (bucket) {
            DurationBucket.SHORT -> duration in 1 until SHORT_MAX_MS
            DurationBucket.MEDIUM -> duration in SHORT_MAX_MS..MEDIUM_MAX_MS
            DurationBucket.LONG -> duration > MEDIUM_MAX_MS
        }
    }
}
