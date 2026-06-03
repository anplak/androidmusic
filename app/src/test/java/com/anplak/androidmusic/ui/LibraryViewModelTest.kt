package com.anplak.androidmusic.ui

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.DurationBucket
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.LibraryFilter
import com.anplak.androidmusic.data.LibraryScanResult
import com.anplak.androidmusic.data.MusicLibraryRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LibraryViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeRepository: FakeMusicLibraryRepository
    private lateinit var fakeFavoritesRepository: FakeFavoritesRepository
    private lateinit var fakeTrackDao: FakeTrackDao
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeMusicLibraryRepository()
        fakeFavoritesRepository = FakeFavoritesRepository()
        fakeTrackDao = FakeTrackDao()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is Loading`() = runTest {
        val viewModel = createViewModel()
        
        assertEquals(LibraryUiState.Loading, viewModel.uiState.value)
    }
    
    @Test
    fun `emits Content state with tracks after successful load`() = runTest {
        val tracks = listOf(
            createTrack(1, "Song One", "Artist A"),
            createTrack(2, "Song Two", "Artist B")
        )
        fakeRepository.setTracks(tracks)
        
        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is LibraryUiState.Content)
        assertEquals(2, (state as LibraryUiState.Content).tracks.size)
        assertEquals("Song One", state.tracks[0].title)
        assertEquals("Song Two", state.tracks[1].title)
    }
    
    @Test
    fun `emits Empty state when repository returns empty list`() = runTest {
        fakeRepository.setTracks(emptyList())
        
        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()
        
        assertEquals(LibraryUiState.Empty, viewModel.uiState.value)
    }
    
    @Test
    fun `caches results and does not reload on subsequent calls`() = runTest {
        val tracks = listOf(createTrack(1, "Song One", "Artist A"))
        fakeRepository.setTracks(tracks)
        
        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()
        
        // Call loadLibrary again
        viewModel.loadLibrary()
        advanceUntilIdle()
        
        // Repository should only be called once
        assertEquals(1, fakeRepository.syncLibraryCallCount)
    }
    
    @Test
    fun `refresh reloads library even when cached`() = runTest {
        val tracks = listOf(createTrack(1, "Song One", "Artist A"))
        fakeRepository.setTracks(tracks)
        
        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()
        
        // Refresh should reload
        viewModel.refresh()
        advanceUntilIdle()
        
        assertEquals(2, fakeRepository.syncLibraryCallCount)
    }
    
    @Test
    fun `toggleFavorite calls repository`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.toggleFavorite(1L)
        advanceUntilIdle()
        
        assertEquals(1, fakeFavoritesRepository.toggleFavoriteCallCount)
        assertEquals(1L, fakeFavoritesRepository.lastToggledTrackId)
    }
    
    @Test
    fun `setFilter favoritesOnly narrows tracks`() = runTest {
        val tracks = listOf(
            createTrack(1, "Fav Song", "Artist A"),
            createTrack(2, "Other Song", "Artist B")
        )
        fakeRepository.setTracks(tracks)
        fakeFavoritesRepository.setFavoriteIds(setOf(1L))

        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()

        viewModel.setFilter(LibraryFilter(favoritesOnly = true))
        advanceUntilIdle()

        val state = viewModel.uiState.value as LibraryUiState.Content
        assertEquals(1, state.tracks.size)
        assertEquals(1L, state.tracks.first().id)
    }

    @Test
    fun `setLocalQuery filters by title`() = runTest {
        val tracks = listOf(
            createTrack(1, "Alpha", "Artist"),
            createTrack(2, "Beta", "Artist")
        )
        fakeRepository.setTracks(tracks)

        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()

        viewModel.setLocalQuery("alp")
        advanceUntilIdle()

        val state = viewModel.uiState.value as LibraryUiState.Content
        assertEquals(1, state.tracks.size)
        assertEquals("Alpha", state.tracks.first().title)
    }

    @Test
    fun `empty filter results sets showNoFilterResults`() = runTest {
        fakeRepository.setTracks(listOf(createTrack(1, "Song", "Artist")))

        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()

        viewModel.setFilter(LibraryFilter(durationBucket = DurationBucket.LONG))
        advanceUntilIdle()

        val state = viewModel.uiState.value as LibraryUiState.Content
        assertTrue(state.showNoFilterResults)
        assertTrue(state.tracks.isEmpty())
    }

    @Test
    fun `Content state includes favorite IDs`() = runTest {
        val tracks = listOf(createTrack(1, "Song One", "Artist A"))
        fakeRepository.setTracks(tracks)
        
        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()
        
        // Set favorites AFTER library loads to trigger flow update
        fakeFavoritesRepository.setFavoriteIds(setOf(1L))
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is LibraryUiState.Content)
        assertTrue((state as LibraryUiState.Content).favoriteIds.contains(1L))
    }
    
    @Test
    fun `scan summary exposed when sync completes`() = runTest {
        fakeRepository.setTracks(listOf(createTrack(1, "Song One", "Artist A")))

        val viewModel = createViewModel()
        viewModel.loadLibrary()
        advanceUntilIdle()

        val summary = viewModel.scanSummary.value
        assertEquals(1, summary?.indexedCount)
    }

    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(
            application,
            fakeRepository,
            fakeFavoritesRepository,
            fakeTrackDao
        )
    }
    
    private fun createTrack(id: Long, title: String, artist: String): TrackInfo {
        return TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = artist,
            album = "Test Album",
            duration = 180000L
        )
    }
}

class FakeMusicLibraryRepository : MusicLibraryRepository {
    private var tracks: List<TrackInfo> = emptyList()
    var syncLibraryCallCount = 0
        private set
    var scanMusicDirectoriesCallCount = 0
        private set
    private var lastScanResult: LibraryScanResult = LibraryScanResult(emptyList(), 0, 0, 0)

    fun setTracks(tracks: List<TrackInfo>) {
        this.tracks = tracks
        lastScanResult = LibraryScanResult(
            tracks = tracks,
            indexedCount = tracks.size,
            skippedDurationCount = 0,
            skippedFolderCount = 0
        )
    }

    override suspend fun syncLibrary(): LibraryScanResult {
        syncLibraryCallCount++
        return lastScanResult
    }

    override suspend fun getAllTracks(): List<TrackInfo> = syncLibrary().tracks

    override suspend fun scanMusicDirectories() {
        scanMusicDirectoriesCallCount++
    }
}

class FakeTrackDao : TrackDao {
    override suspend fun insertAll(tracks: List<TrackEntity>) = Unit
    override suspend fun insert(track: TrackEntity) = Unit
    override suspend fun getById(trackId: Long): TrackEntity? = null
    override suspend fun getAll(): List<TrackEntity> = emptyList()
    override suspend fun getByIds(trackIds: List<Long>): List<TrackEntity> = emptyList()
    override fun getRecentlyAddedTracks(limit: Int): Flow<List<TrackEntity>> {
        return MutableStateFlow(emptyList())
    }
    override suspend fun getTracksAddedSince(sinceMs: Long): List<TrackEntity> = emptyList()
    override suspend fun searchTracks(query: String, limit: Int): List<TrackEntity> = emptyList()
    override suspend fun deleteStaleEntries(validIds: List<Long>) = Unit
    override suspend fun deleteAll() = Unit
}

class FakeFavoritesRepository : FavoritesRepository {
    private val favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val favorites = MutableStateFlow<List<TrackInfo>>(emptyList())
    
    var toggleFavoriteCallCount = 0
        private set
    var lastToggledTrackId: Long? = null
        private set
    
    fun setFavoriteIds(ids: Set<Long>) {
        favoriteIds.value = ids
    }
    
    fun setFavorites(tracks: List<TrackInfo>) {
        favorites.value = tracks
    }
    
    override suspend fun toggleFavorite(trackId: Long) {
        toggleFavoriteCallCount++
        lastToggledTrackId = trackId
    }
    
    override fun isFavorite(trackId: Long): Flow<Boolean> {
        return MutableStateFlow(favoriteIds.value.contains(trackId))
    }
    
    override fun getAllFavorites(): Flow<List<TrackInfo>> {
        return favorites
    }
    
    override fun getAllFavoriteIds(): Flow<Set<Long>> {
        return favoriteIds
    }
}
