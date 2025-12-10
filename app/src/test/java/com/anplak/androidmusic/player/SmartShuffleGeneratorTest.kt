package com.anplak.androidmusic.player

import android.net.Uri
import com.anplak.androidmusic.data.FavoritesRepository
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
    
    @Before
    fun setup() {
        fakeFavoritesRepository = FakeFavoritesRepo()
        fakeStatsRepository = FakeStatsRepo()
        // Use a fixed seed for deterministic tests
        generator = SmartShuffleGenerator(
            fakeFavoritesRepository,
            fakeStatsRepository,
            Random(42)
        )
    }
    
    @Test
    fun `generateShuffledQueue returns empty list for empty input`() = runTest {
        val result = generator.generateShuffledQueue(emptyList())
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `generateShuffledQueue returns single item unchanged`() = runTest {
        val tracks = listOf(createTrack(1))
        
        val result = generator.generateShuffledQueue(tracks)
        
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }
    
    @Test
    fun `generateShuffledQueue contains all input tracks exactly once`() = runTest {
        val tracks = (1L..10L).map { createTrack(it) }
        
        val result = generator.generateShuffledQueue(tracks)
        
        assertEquals(10, result.size)
        assertEquals(tracks.map { it.id }.toSet(), result.map { it.id }.toSet())
    }
    
    @Test
    fun `recently played tracks are pushed to end of queue`() = runTest {
        val tracks = (1L..5L).map { createTrack(it) }
        val recentlyPlayed = setOf(1L, 2L)
        
        val result = generator.generateShuffledQueue(tracks, recentlyPlayed)
        
        // Recently played tracks (1 and 2) should be in the last 2 positions
        val lastTwoIds = result.takeLast(2).map { it.id }.toSet()
        assertEquals(recentlyPlayed, lastTwoIds)
    }
    
    @Test
    fun `calculateWeight returns base weight for non-favorite non-high-playcount track`() {
        val weight = generator.calculateWeight(
            trackId = 1L,
            favoriteIds = emptySet(),
            highPlayCountIds = emptySet()
        )
        
        assertEquals(1.0, weight, 0.001)
    }
    
    @Test
    fun `calculateWeight returns 3x weight for favorite track`() {
        val weight = generator.calculateWeight(
            trackId = 1L,
            favoriteIds = setOf(1L),
            highPlayCountIds = emptySet()
        )
        
        assertEquals(3.0, weight, 0.001)
    }
    
    @Test
    fun `calculateWeight returns 2x weight for high playcount track`() {
        val weight = generator.calculateWeight(
            trackId = 1L,
            favoriteIds = emptySet(),
            highPlayCountIds = setOf(1L)
        )
        
        assertEquals(2.0, weight, 0.001)
    }
    
    @Test
    fun `calculateWeight returns 6x weight for favorite high playcount track`() {
        val weight = generator.calculateWeight(
            trackId = 1L,
            favoriteIds = setOf(1L),
            highPlayCountIds = setOf(1L)
        )
        
        assertEquals(6.0, weight, 0.001)
    }
    
    @Test
    fun `calculateHighPlayCountThreshold returns max for empty list`() {
        val threshold = generator.calculateHighPlayCountThreshold(emptyList())
        
        assertEquals(Int.MAX_VALUE, threshold)
    }
    
    @Test
    fun `calculateHighPlayCountThreshold returns value for single item`() {
        val threshold = generator.calculateHighPlayCountThreshold(listOf(10))
        
        assertEquals(10, threshold)
    }
    
    @Test
    fun `calculateHighPlayCountThreshold returns top 20 percent value`() {
        // 10 items, top 20% = top 2 items
        val playCounts = listOf(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)
        
        val threshold = generator.calculateHighPlayCountThreshold(playCounts)
        
        // Should be the 2nd highest value (index 1 in sorted desc)
        assertEquals(90, threshold)
    }
    
    @Test
    fun `favorites have higher selection probability over multiple runs`() = runTest {
        fakeFavoritesRepository.setFavoriteIds(setOf(1L))
        
        val tracks = (1L..10L).map { createTrack(it) }
        
        // Run multiple times and count how often track 1 appears first
        var track1FirstCount = 0
        repeat(100) {
            // Create new generator with different seed each time
            val gen = SmartShuffleGenerator(
                fakeFavoritesRepository,
                fakeStatsRepository,
                Random(it)
            )
            val result = gen.generateShuffledQueue(tracks)
            if (result.first().id == 1L) {
                track1FirstCount++
            }
        }
        
        // Favorite track should appear first more often than random (1/10 = 10%)
        // With 3x weight, it should be significantly higher
        assertTrue("Favorite track should appear first more often than random", track1FirstCount > 15)
    }
    
    private fun createTrack(id: Long) = TrackInfo(
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        duration = 180000L
    )
}

private class FakeFavoritesRepo : FavoritesRepository {
    private val favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val favorites = MutableStateFlow<List<TrackInfo>>(emptyList())
    
    fun setFavoriteIds(ids: Set<Long>) {
        favoriteIds.value = ids
    }
    
    override suspend fun toggleFavorite(trackId: Long) {}
    
    override fun isFavorite(trackId: Long): Flow<Boolean> = 
        MutableStateFlow(favoriteIds.value.contains(trackId))
    
    override fun getAllFavorites(): Flow<List<TrackInfo>> = favorites
    
    override fun getAllFavoriteIds(): Flow<Set<Long>> = favoriteIds
}

private class FakeStatsRepo : TrackStatsRepository {
    private var allStats: List<TrackStats> = emptyList()
    
    fun setAllStats(stats: List<TrackStats>) {
        allStats = stats
    }
    
    override suspend fun recordPlay(trackId: Long) {}
    
    override suspend fun recordCompletion(trackId: Long) {}
    
    override suspend fun getStats(trackId: Long): TrackStats? = 
        allStats.find { it.trackId == trackId }
    
    override fun observeStats(trackId: Long): Flow<TrackStats?> = 
        MutableStateFlow(allStats.find { it.trackId == trackId })
    
    override suspend fun getAllStatsOrderedByPlayCount(): List<TrackStats> = allStats
}

