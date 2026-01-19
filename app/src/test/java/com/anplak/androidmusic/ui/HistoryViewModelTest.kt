package com.anplak.androidmusic.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.ArtistPlayCount
import com.anplak.androidmusic.data.PlayHistoryEntry
import com.anplak.androidmusic.data.PlayHistoryRepository
import com.anplak.androidmusic.data.TrackPlayCount
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeRepository: TestPlayHistoryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeRepository = TestPlayHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        val viewModel = createViewModel()

        assertEquals(HistoryUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `emits Empty state when no history`() = runTest {
        fakeRepository.setHistory(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HistoryUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `emits Content state with history entries`() = runTest {
        val entries = listOf(
            createHistoryEntry(1, 1, 3000L),
            createHistoryEntry(2, 2, 2000L)
        )
        fakeRepository.setHistory(entries)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Content)
        assertEquals(2, (state as HistoryUiState.Content).entries.size)
    }

    @Test
    fun `Content state has hasMore true when page is full`() = runTest {
        // Create 50 entries (full page)
        val entries = (1..50).map { createHistoryEntry(it.toLong(), it.toLong(), it * 1000L) }
        fakeRepository.setHistory(entries)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Content
        assertTrue(state.hasMore)
    }

    @Test
    fun `Content state has hasMore false when page is not full`() = runTest {
        // Create 10 entries (less than page size)
        val entries = (1..10).map { createHistoryEntry(it.toLong(), it.toLong(), it * 1000L) }
        fakeRepository.setHistory(entries)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Content
        assertTrue(!state.hasMore)
    }

    @Test
    fun `refresh reloads history`() = runTest {
        val initialEntries = listOf(createHistoryEntry(1, 1, 1000L))
        fakeRepository.setHistory(initialEntries)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val newEntries = listOf(
            createHistoryEntry(2, 2, 2000L),
            createHistoryEntry(1, 1, 1000L)
        )
        fakeRepository.setHistory(newEntries)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Content
        assertEquals(2, state.entries.size)
    }

    @Test
    fun `cleanup runs on init`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(fakeRepository.cleanupCalled)
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(application, fakeRepository)
    }

    private fun createHistoryEntry(id: Long, trackId: Long, playedAt: Long) = PlayHistoryEntry(
        id = id,
        trackId = trackId,
        playedAt = playedAt,
        duration = 60000L,
        sessionId = null,
        track = TrackInfo(
            uri = TrackInfo.uriFromId(trackId),
            title = "Track $trackId",
            artist = "Artist",
            album = "Album",
            duration = 180000L
        )
    )
}

class TestPlayHistoryRepository : PlayHistoryRepository {
    private val history = MutableStateFlow<List<PlayHistoryEntry>>(emptyList())
    private val historyForTrack = MutableStateFlow<List<PlayHistoryEntry>>(emptyList())
    private val historySince = MutableStateFlow<List<PlayHistoryEntry>>(emptyList())
    private val totalPlayTime = MutableStateFlow(0L)
    private val topTracks = MutableStateFlow<List<TrackPlayCount>>(emptyList())
    private val topArtists = MutableStateFlow<List<ArtistPlayCount>>(emptyList())
    private var historyCount = 0

    var recordPlayCalls = 0
        private set
    var lastRecordedTrackId: Long? = null
        private set
    var cleanupCalled = false
        private set

    fun setHistory(entries: List<PlayHistoryEntry>) {
        history.value = entries
    }

    fun setHistoryForTrack(entries: List<PlayHistoryEntry>) {
        historyForTrack.value = entries
    }

    fun setHistorySince(entries: List<PlayHistoryEntry>) {
        historySince.value = entries
    }

    fun setTotalPlayTime(time: Long) {
        totalPlayTime.value = time
    }

    fun setTopTracks(tracks: List<TrackPlayCount>) {
        topTracks.value = tracks
    }

    fun setTopArtists(artists: List<ArtistPlayCount>) {
        topArtists.value = artists
    }

    fun setHistoryCount(count: Int) {
        historyCount = count
    }

    override suspend fun recordPlay(trackId: Long, sessionId: String?): Long {
        recordPlayCalls++
        lastRecordedTrackId = trackId
        return 1L
    }

    override suspend fun updateDuration(historyId: Long, duration: Long) {}

    override fun getHistory(limit: Int, offset: Int): Flow<List<PlayHistoryEntry>> = history

    override fun getHistoryForTrack(trackId: Long, limit: Int): Flow<List<PlayHistoryEntry>> = historyForTrack

    override fun getHistorySince(timestamp: Long): Flow<List<PlayHistoryEntry>> = historySince

    override fun getTotalPlayTimeSince(timestamp: Long): Flow<Long> = totalPlayTime

    override fun getTopTracksSince(timestamp: Long, limit: Int): Flow<List<TrackPlayCount>> = topTracks

    override fun getTopArtistsSince(timestamp: Long, limit: Int): Flow<List<ArtistPlayCount>> = topArtists

    override suspend fun getHistoryCount(): Int = historyCount

    override suspend fun cleanupOldHistory(retentionDays: Int): Int {
        cleanupCalled = true
        return 0
    }
}
