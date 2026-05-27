package com.anplak.androidmusic.data

import android.net.Uri
import com.anplak.androidmusic.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchEngineTest {

    private lateinit var engine: SearchEngine

    @Before
    fun setup() {
        engine = SearchEngine()
    }

    @Test
    fun `buildGrouped returns empty for blank query`() {
        val raw = sampleRaw()
        val result = engine.buildGrouped("   ", raw)
        assertEquals(0, result.totalCount)
        assertTrue(result.sections.isEmpty())
    }

    @Test
    fun `buildGrouped includes track and artist sections`() {
        val raw = SearchRawResults(
            tracks = listOf(
                track(1, "Beat It", "Michael Jackson"),
                track(2, "Billie Jean", "Michael Jackson")
            ),
            playlists = emptyList(),
            history = emptyList()
        )
        val result = engine.buildGrouped("beat", raw)
        val headers = result.sections.map { it.header }
        assertTrue(headers.contains(SearchEngine.SECTION_TRACKS))
        assertTrue(headers.contains(SearchEngine.SECTION_ARTISTS))
        assertEquals(1, result.sections.first { it.header == SearchEngine.SECTION_ARTISTS }.items.size)
    }

    @Test
    fun `buildGrouped includes playlist and history sections`() {
        val raw = SearchRawResults(
            tracks = emptyList(),
            playlists = listOf(Playlist(1, "Workout", 0L, 5)),
            history = listOf(
                PlayHistoryEntry(
                    id = 9,
                    trackId = 3,
                    playedAt = 0,
                    duration = 0,
                    sessionId = null,
                    track = track(3, "Song", "Artist")
                )
            )
        )
        val result = engine.buildGrouped("work", raw)
        assertTrue(result.sections.any { it.header == SearchEngine.SECTION_PLAYLISTS })
        assertTrue(result.sections.any { it.header == SearchEngine.SECTION_HISTORY })
    }

    private fun sampleRaw() = SearchRawResults(
        tracks = listOf(track(1, "A", "B")),
        playlists = emptyList(),
        history = emptyList()
    )

    private fun track(id: Long, title: String, artist: String): TrackInfo {
        return TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = artist,
            album = "Album",
            duration = 240_000L
        )
    }
}
