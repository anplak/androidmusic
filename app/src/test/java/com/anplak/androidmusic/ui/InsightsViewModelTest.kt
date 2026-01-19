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
        fakeRepository.setTotalPlayTime(60000L, 120000L)
        fakeRepository.setTopTracks(emptyList(), emptyList())
        fakeRepository.setTopArtists(emptyList(), emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasData)
        assertEquals(60000L, state.todayPlayTime)
        assertEquals(120000L, state.weekPlayTime)
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
        fakeRepository.setTotalPlayTime(60000L, 120000L)
        fakeRepository.setTopTracks(emptyList(), emptyList())
        fakeRepository.setTopArtists(emptyList(), emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(60000L, viewModel.uiState.value.todayPlayTime)

        fakeRepository.setTotalPlayTime(90000L, 180000L)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(90000L, viewModel.uiState.value.todayPlayTime)
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
    private val todayPlayTime = MutableStateFlow(0L)
    private val weekPlayTime = MutableStateFlow(0L)
    private val todayTopTracks = MutableStateFlow<List<TrackPlayCount>>(emptyList())
    private val weekTopTracks = MutableStateFlow<List<TrackPlayCount>>(emptyList())
    private val todayTopArtists = MutableStateFlow<List<ArtistPlayCount>>(emptyList())
    private val weekTopArtists = MutableStateFlow<List<ArtistPlayCount>>(emptyList())

    fun setTotalPlayTime(today: Long, week: Long) {
        todayPlayTime.value = today
        weekPlayTime.value = week
    }

    fun setTopTracks(today: List<TrackPlayCount>, week: List<TrackPlayCount>) {
        todayTopTracks.value = today
        weekTopTracks.value = week
    }

    fun setTopArtists(today: List<ArtistPlayCount>, week: List<ArtistPlayCount>) {
        todayTopArtists.value = today
        weekTopArtists.value = week
    }

    override suspend fun recordPlay(trackId: Long, sessionId: String?): Long = 1L

    override suspend fun updateDuration(historyId: Long, duration: Long) {}

    override fun getHistory(limit: Int, offset: Int): Flow<List<PlayHistoryEntry>> = history

    override fun getHistoryForTrack(trackId: Long, limit: Int): Flow<List<PlayHistoryEntry>> = historyForTrack

    override fun getHistorySince(timestamp: Long): Flow<List<PlayHistoryEntry>> = historySince

    override fun getTotalPlayTimeSince(timestamp: Long): Flow<Long> {
        // Distinguish between today and week based on timestamp proximity
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        return if (now - timestamp < dayMs) todayPlayTime else weekPlayTime
    }

    override fun getTopTracksSince(timestamp: Long, limit: Int): Flow<List<TrackPlayCount>> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        return if (now - timestamp < dayMs) todayTopTracks else weekTopTracks
    }

    override fun getTopArtistsSince(timestamp: Long, limit: Int): Flow<List<ArtistPlayCount>> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        return if (now - timestamp < dayMs) todayTopArtists else weekTopArtists
    }

    override suspend fun getHistoryCount(): Int = 0

    override suspend fun cleanupOldHistory(retentionDays: Int): Int = 0
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

    override suspend fun deleteStaleEntries(validIds: List<Long>) {}

    override suspend fun deleteAll() {}
}
