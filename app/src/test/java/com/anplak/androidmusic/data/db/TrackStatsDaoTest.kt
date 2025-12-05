package com.anplak.androidmusic.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TrackStatsDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var trackStatsDao: TrackStatsDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        trackDao = database.trackDao()
        trackStatsDao = database.trackStatsDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `insertIfNotExists creates new stats entry`() = runTest {
        trackDao.insert(createTrackEntity(1))
        
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        
        val stats = trackStatsDao.getStatsForTrack(1)
        assertNotNull(stats)
        assertEquals(1L, stats?.trackId)
        assertEquals(0, stats?.playCount)
    }
    
    @Test
    fun `insertIfNotExists does not overwrite existing stats`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        trackStatsDao.incrementPlayCount(1, 12345L)
        
        // Try to insert again
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        
        val stats = trackStatsDao.getStatsForTrack(1)
        assertEquals(1, stats?.playCount) // Should still be 1
    }
    
    @Test
    fun `incrementPlayCount updates count and timestamp`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        
        val timestamp = System.currentTimeMillis()
        trackStatsDao.incrementPlayCount(1, timestamp)
        
        val stats = trackStatsDao.getStatsForTrack(1)
        assertEquals(1, stats?.playCount)
        assertEquals(timestamp, stats?.lastPlayedAt)
    }
    
    @Test
    fun `incrementPlayCount increments existing count`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        
        trackStatsDao.incrementPlayCount(1)
        trackStatsDao.incrementPlayCount(1)
        trackStatsDao.incrementPlayCount(1)
        
        val stats = trackStatsDao.getStatsForTrack(1)
        assertEquals(3, stats?.playCount)
    }
    
    @Test
    fun `incrementCompletionCount updates completion count`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        
        trackStatsDao.incrementCompletionCount(1)
        trackStatsDao.incrementCompletionCount(1)
        
        val stats = trackStatsDao.getStatsForTrack(1)
        assertEquals(2, stats?.completionCount)
    }
    
    @Test
    fun `getMostPlayedTrackIds returns ordered by playCount desc`() = runTest {
        // Setup tracks
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        trackDao.insert(createTrackEntity(3))
        
        // Add play counts
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 2))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 3))
        
        repeat(5) { trackStatsDao.incrementPlayCount(1) }
        repeat(10) { trackStatsDao.incrementPlayCount(2) }
        repeat(3) { trackStatsDao.incrementPlayCount(3) }
        
        val mostPlayed = trackStatsDao.getMostPlayedTrackIds(10).first()
        
        assertEquals(3, mostPlayed.size)
        assertEquals(2L, mostPlayed[0]) // 10 plays
        assertEquals(1L, mostPlayed[1]) // 5 plays
        assertEquals(3L, mostPlayed[2]) // 3 plays
    }
    
    @Test
    fun `getMostPlayedTrackIds respects limit`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        trackDao.insert(createTrackEntity(3))
        
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 2))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 3))
        
        repeat(5) { trackStatsDao.incrementPlayCount(1) }
        repeat(10) { trackStatsDao.incrementPlayCount(2) }
        repeat(3) { trackStatsDao.incrementPlayCount(3) }
        
        val mostPlayed = trackStatsDao.getMostPlayedTrackIds(2).first()
        
        assertEquals(2, mostPlayed.size)
    }
    
    @Test
    fun `getMostPlayedTrackIds excludes tracks with zero plays`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 2))
        
        trackStatsDao.incrementPlayCount(1)
        // Track 2 has 0 plays
        
        val mostPlayed = trackStatsDao.getMostPlayedTrackIds(10).first()
        
        assertEquals(1, mostPlayed.size)
        assertEquals(1L, mostPlayed[0])
    }
    
    @Test
    fun `getRecentlyPlayedTrackIds returns ordered by lastPlayedAt desc`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        trackDao.insert(createTrackEntity(3))
        
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 2))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 3))
        
        trackStatsDao.incrementPlayCount(1, 1000L)
        trackStatsDao.incrementPlayCount(2, 3000L)
        trackStatsDao.incrementPlayCount(3, 2000L)
        
        val recentlyPlayed = trackStatsDao.getRecentlyPlayedTrackIds(10).first()
        
        assertEquals(3, recentlyPlayed.size)
        assertEquals(2L, recentlyPlayed[0]) // Most recent
        assertEquals(3L, recentlyPlayed[1])
        assertEquals(1L, recentlyPlayed[2]) // Oldest
    }
    
    @Test
    fun `getRecentlyPlayedTrackIds excludes tracks never played`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 2))
        
        trackStatsDao.incrementPlayCount(1, 1000L)
        // Track 2 never played (lastPlayedAt is null)
        
        val recentlyPlayed = trackStatsDao.getRecentlyPlayedTrackIds(10).first()
        
        assertEquals(1, recentlyPlayed.size)
        assertEquals(1L, recentlyPlayed[0])
    }
    
    @Test
    fun `deleting track cascades to stats`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = 1))
        trackStatsDao.incrementPlayCount(1)
        
        assertNotNull(trackStatsDao.getStatsForTrack(1))
        
        trackDao.deleteStaleEntries(emptyList())
        
        assertNull(trackStatsDao.getStatsForTrack(1))
    }
    
    private fun createTrackEntity(id: Long) = TrackEntity(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        duration = 180000L,
        path = "content://media/external/audio/media/$id",
        firstSeenAt = System.currentTimeMillis()
    )
}

