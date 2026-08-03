package com.anplak.androidmusic.player

import android.net.Uri
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.RankingClock
import com.anplak.androidmusic.data.RankingConfig
import com.anplak.androidmusic.data.TrackStats
import com.anplak.androidmusic.data.TrackStatsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SmartShuffleGeneratorTest {
    private lateinit var fakeFavoritesRepository: FakeFavoritesRepo
    private lateinit var fakeStatsRepository: FakeStatsRepo
    private lateinit var generator: SmartShuffleGenerator
    private val fixedNowMs = 1_700_000_000_000L

    @Before
    fun setup() {
        fakeFavoritesRepository = FakeFavoritesRepo()
        fakeStatsRepository = FakeStatsRepo()
        generator =
            SmartShuffleGenerator(
                fakeFavoritesRepository,
                fakeStatsRepository,
                clock = FixedRankingClock(fixedNowMs),
                random = Random(42),
            )
    }

    @Test
    fun `generateShuffledQueue returns empty list for empty input`() =
        runTest {
            val result = generator.generateShuffledQueue(emptyList())

            assertTrue(result.isEmpty())
        }

    @Test
    fun `generateShuffledQueue returns single item unchanged`() =
        runTest {
            val tracks = listOf(createTrack(1))

            val result = generator.generateShuffledQueue(tracks)

            assertEquals(1, result.size)
            assertEquals(1L, result[0].id)
        }

    @Test
    fun `generateShuffledQueue contains all input tracks exactly once`() =
        runTest {
            val tracks = (1L..10L).map { createTrack(it) }

            val result = generator.generateShuffledQueue(tracks)

            assertEquals(10, result.size)
            assertEquals(tracks.map { it.id }.toSet(), result.map { it.id }.toSet())
        }

    @Test
    fun `recently played tracks are pushed to end of queue`() =
        runTest {
            val tracks = (1L..5L).map { createTrack(it) }
            val recentlyPlayed = setOf(1L, 2L)

            val result = generator.generateShuffledQueue(tracks, recentlyPlayed)

            val lastTwoIds = result.takeLast(2).map { it.id }.toSet()
            assertEquals(recentlyPlayed, lastTwoIds)
        }

    @Test
    fun `favorites have higher selection probability over multiple runs`() =
        runTest {
            fakeFavoritesRepository.setFavoriteTimestamps(mapOf(1L to fixedNowMs))

            val tracks = (1L..10L).map { createTrack(it) }

            var track1FirstCount = 0
            repeat(100) {
                val gen =
                    SmartShuffleGenerator(
                        fakeFavoritesRepository,
                        fakeStatsRepository,
                        clock = FixedRankingClock(fixedNowMs),
                        random = Random(it),
                    )
                val result = gen.generateShuffledQueue(tracks)
                if (result.first().id == 1L) {
                    track1FirstCount++
                }
            }

            assertTrue("Favorite track should appear first more often than random", track1FirstCount > 15)
        }

    @Test
    fun `monte carlo first pick share stays below max for fixture library`() =
        runTest {
            val tracks = (1L..20L).map { createTrack(it) }
            fakeStatsRepository.setAllStats(
                listOf(TrackStats(trackId = 1L, playCount = 100, lastPlayedAt = fixedNowMs, completionCount = 80)),
            )

            val pickCounts = mutableMapOf<Long, Int>()
            repeat(200) {
                val gen =
                    SmartShuffleGenerator(
                        fakeFavoritesRepository,
                        fakeStatsRepository,
                        clock = FixedRankingClock(fixedNowMs),
                        random = Random(it),
                    )
                val firstPick = gen.generateShuffledQueue(tracks).first().id
                pickCounts[firstPick] = pickCounts.getOrDefault(firstPick, 0) + 1
            }

            val maxShare = pickCounts.values.max().toDouble() / 200
            assertTrue(
                "No track should exceed ${RankingConfig.MAX_SHUFFLE_SHARE} share",
                maxShare <= RankingConfig.MAX_SHUFFLE_SHARE,
            )
        }

    private fun createTrack(id: Long) =
        TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = "Track $id",
            artist = "Artist",
            album = "Album",
            duration = 180000L,
        )
}

private class FixedRankingClock(private val nowMs: Long) : RankingClock {
    override fun nowMs(): Long = nowMs
}

private class FakeFavoritesRepo : FavoritesRepository {
    private val favoriteTimestamps = MutableStateFlow<Map<Long, Long>>(emptyMap())
    private val favorites = MutableStateFlow<List<TrackInfo>>(emptyList())

    fun setFavoriteTimestamps(timestamps: Map<Long, Long>) {
        favoriteTimestamps.value = timestamps
    }

    override suspend fun toggleFavorite(track: TrackInfo) {}

    override suspend fun toggleFavorite(trackId: Long) {}

    override fun isFavorite(trackId: Long): Flow<Boolean> = MutableStateFlow(favoriteTimestamps.value.containsKey(trackId))

    override fun getAllFavorites(): Flow<List<TrackInfo>> = favorites

    override fun getAllFavoriteIds(): Flow<Set<Long>> = MutableStateFlow(favoriteTimestamps.value.keys)

    override fun getFavoriteTimestamps(): Flow<Map<Long, Long>> = favoriteTimestamps
}

private class FakeStatsRepo : TrackStatsRepository {
    private var allStats: List<TrackStats> = emptyList()

    fun setAllStats(stats: List<TrackStats>) {
        allStats = stats
    }

    override suspend fun recordQualifiedPlay(
        trackId: Long,
        timestamp: Long,
    ) {}

    override suspend fun recordSkip(trackId: Long) {}

    override suspend fun recordCompletion(trackId: Long) {}

    override suspend fun getStats(trackId: Long): TrackStats? = allStats.find { it.trackId == trackId }

    override fun observeStats(trackId: Long): Flow<TrackStats?> = MutableStateFlow(allStats.find { it.trackId == trackId })

    override suspend fun getAllStatsOrderedByPlayCount(): List<TrackStats> = allStats
}
