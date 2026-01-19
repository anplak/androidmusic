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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayHistoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var playHistoryDao: PlayHistoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        trackDao = database.trackDao()
        playHistoryDao = database.playHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert creates new history entry and returns id`() = runTest {
        trackDao.insert(createTrackEntity(1))

        val historyId = playHistoryDao.insert(
            PlayHistoryEntity(trackId = 1, playedAt = 1000L, duration = 5000L)
        )

        assertTrue(historyId > 0)
        val history = playHistoryDao.getById(historyId)
        assertNotNull(history)
        assertEquals(1L, history?.trackId)
        assertEquals(1000L, history?.playedAt)
        assertEquals(5000L, history?.duration)
    }

    @Test
    fun `insert with sessionId stores session`() = runTest {
        trackDao.insert(createTrackEntity(1))

        val historyId = playHistoryDao.insert(
            PlayHistoryEntity(trackId = 1, playedAt = 1000L, sessionId = "session-123")
        )

        val history = playHistoryDao.getById(historyId)
        assertEquals("session-123", history?.sessionId)
    }

    @Test
    fun `update modifies existing entry`() = runTest {
        trackDao.insert(createTrackEntity(1))
        val historyId = playHistoryDao.insert(
            PlayHistoryEntity(trackId = 1, playedAt = 1000L, duration = 0)
        )

        val original = playHistoryDao.getById(historyId)!!
        playHistoryDao.update(original.copy(duration = 60000L))

        val updated = playHistoryDao.getById(historyId)
        assertEquals(60000L, updated?.duration)
    }

    @Test
    fun `getHistory returns entries in reverse chronological order`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 2, playedAt = 3000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 2000L))

        val history = playHistoryDao.getHistory(10).first()

        assertEquals(3, history.size)
        assertEquals(3000L, history[0].playedAt) // Most recent
        assertEquals(2000L, history[1].playedAt)
        assertEquals(1000L, history[2].playedAt) // Oldest
    }

    @Test
    fun `getHistory respects limit and offset`() = runTest {
        trackDao.insert(createTrackEntity(1))

        repeat(10) { i ->
            playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = (i + 1) * 1000L))
        }

        val firstPage = playHistoryDao.getHistory(3, 0).first()
        val secondPage = playHistoryDao.getHistory(3, 3).first()

        assertEquals(3, firstPage.size)
        assertEquals(3, secondPage.size)
        assertEquals(10000L, firstPage[0].playedAt) // Entry 10 (most recent)
        assertEquals(7000L, secondPage[0].playedAt) // Entry 7
    }

    @Test
    fun `getHistory includes track details`() = runTest {
        trackDao.insert(TrackEntity(
            id = 1,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            duration = 180000L,
            path = "path/to/song",
            firstSeenAt = System.currentTimeMillis()
        ))

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))

        val history = playHistoryDao.getHistory(10).first()

        assertEquals(1, history.size)
        assertEquals("Test Song", history[0].title)
        assertEquals("Test Artist", history[0].artist)
        assertEquals("Test Album", history[0].album)
        assertEquals(180000L, history[0].trackDuration)
    }

    @Test
    fun `getHistoryForTrack returns only entries for specified track`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 2, playedAt = 2000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 3000L))

        val track1History = playHistoryDao.getHistoryForTrack(1, 10).first()

        assertEquals(2, track1History.size)
        assertTrue(track1History.all { it.trackId == 1L })
    }

    @Test
    fun `getHistorySince returns entries after timestamp`() = runTest {
        trackDao.insert(createTrackEntity(1))

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 2000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 3000L))

        val history = playHistoryDao.getHistorySince(2000L).first()

        assertEquals(2, history.size)
        assertTrue(history.all { it.playedAt >= 2000L })
    }

    @Test
    fun `getTotalPlayTimeSince calculates sum of durations`() = runTest {
        trackDao.insert(createTrackEntity(1))

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L, duration = 60000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 2000L, duration = 30000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 3000L, duration = 45000L))

        val totalTime = playHistoryDao.getTotalPlayTimeSince(2000L).first()

        assertEquals(75000L, totalTime) // 30000 + 45000
    }

    @Test
    fun `getTotalPlayTimeSince returns 0 when no entries`() = runTest {
        val totalTime = playHistoryDao.getTotalPlayTimeSince(0L).first()

        assertEquals(0L, totalTime)
    }

    @Test
    fun `getTopTracksSince returns tracks ordered by play count`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        trackDao.insert(createTrackEntity(3))

        // Track 1: 2 plays
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 2000L))
        // Track 2: 3 plays
        playHistoryDao.insert(PlayHistoryEntity(trackId = 2, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 2, playedAt = 2000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 2, playedAt = 3000L))
        // Track 3: 1 play
        playHistoryDao.insert(PlayHistoryEntity(trackId = 3, playedAt = 1000L))

        val topTracks = playHistoryDao.getTopTracksSince(0L, 10).first()

        assertEquals(3, topTracks.size)
        assertEquals(2L, topTracks[0].trackId) // 3 plays
        assertEquals(3, topTracks[0].playCount)
        assertEquals(1L, topTracks[1].trackId) // 2 plays
        assertEquals(2, topTracks[1].playCount)
        assertEquals(3L, topTracks[2].trackId) // 1 play
        assertEquals(1, topTracks[2].playCount)
    }

    @Test
    fun `getTopTracksSince respects limit`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        trackDao.insert(createTrackEntity(3))

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 2, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 3, playedAt = 1000L))

        val topTracks = playHistoryDao.getTopTracksSince(0L, 2).first()

        assertEquals(2, topTracks.size)
    }

    @Test
    fun `getTopArtistsSince returns artists ordered by play count`() = runTest {
        trackDao.insert(createTrackEntity(1, artist = "Artist A"))
        trackDao.insert(createTrackEntity(2, artist = "Artist B"))
        trackDao.insert(createTrackEntity(3, artist = "Artist A"))

        // Artist A: 3 plays (tracks 1 and 3)
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 3, playedAt = 2000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 3000L))
        // Artist B: 1 play
        playHistoryDao.insert(PlayHistoryEntity(trackId = 2, playedAt = 1000L))

        val topArtists = playHistoryDao.getTopArtistsSince(0L, 10).first()

        assertEquals(2, topArtists.size)
        assertEquals("Artist A", topArtists[0].artist)
        assertEquals(3, topArtists[0].playCount)
        assertEquals("Artist B", topArtists[1].artist)
        assertEquals(1, topArtists[1].playCount)
    }

    @Test
    fun `getHistoryCount returns correct count`() = runTest {
        trackDao.insert(createTrackEntity(1))

        assertEquals(0, playHistoryDao.getHistoryCount())

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 2000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 3000L))

        assertEquals(3, playHistoryDao.getHistoryCount())
    }

    @Test
    fun `deleteHistoryOlderThan removes old entries`() = runTest {
        trackDao.insert(createTrackEntity(1))

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 2000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 3000L))

        val deleted = playHistoryDao.deleteHistoryOlderThan(2500L)

        assertEquals(2, deleted)
        assertEquals(1, playHistoryDao.getHistoryCount())
    }

    @Test
    fun `deleteAll removes all entries`() = runTest {
        trackDao.insert(createTrackEntity(1))

        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 2000L))

        playHistoryDao.deleteAll()

        assertEquals(0, playHistoryDao.getHistoryCount())
    }

    @Test
    fun `deleting track cascades to history`() = runTest {
        trackDao.insert(createTrackEntity(1))
        playHistoryDao.insert(PlayHistoryEntity(trackId = 1, playedAt = 1000L))

        assertEquals(1, playHistoryDao.getHistoryCount())

        trackDao.deleteStaleEntries(emptyList())

        assertEquals(0, playHistoryDao.getHistoryCount())
    }

    private fun createTrackEntity(id: Long, artist: String = "Artist") = TrackEntity(
        id = id,
        title = "Track $id",
        artist = artist,
        album = "Album",
        duration = 180000L,
        path = "content://media/external/audio/media/$id",
        firstSeenAt = System.currentTimeMillis()
    )
}
