package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.TrackStatsDao
import com.anplak.androidmusic.data.db.TrackStatsEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TrackStatsRepositoryTest {
    
    private lateinit var fakeTrackStatsDao: FakeTrackStatsDao
    private lateinit var repository: TrackStatsRepository
    
    @Before
    fun setup() {
        fakeTrackStatsDao = FakeTrackStatsDao()
        repository = TrackStatsRepositoryImpl(fakeTrackStatsDao)
    }
    
    @Test
    fun `recordPlay inserts if not exists and increments count`() = runTest {
        repository.recordPlay(1L)
        
        assertEquals(1, fakeTrackStatsDao.insertIfNotExistsCalls)
        assertEquals(1, fakeTrackStatsDao.incrementPlayCountCalls)
        assertEquals(1L, fakeTrackStatsDao.lastIncrementedTrackId)
    }
    
    @Test
    fun `recordCompletion increments completion count`() = runTest {
        repository.recordCompletion(1L)
        
        assertEquals(1, fakeTrackStatsDao.incrementCompletionCountCalls)
        assertEquals(1L, fakeTrackStatsDao.lastCompletionTrackId)
    }
    
    @Test
    fun `getStats returns null when no stats exist`() = runTest {
        fakeTrackStatsDao.setStatsForTrack(null)
        
        val result = repository.getStats(1L)
        
        assertNull(result)
    }
    
    @Test
    fun `getStats returns TrackStats when stats exist`() = runTest {
        val entity = TrackStatsEntity(
            trackId = 1L,
            playCount = 10,
            lastPlayedAt = 12345L,
            completionCount = 8
        )
        fakeTrackStatsDao.setStatsForTrack(entity)
        
        val result = repository.getStats(1L)
        
        assertNotNull(result)
        assertEquals(1L, result?.trackId)
        assertEquals(10, result?.playCount)
        assertEquals(12345L, result?.lastPlayedAt)
        assertEquals(8, result?.completionCount)
    }
    
    @Test
    fun `observeStats emits TrackStats updates`() = runTest {
        val entity = TrackStatsEntity(
            trackId = 1L,
            playCount = 5,
            lastPlayedAt = 1000L,
            completionCount = 3
        )
        fakeTrackStatsDao.setObservableStats(entity)
        
        val result = repository.observeStats(1L).first()
        
        assertNotNull(result)
        assertEquals(5, result?.playCount)
    }
    
    @Test
    fun `getAllStatsOrderedByPlayCount returns converted stats`() = runTest {
        val entities = listOf(
            TrackStatsEntity(1L, playCount = 10, lastPlayedAt = 1000L, completionCount = 8),
            TrackStatsEntity(2L, playCount = 5, lastPlayedAt = 2000L, completionCount = 4)
        )
        fakeTrackStatsDao.setAllStats(entities)
        
        val result = repository.getAllStatsOrderedByPlayCount()
        
        assertEquals(2, result.size)
        assertEquals(10, result[0].playCount)
        assertEquals(5, result[1].playCount)
    }
    
    @Test
    fun `TrackStats completionRatio calculates correctly`() {
        val stats = TrackStats(
            trackId = 1L,
            playCount = 10,
            lastPlayedAt = 1000L,
            completionCount = 8
        )
        
        assertEquals(0.8f, stats.completionRatio, 0.001f)
    }
    
    @Test
    fun `TrackStats completionRatio returns zero when no plays`() {
        val stats = TrackStats(
            trackId = 1L,
            playCount = 0,
            lastPlayedAt = null,
            completionCount = 0
        )
        
        assertEquals(0f, stats.completionRatio, 0.001f)
    }
}

class FakeTrackStatsDao : TrackStatsDao {
    private var statsForTrack: TrackStatsEntity? = null
    private val observableStats = MutableStateFlow<TrackStatsEntity?>(null)
    private var allStats: List<TrackStatsEntity> = emptyList()
    
    var insertIfNotExistsCalls = 0
        private set
    var incrementPlayCountCalls = 0
        private set
    var lastIncrementedTrackId: Long? = null
        private set
    var incrementCompletionCountCalls = 0
        private set
    var lastCompletionTrackId: Long? = null
        private set
    
    fun setStatsForTrack(stats: TrackStatsEntity?) {
        statsForTrack = stats
    }
    
    fun setObservableStats(stats: TrackStatsEntity?) {
        observableStats.value = stats
    }
    
    fun setAllStats(stats: List<TrackStatsEntity>) {
        allStats = stats
    }
    
    override suspend fun insertIfNotExists(stats: TrackStatsEntity) {
        insertIfNotExistsCalls++
    }
    
    override suspend fun incrementPlayCount(trackId: Long, timestamp: Long) {
        incrementPlayCountCalls++
        lastIncrementedTrackId = trackId
    }
    
    override suspend fun incrementCompletionCount(trackId: Long) {
        incrementCompletionCountCalls++
        lastCompletionTrackId = trackId
    }
    
    override suspend fun getStatsForTrack(trackId: Long): TrackStatsEntity? = statsForTrack
    
    override fun observeStatsForTrack(trackId: Long): Flow<TrackStatsEntity?> = observableStats
    
    override fun getMostPlayedTrackIds(limit: Int): Flow<List<Long>> = 
        MutableStateFlow(allStats.take(limit).map { it.trackId })
    
    override fun getRecentlyPlayedTrackIds(limit: Int): Flow<List<Long>> =
        MutableStateFlow(allStats.take(limit).map { it.trackId })
    
    override suspend fun getAllStatsOrderedByPlayCount(): List<TrackStatsEntity> = allStats
    
    override suspend fun deleteStats(trackId: Long) {}
    
    override suspend fun deleteAll() {}
}

