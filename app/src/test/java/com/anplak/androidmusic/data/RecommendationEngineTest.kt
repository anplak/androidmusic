package com.anplak.androidmusic.data

import android.net.Uri
import com.anplak.androidmusic.player.AutoMixGenerator
import com.anplak.androidmusic.player.SmartShuffleGenerator
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class RecommendationEngineTest {

    private lateinit var engine: RecommendationEngine
    private val fixedDay = 20_000L

    @Before
    fun setup() {
        val smartShuffle = SmartShuffleGenerator(
            FakeFavoritesRepo(),
            FakeStatsRepo(),
            Random(1)
        )
        engine = RecommendationEngine(
            autoMixGenerator = AutoMixGenerator(smartShuffle),
            clock = RecommendationClock { fixedDay }
        )
    }

    @Test
    fun `buildRows returns empty for empty library`() = runTest {
        val rows = engine.buildRows(emptyInputs(library = emptyList()))
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `buildRows includes daily mix with stable id`() = runTest {
        val library = listOf(
            track(1, "Song A", "Artist One"),
            track(2, "Song B", "Artist One"),
            track(3, "Song C", "Artist Two")
        )
        val rows = engine.buildRows(
            emptyInputs(
                library = library,
                topArtists30d = listOf("Artist One")
            )
        )

        val dailyMix = rows.find { it.type == RecommendationRowType.DAILY_MIX }
        assertNotNull(dailyMix)
        assertEquals("daily_mix_$fixedDay", dailyMix!!.id)
        assertTrue(dailyMix.tracks.isNotEmpty())
    }

    @Test
    fun `buildRows includes because you listen row for top track`() = runTest {
        val seed = track(10, "Hit Song", "Star Artist", album = "Album X")
        val library = listOf(
            seed,
            track(11, "Other", "Star Artist"),
            track(12, "Another", "Other Artist")
        )
        val rows = engine.buildRows(
            emptyInputs(
                library = library,
                topTracks30d = listOf(10L),
                coOccurrenceBySeed = mapOf(10L to listOf(11L))
            )
        )

        val because = rows.filter { it.type == RecommendationRowType.BECAUSE_YOU_LISTEN }
        assertEquals(1, because.size)
        assertTrue(because[0].title.contains("Star Artist"))
        assertTrue(because[0].tracks.any { it.id == 10L })
        assertTrue(because[0].tracks.any { it.id == 11L })
    }

    @Test
    fun `buildRows includes continue listening for last session`() = runTest {
        val library = (1L..4L).map { track(it, "T$it", "Artist") }
        val rows = engine.buildRows(
            emptyInputs(
                library = library,
                lastSessionTrackIds = listOf(1L, 2L, 3L)
            )
        )

        val continueRow = rows.find { it.type == RecommendationRowType.CONTINUE_LISTENING }
        assertNotNull(continueRow)
        assertEquals(3, continueRow!!.tracks.size)
    }

    private fun emptyInputs(
        library: List<TrackInfo> = listOf(track(1, "A", "Artist")),
        favorites: Set<Long> = emptySet(),
        topArtists30d: List<String> = emptyList(),
        topTracks30d: List<Long> = emptyList(),
        coOccurrenceBySeed: Map<Long, List<Long>> = emptyMap(),
        lastSessionTrackIds: List<Long> = emptyList()
    ) = RecommendationInputs(
        library = library,
        favorites = favorites,
        topArtists30d = topArtists30d,
        topTracks30d = topTracks30d,
        recentHistory = emptyList(),
        coOccurrenceBySeed = coOccurrenceBySeed,
        lastSessionTrackIds = lastSessionTrackIds,
        userPlaylists = emptyList()
    )

    private fun track(
        id: Long,
        title: String,
        artist: String,
        album: String = ""
    ) = TrackInfo(
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = title,
        artist = artist,
        album = album,
        duration = 180_000L
    )

    private class FakeFavoritesRepo : com.anplak.androidmusic.data.FavoritesRepository {
        override suspend fun toggleFavorite(trackId: Long) {}
        override fun isFavorite(trackId: Long) = kotlinx.coroutines.flow.flowOf(false)
        override fun getAllFavorites() = kotlinx.coroutines.flow.flowOf(emptyList<TrackInfo>())
        override fun getAllFavoriteIds() = kotlinx.coroutines.flow.flowOf(emptySet<Long>())
    }

    private class FakeStatsRepo : com.anplak.androidmusic.data.TrackStatsRepository {
        override suspend fun recordPlay(trackId: Long) {}
        override suspend fun recordCompletion(trackId: Long) {}
        override suspend fun getStats(trackId: Long): TrackStats? =
            TrackStats(trackId, 0, null, 0)
        override fun observeStats(trackId: Long) =
            kotlinx.coroutines.flow.flowOf(TrackStats(trackId, 0, null, 0))
        override suspend fun getAllStatsOrderedByPlayCount(): List<TrackStats> = emptyList()
    }
}
