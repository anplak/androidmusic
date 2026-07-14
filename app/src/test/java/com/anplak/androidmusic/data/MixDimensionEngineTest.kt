package com.anplak.androidmusic.data

import android.net.Uri
import com.anplak.androidmusic.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MixDimensionEngineTest {

    private val epochDay = 20_000L

    @Test
    fun `effectiveGenre prefers id3 genre over folder tag`() {
        val track = track(1, genre = "Rock", folderTag = "metal")
        assertEquals("Rock", track.effectiveGenre())
    }

    @Test
    fun `effectiveGenre falls back to folder tag`() {
        val track = track(1, folderTag = "metal")
        assertEquals("metal", track.effectiveGenre())
    }

    @Test
    fun `pickGenreMix omitted when bucket below threshold`() {
        val library = (1L..7L).map { track(it, folderTag = "metal") }
        val picks = MixDimensionEngine.pickCandidates(emptyInputs(library), epochDay)
        assertTrue(picks.none { it.type == RecommendationRowType.GENRE_MIX })
    }

    @Test
    fun `pickGenreMix selects largest bucket`() {
        val metal = (1L..10L).map { track(it, folderTag = "metal") }
        val rock = (11L..14L).map { track(it, genre = "Rock") }
        val picks = MixDimensionEngine.pickCandidates(emptyInputs(metal + rock), epochDay)
        val genreMix = picks.first { it.type == RecommendationRowType.GENRE_MIX }
        assertEquals("Your Metal mix", genreMix.title)
        assertEquals(10, genreMix.pool.size)
    }

    @Test
    fun `pickGenreMix folder sourced title`() {
        val library = (1L..8L).map { track(it, folderTag = "metal") }
        val genreMix = MixDimensionEngine.pickCandidates(emptyInputs(library), epochDay).first()
        assertEquals("Your Metal mix", genreMix.title)
        assertEquals("genre_mix_metal_$epochDay", genreMix.id)
    }

    @Test
    fun `pickCandidates caps at two rows with priority genre then playlist`() {
        val library = (1L..10L).map { track(it, folderTag = "metal") } +
            (11L..13L).map { track(it, title = "Sibling $it") }
        val picks = MixDimensionEngine.pickCandidates(
            emptyInputs(
                library = library,
                favorites = setOf(1L),
                coPlaylistBySeed = mapOf(1L to listOf(11L, 12L, 13L))
            ),
            epochDay
        )
        assertEquals(2, picks.size)
        assertEquals(RecommendationRowType.GENRE_MIX, picks[0].type)
        assertEquals(RecommendationRowType.PLAYLIST_AFFINITY, picks[1].type)
    }

    @Test
    fun `playlist affinity omitted when seed has no siblings`() {
        val library = (1L..10L).map { track(it, folderTag = "metal") }
        val picks = MixDimensionEngine.pickCandidates(
            emptyInputs(library = library, favorites = setOf(1L)),
            epochDay
        )
        assertEquals(1, picks.size)
        assertEquals(RecommendationRowType.GENRE_MIX, picks[0].type)
    }

    @Test
    fun `language mix omitted when no language metadata`() {
        val library = (1L..10L).map { track(it, folderTag = "metal") }
        val picks = MixDimensionEngine.pickCandidates(emptyInputs(library), epochDay)
        assertTrue(picks.none { it.type == RecommendationRowType.LANGUAGE_MIX })
    }

    @Test
    fun `language mix appears when enough tracks share bucket`() {
        val library = (1L..8L).map { track(it, language = "de") }
        val picks = MixDimensionEngine.pickCandidates(emptyInputs(library), epochDay)
        val languageMix = picks.single()
        assertEquals(RecommendationRowType.LANGUAGE_MIX, languageMix.type)
        assertEquals("Your German mix", languageMix.title)
    }

    @Test
    fun `playlist affinity excludes seed from pool`() {
        val library = listOf(
            track(1, title = "Seed"),
            track(2, title = "A"),
            track(3, title = "B"),
            track(4, title = "C")
        )
        val picks = MixDimensionEngine.pickCandidates(
            emptyInputs(
                library = library,
                favorites = setOf(1L),
                coPlaylistBySeed = mapOf(1L to listOf(2L, 3L, 4L))
            ),
            epochDay
        )
        val playlistMix = picks.first { it.type == RecommendationRowType.PLAYLIST_AFFINITY }
        assertNull(playlistMix.pool.find { it.id == 1L })
        assertEquals(3, playlistMix.pool.size)
    }

    private fun emptyInputs(
        library: List<TrackInfo>,
        favorites: Set<Long> = emptySet(),
        coPlaylistBySeed: Map<Long, List<Long>> = emptyMap()
    ) = RecommendationInputs(
        library = library,
        favorites = favorites,
        topArtists30d = emptyList(),
        topTracks30d = emptyList(),
        recentHistory = emptyList(),
        coOccurrenceBySeed = emptyMap(),
        lastSessionTrackIds = emptyList(),
        userPlaylists = emptyList(),
        coPlaylistBySeed = coPlaylistBySeed
    )

    private fun track(
        id: Long,
        title: String = "Song $id",
        genre: String? = null,
        folderTag: String? = null,
        language: String? = null
    ) = TrackInfo(
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = title,
        artist = "Artist",
        duration = 180_000L,
        genre = genre,
        folderTag = folderTag,
        language = language
    )
}
