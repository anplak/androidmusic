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
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class RecommendationEngineTest {

    private lateinit var engine: RecommendationEngine
    private val fixedDay = 20_000L
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setup() {
        engine = createEngine()
    }

    private fun createEngine(): RecommendationEngine {
        val smartShuffle = SmartShuffleGenerator(
            FakeFavoritesRepo(),
            FakeStatsRepo(),
            random = Random(1)
        )
        return RecommendationEngine(
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
    fun `buildRows includes daily mixes with stable slot ids`() = runTest {
        val library = richLibrary()
        val inputs = emptyInputs(
            library = library,
            topArtists30d = listOf("Top Artist A", "Recent Artist", "Decade Artist")
        )
        val rowsFirst = createEngine().buildRows(inputs)
        val rowsSecond = createEngine().buildRows(inputs)

        val dailyMixesFirst = rowsFirst.filter { it.type == RecommendationRowType.DAILY_MIX }
        val dailyMixesSecond = rowsSecond.filter { it.type == RecommendationRowType.DAILY_MIX }
        assertEquals(3, dailyMixesFirst.size)
        assertEquals(dailyMixesFirst.map { it.id }, dailyMixesSecond.map { it.id })
        assertEquals(dailyMixesFirst.map { it.title }, dailyMixesSecond.map { it.title })
        assertEquals(dailyMixesFirst.map { it.subtitle }, dailyMixesSecond.map { it.subtitle })
        assertEquals(
            dailyMixesFirst.map { row -> row.tracks.map { it.id }.toSet() },
            dailyMixesSecond.map { row -> row.tracks.map { it.id }.toSet() }
        )
        assertEquals(
            setOf("Daily Mix 1", "Daily Mix 2", "Daily Mix 3"),
            dailyMixesFirst.map { it.title }.toSet()
        )
        dailyMixesFirst.forEach { row ->
            assertTrue(row.id.startsWith("daily_mix_"))
            assertTrue(row.id.endsWith("_$fixedDay"))
            assertTrue(row.tracks.isNotEmpty())
        }
    }

    @Test
    fun `buildRows daily mixes avoid track overlap when possible`() = runTest {
        val library = richLibrary()
        val rows = engine.buildRows(
            emptyInputs(
                library = library,
                topArtists30d = listOf("Top Artist A", "Recent Artist", "Decade Artist")
            )
        )
        val dailyMixes = rows.filter { it.type == RecommendationRowType.DAILY_MIX }
        if (dailyMixes.size >= 2) {
            val firstIds = dailyMixes[0].tracks.map { it.id }.toSet()
            val secondIds = dailyMixes[1].tracks.map { it.id }.toSet()
            assertTrue(firstIds.intersect(secondIds).isEmpty())
        }
    }

    @Test
    fun `buildRows includes at least one daily mix for sparse metadata library`() = runTest {
        val library = (1L..12L).map { track(it, "Song $it", "Artist One") }
        val rows = engine.buildRows(
            emptyInputs(
                library = library,
                topArtists30d = listOf("Artist One")
            )
        )

        val dailyMixes = rows.filter { it.type == RecommendationRowType.DAILY_MIX }
        assertEquals(1, dailyMixes.size)
        assertEquals("daily_mix_0_$fixedDay", dailyMixes.first().id)
        assertTrue(dailyMixes.first().tracks.isNotEmpty())
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

    private fun richLibrary(): List<TrackInfo> {
        val decadeTracks = (1L..12L).map { track(it, "Decade $it", "Decade Artist", year = 1985) }
        val recentTracks = (13L..24L).map {
            track(
                id = it,
                title = "Recent $it",
                artist = "Recent Artist",
                year = 2020,
                dateAddedSec = monthStartEpochSecond(fixedDay)
            )
        }
        val artistTracks = (25L..36L).map {
            track(it, "Artist track $it", "Top Artist A", year = 1995)
        }
        return decadeTracks + recentTracks + artistTracks
    }

    private fun monthStartEpochSecond(epochDay: Long): Long =
        LocalDate.ofEpochDay(DailyMixConfig.monthStartEpochDay(epochDay))
            .atStartOfDay(zoneId)
            .toEpochSecond()

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
        album: String = "",
        year: Int? = null,
        dateAddedSec: Long? = null
    ) = TrackInfo(
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = title,
        artist = artist,
        album = album,
        duration = 180_000L,
        year = year,
        dateAddedSec = dateAddedSec
    )

    private class FakeFavoritesRepo : FavoritesRepository {
        override suspend fun toggleFavorite(track: TrackInfo) {}
        override suspend fun toggleFavorite(trackId: Long) {}
        override fun isFavorite(trackId: Long) = kotlinx.coroutines.flow.flowOf(false)
        override fun getAllFavorites() = kotlinx.coroutines.flow.flowOf(emptyList<TrackInfo>())
        override fun getAllFavoriteIds() = kotlinx.coroutines.flow.flowOf(emptySet<Long>())
        override fun getFavoriteTimestamps() = kotlinx.coroutines.flow.flowOf(emptyMap<Long, Long>())
    }

    private class FakeStatsRepo : TrackStatsRepository {
        override suspend fun recordQualifiedPlay(trackId: Long, timestamp: Long) {}
        override suspend fun recordSkip(trackId: Long) {}
        override suspend fun recordCompletion(trackId: Long) {}
        override suspend fun getStats(trackId: Long): TrackStats? =
            TrackStats(trackId, 0, null, 0, 0)
        override fun observeStats(trackId: Long) =
            kotlinx.coroutines.flow.flowOf(TrackStats(trackId, 0, null, 0, 0))
        override suspend fun getAllStatsOrderedByPlayCount(): List<TrackStats> = emptyList()
    }
}
