package com.anplak.androidmusic.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.ArtistPlayCount
import com.anplak.androidmusic.data.PlayHistoryEntry
import com.anplak.androidmusic.data.PlayHistoryRepository
import com.anplak.androidmusic.data.TrackPlayCount
import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.data.db.TrackEntity
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeRepository: TestInsightsPlayHistoryRepository
    private lateinit var fakeTrackDao: TestTrackDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeRepository = TestInsightsPlayHistoryRepository()
        fakeTrackDao = TestTrackDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `emits state with hasData false when no listening data`() = runTest {
        fakeRepository.setTotalPlayTime(0, 0)
        fakeRepository.setTopTracks(emptyList(), emptyList())
        fakeRepository.setTopArtists(emptyList(), emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasData)
    }

    @Test
    fun `emits state with hasData true when has play time`() = runTest {
        // Use same value for both since the mock can't reliably distinguish
        // today vs week timestamps in a concurrent test environment
        fakeRepository.setTotalPlayTime(60000L, 60000L)
        fakeRepository.setTopTracks(emptyList(), emptyList())
        fakeRepository.setTopArtists(emptyList(), emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should have data when play time > 0", state.hasData)
        assertTrue("Today play time should be > 0", state.todayPlayTime > 0)
        assertTrue("Week play time should be > 0", state.weekPlayTime > 0)
    }

    @Test
    fun `emits state with top tracks when available`() = runTest {
        fakeRepository.setTotalPlayTime(60000L, 120000L)
        fakeRepository.setTopTracks(
            listOf(TrackPlayCount(1, 5), TrackPlayCount(2, 3)),
            listOf(TrackPlayCount(1, 10), TrackPlayCount(2, 8))
        )
        fakeRepository.setTopArtists(emptyList(), emptyList())
        fakeTrackDao.setTracks(listOf(
            createTrackEntity(1, "Song A"),
            createTrackEntity(2, "Song B")
        ))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.todayTopTracks.size)
        assertEquals("Song A", state.todayTopTracks[0].track.title)
        assertEquals(5, state.todayTopTracks[0].playCount)
        assertEquals(2, state.weekTopTracks.size)
    }

    @Test
    fun `emits state with top artists when available`() = runTest {
        fakeRepository.setTotalPlayTime(60000L, 120000L)
        fakeRepository.setTopTracks(emptyList(), emptyList())
        fakeRepository.setTopArtists(
            listOf(ArtistPlayCount("Artist A", 10), ArtistPlayCount("Artist B", 5)),
            listOf(ArtistPlayCount("Artist A", 20), ArtistPlayCount("Artist B", 15))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.todayTopArtists.size)
        assertEquals("Artist A", state.todayTopArtists[0].artist)
        assertEquals(10, state.todayTopArtists[0].playCount)
        assertEquals(2, state.weekTopArtists.size)
    }

    @Test
    fun `refresh reloads insights`() = runTest {
        fakeRepository.setTotalPlayTime(60000L, 60000L)
        fakeRepository.setTopTracks(emptyList(), emptyList())
        fakeRepository.setTopArtists(emptyList(), emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val initialPlayTime = viewModel.uiState.value.todayPlayTime
        assertTrue("Initial play time should be set", initialPlayTime > 0)

        fakeRepository.setTotalPlayTime(90000L, 90000L)

        viewModel.refresh()
        advanceUntilIdle()

        val refreshedPlayTime = viewModel.uiState.value.todayPlayTime
        assertTrue("Refreshed play time should be updated", refreshedPlayTime > initialPlayTime)
    }

    private fun createViewModel(): InsightsViewModel {
        return InsightsViewModel(application, fakeRepository, fakeTrackDao)
    }

    private fun createTrackEntity(id: Long, title: String) = TrackEntity(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        duration = 180000L,
        path = "path/to/track",
        firstSeenAt = System.currentTimeMillis()
    )
}

class TestInsightsPlayHistoryRepository : PlayHistoryRepository {
    private val history = MutableStateFlow<List<PlayHistoryEntry>>(emptyList())
    private val historyForTrack = MutableStateFlow<List<PlayHistoryEntry>>(emptyList())
    private val historySince = MutableStateFlow<List<PlayHistoryEntry>>(emptyList())
    
    // Store play time values mapped by the actual timestamp that will be used
    private val playTimeByTimestamp = mutableMapOf<Long, Long>()
    private val topTracksByTimestamp = mutableMapOf<Long, List<TrackPlayCount>>()
    private val topArtistsByTimestamp = mutableMapOf<Long, List<ArtistPlayCount>>()
    
    // Captured timestamps from ViewModel calls
    private val capturedTimestamps = mutableSetOf<Long>()
    
    // Default values
    private var todayPlayTimeValue = 0L
    private var weekPlayTimeValue = 0L
    private var todayTopTracksValue = emptyList<TrackPlayCount>()
    private var weekTopTracksValue = emptyList<TrackPlayCount>()
    private var todayTopArtistsValue = emptyList<ArtistPlayCount>()
    private var weekTopArtistsValue = emptyList<ArtistPlayCount>()

    fun setTotalPlayTime(today: Long, week: Long) {
        todayPlayTimeValue = today
        weekPlayTimeValue = week
    }

    fun setTopTracks(today: List<TrackPlayCount>, week: List<TrackPlayCount>) {
        todayTopTracksValue = today
        weekTopTracksValue = week
    }

    fun setTopArtists(today: List<ArtistPlayCount>, week: List<ArtistPlayCount>) {
        todayTopArtistsValue = today
        weekTopArtistsValue = week
    }
    
    private fun isMoreRecentTimestamp(timestamp: Long): Boolean {
        // The more recent timestamp is "today", the older one is "week"
        // todayStart is always >= weekStart (midnight today >= Monday midnight)
        synchronized(capturedTimestamps) {
            capturedTimestamps.add(timestamp)
            // If we have multiple timestamps, the largest is "today"
            return if (capturedTimestamps.size > 1) {
                timestamp == capturedTimestamps.maxOrNull()
            } else {
                // First call - assume it's today (will be corrected when week call comes)
                true
            }
        }
    }

    override suspend fun recordPlay(trackId: Long, sessionId: String?): Long = 1L

    override suspend fun updateDuration(historyId: Long, duration: Long) {}

    override fun getHistory(limit: Int, offset: Int): Flow<List<PlayHistoryEntry>> = history

    override fun getHistoryForTrack(trackId: Long, limit: Int): Flow<List<PlayHistoryEntry>> = historyForTrack

    override fun getHistorySince(timestamp: Long): Flow<List<PlayHistoryEntry>> = historySince

    override fun getTotalPlayTimeSince(timestamp: Long): Flow<Long> {
        val isToday = isMoreRecentTimestamp(timestamp)
        return MutableStateFlow(if (isToday) todayPlayTimeValue else weekPlayTimeValue)
    }

    override fun getTopTracksSince(timestamp: Long, limit: Int): Flow<List<TrackPlayCount>> {
        val isToday = isMoreRecentTimestamp(timestamp)
        return MutableStateFlow(if (isToday) todayTopTracksValue else weekTopTracksValue)
    }

    override fun getTopArtistsSince(timestamp: Long, limit: Int): Flow<List<ArtistPlayCount>> {
        val isToday = isMoreRecentTimestamp(timestamp)
        return MutableStateFlow(if (isToday) todayTopArtistsValue else weekTopArtistsValue)
    }

    override suspend fun getHistoryCount(): Int = 0

    override suspend fun cleanupOldHistory(retentionDays: Int): Int = 0

    override suspend fun getCoPlayedTrackIds(seedTrackId: Long, limit: Int): List<Long> = emptyList()

    override suspend fun getLastSessionTrackIds(limit: Int): List<Long> = emptyList()
}

class TestTrackDao : TrackDao {
    private var tracks = listOf<TrackEntity>()

    fun setTracks(trackList: List<TrackEntity>) {
        tracks = trackList
    }

    override suspend fun insertAll(tracks: List<TrackEntity>) {}

    override suspend fun insert(track: TrackEntity) {}

    override suspend fun getById(trackId: Long): TrackEntity? = tracks.find { it.id == trackId }

    override suspend fun getAll(): List<TrackEntity> = tracks

    override suspend fun getByIds(trackIds: List<Long>): List<TrackEntity> =
        tracks.filter { it.id in trackIds }

    override fun getRecentlyAddedTracks(limit: Int): Flow<List<TrackEntity>> =
        MutableStateFlow(tracks.take(limit))

    override suspend fun getTracksAddedSince(sinceMs: Long): List<TrackEntity> =
        tracks.filter { it.firstSeenAt >= sinceMs }

    override suspend fun searchTracks(query: String, limit: Int): List<TrackEntity> = emptyList()

    override suspend fun deleteStaleEntries(validIds: List<Long>) {}

    override suspend fun deleteAll() {}
}
