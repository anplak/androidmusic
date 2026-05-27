package com.anplak.androidmusic.data

import android.net.Uri
import com.anplak.androidmusic.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryFilterEngineTest {

    @Test
    fun `favoritesOnly filters to favorite ids`() {
        val tracks = listOf(
            track(1, durationMs = 120_000),
            track(2, durationMs = 120_000)
        )
        val result = LibraryFilterEngine.apply(
            tracks = tracks,
            filter = LibraryFilter(favoritesOnly = true),
            favoriteIds = setOf(1L),
            recentlyAddedIds = emptySet()
        )
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun `duration short filter excludes long tracks`() {
        val tracks = listOf(
            track(1, durationMs = 120_000),
            track(2, durationMs = 600_000)
        )
        val result = LibraryFilterEngine.apply(
            tracks = tracks,
            filter = LibraryFilter(durationBucket = DurationBucket.SHORT),
            favoriteIds = emptySet(),
            recentlyAddedIds = emptySet()
        )
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun `matchesLocalQuery is case insensitive`() {
        val track = track(1, title = "Hello World", durationMs = 120_000)
        assertTrue(LibraryFilterEngine.matchesLocalQuery(track, "hello"))
        assertTrue(LibraryFilterEngine.matchesLocalQuery(track, "WORLD"))
    }

    @Test
    fun `combined filters apply intersection`() {
        val tracks = listOf(
            track(1, durationMs = 120_000),
            track(2, durationMs = 120_000),
            track(3, durationMs = 600_000)
        )
        val result = LibraryFilterEngine.apply(
            tracks = tracks,
            filter = LibraryFilter(
                favoritesOnly = true,
                durationBucket = DurationBucket.SHORT
            ),
            favoriteIds = setOf(1L, 2L, 3L),
            recentlyAddedIds = emptySet()
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it.duration < 180_000 })
    }

    private fun track(
        id: Long,
        title: String = "Title",
        durationMs: Long
    ): TrackInfo {
        return TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = "Artist",
            album = "Album",
            duration = durationMs
        )
    }
}
