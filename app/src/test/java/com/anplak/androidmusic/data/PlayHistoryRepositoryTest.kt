package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.ArtistPlayCountResult
import com.anplak.androidmusic.data.db.PlayHistoryDao
import com.anplak.androidmusic.data.db.PlayHistoryEntity
import com.anplak.androidmusic.data.db.PlayHistoryWithTrack
import com.anplak.androidmusic.data.db.TrackPlayCountResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
class PlayHistoryRepositoryTest {

    private lateinit var fakePlayHistoryDao: FakePlayHistoryDao
    private lateinit var repository: PlayHistoryRepository

    @Before
    fun setup() {
        fakePlayHistoryDao = FakePlayHistoryDao()
        repository = PlayHistoryRepositoryImpl(fakePlayHistoryDao)
    }

    @Test
    fun `recordPlay inserts history entry and returns id`() = runTest {
        val id = repository.recordPlay(1L, "session-123")

        assertEquals(1L, id)
        assertEquals(1, fakePlayHistoryDao.insertCalls)
        assertEquals(1L, fakePlayHistoryDao.lastInsertedEntity?.trackId)
        assertEquals("session-123", fakePlayHistoryDao.lastInsertedEntity?.sessionId)
    }

    @Test
    fun `recordPlay without session inserts entry with null sessionId`() = runTest {
        repository.recordPlay(1L)

        assertNull(fakePlayHistoryDao.lastInsertedEntity?.sessionId)
    }

    @Test
    fun `updateDuration updates existing entry`() = runTest {
        fakePlayHistoryDao.setEntityById(PlayHistoryEntity(id = 1, trackId = 1, playedAt = 1000L))

        repository.updateDuration(1L, 60000L)

        assertEquals(1, fakePlayHistoryDao.updateCalls)
        assertEquals(60000L, fakePlayHistoryDao.lastUpdatedEntity?.duration)
    }

    @Test
    fun `updateDuration does nothing for non-existent entry`() = runTest {
        fakePlayHistoryDao.setEntityById(null)

        repository.updateDuration(1L, 60000L)

        assertEquals(0, fakePlayHistoryDao.updateCalls)
    }

    @Test
    fun `getHistory returns converted entries`() = runTest {
        val entries = listOf(
            createPlayHistoryWithTrack(1, 1, 3000L),
            createPlayHistoryWithTrack(2, 2, 2000L)
        )
        fakePlayHistoryDao.setHistory(entries)

        val result = repository.getHistory(10).first()

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(1L, result[0].trackId)
        assertEquals("Track 1", result[0].track.title)
    }

    @Test
    fun `getHistoryForTrack returns filtered entries`() = runTest {
        val entries = listOf(
            createPlayHistoryWithTrack(1, 1, 2000L),
            createPlayHistoryWithTrack(2, 1, 1000L)
        )
        fakePlayHistoryDao.setHistoryForTrack(entries)

        val result = repository.getHistoryForTrack(1L).first()

        assertEquals(2, result.size)
    }

    @Test
    fun `getTotalPlayTimeSince returns time from DAO`() = runTest {
        fakePlayHistoryDao.setTotalPlayTime(120000L)

        val result = repository.getTotalPlayTimeSince(0L).first()

        assertEquals(120000L, result)
    }

    @Test
    fun `getTopTracksSince returns converted results`() = runTest {
        val results = listOf(
            TrackPlayCountResult(1L, 10),
            TrackPlayCountResult(2L, 5)
        )
        fakePlayHistoryDao.setTopTracks(results)

        val topTracks = repository.getTopTracksSince(0L, 10).first()

        assertEquals(2, topTracks.size)
        assertEquals(1L, topTracks[0].trackId)
        assertEquals(10, topTracks[0].playCount)
    }

    @Test
    fun `getTopArtistsSince returns converted results`() = runTest {
        val results = listOf(
            ArtistPlayCountResult("Artist A", 15),
            ArtistPlayCountResult("Artist B", 8)
        )
        fakePlayHistoryDao.setTopArtists(results)

        val topArtists = repository.getTopArtistsSince(0L, 10).first()

        assertEquals(2, topArtists.size)
        assertEquals("Artist A", topArtists[0].artist)
        assertEquals(15, topArtists[0].playCount)
    }

    @Test
    fun `getHistoryCount returns count from DAO`() = runTest {
        fakePlayHistoryDao.setHistoryCount(42)

        val count = repository.getHistoryCount()

        assertEquals(42, count)
    }

    @Test
    fun `cleanupOldHistory deletes entries and returns count`() = runTest {
        fakePlayHistoryDao.setDeletedCount(5)

        val deleted = repository.cleanupOldHistory(90)

        assertEquals(5, deleted)
        assertEquals(1, fakePlayHistoryDao.deleteOlderThanCalls)
    }

    private fun createPlayHistoryWithTrack(
        id: Long,
        trackId: Long,
        playedAt: Long
    ) = PlayHistoryWithTrack(
        id = id,
        trackId = trackId,
        playedAt = playedAt,
        duration = 60000L,
        sessionId = null,
        title = "Track $trackId",
        artist = "Artist",
        album = "Album",
        trackDuration = 180000L
    )
}

class FakePlayHistoryDao : PlayHistoryDao {
    private var nextInsertId = 1L
    private var entityById: PlayHistoryEntity? = null
    private val history = MutableStateFlow<List<PlayHistoryWithTrack>>(emptyList())
    private val historyForTrack = MutableStateFlow<List<PlayHistoryWithTrack>>(emptyList())
    private val historySince = MutableStateFlow<List<PlayHistoryWithTrack>>(emptyList())
    private val totalPlayTime = MutableStateFlow(0L)
    private val topTracks = MutableStateFlow<List<TrackPlayCountResult>>(emptyList())
    private val topArtists = MutableStateFlow<List<ArtistPlayCountResult>>(emptyList())
    private var historyCount = 0
    private var deletedCount = 0

    var insertCalls = 0
        private set
    var lastInsertedEntity: PlayHistoryEntity? = null
        private set
    var updateCalls = 0
        private set
    var lastUpdatedEntity: PlayHistoryEntity? = null
        private set
    var deleteOlderThanCalls = 0
        private set

    fun setEntityById(entity: PlayHistoryEntity?) {
        entityById = entity
    }

    fun setHistory(entries: List<PlayHistoryWithTrack>) {
        history.value = entries
    }

    fun setHistoryForTrack(entries: List<PlayHistoryWithTrack>) {
        historyForTrack.value = entries
    }

    fun setHistorySince(entries: List<PlayHistoryWithTrack>) {
        historySince.value = entries
    }

    fun setTotalPlayTime(time: Long) {
        totalPlayTime.value = time
    }

    fun setTopTracks(results: List<TrackPlayCountResult>) {
        topTracks.value = results
    }

    fun setTopArtists(results: List<ArtistPlayCountResult>) {
        topArtists.value = results
    }

    fun setHistoryCount(count: Int) {
        historyCount = count
    }

    fun setDeletedCount(count: Int) {
        deletedCount = count
    }

    override suspend fun insert(history: PlayHistoryEntity): Long {
        insertCalls++
        lastInsertedEntity = history
        return nextInsertId++
    }

    override suspend fun update(history: PlayHistoryEntity) {
        updateCalls++
        lastUpdatedEntity = history
    }

    override suspend fun getById(id: Long): PlayHistoryEntity? = entityById

    override fun getHistory(limit: Int, offset: Int): Flow<List<PlayHistoryWithTrack>> = history

    override fun getHistoryForTrack(trackId: Long, limit: Int): Flow<List<PlayHistoryWithTrack>> = historyForTrack

    override fun getHistorySince(timestamp: Long): Flow<List<PlayHistoryWithTrack>> = historySince

    override fun getTotalPlayTimeSince(timestamp: Long): Flow<Long> = totalPlayTime

    override fun getTopTracksSince(timestamp: Long, limit: Int): Flow<List<TrackPlayCountResult>> = topTracks

    override fun getTopArtistsSince(timestamp: Long, limit: Int): Flow<List<ArtistPlayCountResult>> = topArtists

    override suspend fun getHistoryCount(): Int = historyCount

    override suspend fun deleteHistoryOlderThan(timestamp: Long): Int {
        deleteOlderThanCalls++
        return deletedCount
    }

    override suspend fun deleteAll() {
        history.value = emptyList()
    }

    override suspend fun getCoPlayedTrackIds(seedTrackId: Long, limit: Int): List<TrackPlayCountResult> =
        emptyList()

    override suspend fun getLastSessionTrackIds(limit: Int): List<Long> = emptyList()
}
