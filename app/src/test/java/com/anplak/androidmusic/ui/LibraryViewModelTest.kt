package com.anplak.androidmusic.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.DurationBucket
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.LibraryFilter
import com.anplak.androidmusic.data.LibraryScanResult
import com.anplak.androidmusic.data.LibrarySyncCoordinator
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.data.db.TrackEntity
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    private lateinit var syncCoordinator: LibrarySyncCoordinator

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeMusicLibraryRepository()
        fakeFavoritesRepository = FakeFavoritesRepository()
        fakeTrackDao = FakeTrackDao()
        syncCoordinator =
            LibrarySyncCoordinator(
                repository = fakeRepository,
                scope = CoroutineScope(testDispatcher),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading when cache is empty`() =
        runTest {
            val viewModel = createViewModel()

            assertEquals(LibraryUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `emits Content state with tracks after successful cold sync`() =
        runTest {
            val tracks =
                listOf(
                    createTrack(1, "Song One", "Artist A"),
                    createTrack(2, "Song Two", "Artist B"),
                )
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is LibraryUiState.Content)
            assertEquals(2, (state as LibraryUiState.Content).tracks.size)
            assertEquals("Song One", state.tracks[0].title)
            assertEquals("Song Two", state.tracks[1].title)
        }

    @Test
    fun `warm cache shows Content before sync completes`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setCachedTracks(tracks)
            fakeRepository.setSyncTracks(tracks)
            fakeRepository.syncDelayMs = 5_000

            val viewModel = createViewModel()
            testDispatcher.scheduler.runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is LibraryUiState.Content)
            assertEquals(1, (state as LibraryUiState.Content).tracks.size)
            assertEquals(0, fakeRepository.syncLibraryCallCount)

            advanceTimeBy(300)
            testDispatcher.scheduler.runCurrent()
            assertEquals(1, fakeRepository.syncLibraryCallCount)
        }

    @Test
    fun `emits Empty state when repository returns empty list`() =
        runTest {
            fakeRepository.setSyncTracks(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(LibraryUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun `onLibraryVisible debounces duplicate sync requests`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setCachedTracks(tracks)
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.resetSyncCallCount()
            viewModel.onLibraryVisible()
            viewModel.onLibraryVisible()
            viewModel.onLibraryVisible()
            advanceTimeBy(300)
            advanceUntilIdle()

            assertEquals(1, fakeRepository.syncLibraryCallCount)
        }

    @Test
    fun `refresh reloads library when cache is warm`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setCachedTracks(tracks)
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.resetSyncCallCount()
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(1, fakeRepository.syncLibraryCallCount)
            assertTrue(viewModel.uiState.value is LibraryUiState.Content)
        }

    @Test
    fun `refresh keeps Content visible when cache is warm`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setCachedTracks(tracks)
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.refresh()
            testDispatcher.scheduler.runCurrent()

            assertTrue(viewModel.uiState.value is LibraryUiState.Content)
        }

    @Test
    fun `sync failure keeps cache visible with retry flag`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setCachedTracks(tracks)
            fakeRepository.setSyncTracks(tracks)
            fakeRepository.shouldFailSync = true

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertTrue(state.syncFailed)
            assertEquals(1, state.tracks.size)
        }

    @Test
    fun `toggleFavorite calls repository`() =
        runTest {
            fakeRepository.setSyncTracks(listOf(createTrack(1, "Song One", "Artist A")))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.toggleFavorite(1L)
            advanceUntilIdle()

            assertEquals(1, fakeFavoritesRepository.toggleFavoriteCallCount)
            assertEquals(1L, fakeFavoritesRepository.lastToggledTrackId)
        }

    @Test
    fun `toggleFavorite updates favoriteIds optimistically`() =
        runTest {
            fakeRepository.setSyncTracks(listOf(createTrack(1, "Song One", "Artist A")))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.toggleFavorite(1L)

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertTrue(state.favoriteIds.contains(1L))
        }

    @Test
    fun `setFilter favoritesOnly narrows tracks`() =
        runTest {
            val tracks =
                listOf(
                    createTrack(1, "Fav Song", "Artist A"),
                    createTrack(2, "Other Song", "Artist B"),
                )
            fakeRepository.setSyncTracks(tracks)
            fakeFavoritesRepository.setFavoriteIds(setOf(1L))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setFilter(LibraryFilter(favoritesOnly = true))
            advanceUntilIdle()

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertEquals(1, state.tracks.size)
            assertEquals(1L, state.tracks.first().id)
        }

    @Test
    fun `setLocalQuery filters by title`() =
        runTest {
            val tracks =
                listOf(
                    createTrack(1, "Alpha", "Artist"),
                    createTrack(2, "Beta", "Artist"),
                )
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setLocalQuery("alp")
            advanceUntilIdle()

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertEquals(1, state.tracks.size)
            assertEquals("Alpha", state.tracks.first().title)
        }

    @Test
    fun `empty filter results sets showNoFilterResults`() =
        runTest {
            fakeRepository.setSyncTracks(listOf(createTrack(1, "Song", "Artist")))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setFilter(LibraryFilter(durationBucket = DurationBucket.LONG))
            advanceUntilIdle()

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertTrue(state.showNoFilterResults)
            assertTrue(state.tracks.isEmpty())
        }

    @Test
    fun `Content state includes favorite IDs`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeFavoritesRepository.setFavoriteIds(setOf(1L))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is LibraryUiState.Content)
            assertTrue((state as LibraryUiState.Content).favoriteIds.contains(1L))
        }

    @Test
    fun `scan summary exposed when sync completes`() =
        runTest {
            fakeRepository.setSyncTracks(listOf(createTrack(1, "Song One", "Artist A")))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val summary = viewModel.scanSummary.value
            assertEquals(1, summary?.indexedCount)
        }

    @Test
    fun `isRefreshing true while sync is running on warm cache`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setCachedTracks(tracks)
            fakeRepository.setSyncTracks(tracks)
            fakeRepository.syncDelayMs = 5_000

            val viewModel = createViewModel()
            testDispatcher.scheduler.runCurrent()

            advanceTimeBy(300)
            testDispatcher.scheduler.runCurrent()

            assertTrue(viewModel.uiState.value is LibraryUiState.Content)
            assertTrue((viewModel.uiState.value as LibraryUiState.Content).isRefreshing)
        }

    @Test
    fun `sync completion keeps favorite IDs in Content state`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setCachedTracks(tracks)
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeFavoritesRepository.setFavoriteIds(setOf(1L))
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertTrue(state.favoriteIds.contains(1L))
        }

    @Test
    fun `setBrowseTab does not trigger sync`() =
        runTest {
            val tracks =
                listOf(
                    createTrack(1, "Song One", "Artist A"),
                    createTrack(2, "Song Two", "Artist B"),
                )
            fakeRepository.setCachedTracks(tracks)
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()
            fakeRepository.resetSyncCallCount()

            viewModel.setBrowseTab(LibraryBrowseTab.Artists)
            viewModel.setBrowseTab(LibraryBrowseTab.Albums)
            advanceUntilIdle()

            assertEquals(0, fakeRepository.syncLibraryCallCount)
            val state = viewModel.uiState.value as LibraryUiState.Content
            assertEquals(LibraryBrowseTab.Albums, state.browseTab)
            assertEquals(2, state.artists.size)
        }

    @Test
    fun `artist and album counts update when cache changes`() =
        runTest {
            val initial = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setCachedTracks(initial)
            fakeRepository.setSyncTracks(initial)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val updated =
                listOf(
                    createTrack(1, "Song One", "Artist A"),
                    createTrack(2, "Song Two", "Artist B"),
                )
            fakeRepository.setCachedTracks(updated)
            advanceUntilIdle()

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertEquals(2, state.artists.size)
            assertEquals(2, state.albums.size)
        }

    @Test
    fun `filters apply only to tracks tab list`() =
        runTest {
            val tracks =
                listOf(
                    createTrack(1, "Fav Song", "Artist A"),
                    createTrack(2, "Other Song", "Artist B"),
                )
            fakeRepository.setSyncTracks(tracks)
            fakeFavoritesRepository.setFavoriteIds(setOf(1L))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setFilter(LibraryFilter(favoritesOnly = true))
            viewModel.setBrowseTab(LibraryBrowseTab.Artists)
            advanceUntilIdle()

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertEquals(1, state.tracks.size)
            assertEquals(2, state.artists.size)
        }

    @Test
    fun `saved browse tab restored from SavedStateHandle`() =
        runTest {
            val tracks = listOf(createTrack(1, "Song One", "Artist A"))
            fakeRepository.setSyncTracks(tracks)

            val viewModel =
                createViewModel(
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf(LibraryViewModel.KEY_BROWSE_TAB to LibraryBrowseTab.Artists.name),
                        ),
                )
            advanceUntilIdle()

            val state = viewModel.uiState.value as LibraryUiState.Content
            assertEquals(LibraryBrowseTab.Artists, state.browseTab)
        }

    @Test
    fun `tracksForArtist returns matching tracks`() =
        runTest {
            val tracks =
                listOf(
                    createTrack(1, "Alpha", "Artist A"),
                    createTrack(2, "Beta", "Artist B"),
                )
            fakeRepository.setSyncTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val artistTracks = viewModel.tracksForArtist("artist a")
            assertEquals(1, artistTracks.size)
            assertEquals("Alpha", artistTracks.first().title)
        }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): LibraryViewModel {
        return LibraryViewModel(
            application = application,
            savedStateHandle = savedStateHandle,
            repository = fakeRepository,
            favoritesRepository = fakeFavoritesRepository,
            trackDao = fakeTrackDao,
            syncCoordinator = syncCoordinator,
        )
    }

    private fun createTrack(
        id: Long,
        title: String,
        artist: String,
    ): TrackInfo {
        return TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = artist,
            album = "Test Album",
            duration = 180000L,
        )
    }
}

class FakeMusicLibraryRepository : MusicLibraryRepository {
    private val cacheFlow = MutableStateFlow<List<TrackInfo>>(emptyList())
    private var syncTracks: List<TrackInfo> = emptyList()
    private var lastScanResult: LibraryScanResult =
        LibraryScanResult(emptyList(), 0, 0, 0, 0)

    var syncLibraryCallCount = 0
        private set

    fun resetSyncCallCount() {
        syncLibraryCallCount = 0
    }

    var scanMusicDirectoriesCallCount = 0
        private set
    var syncDelayMs: Long = 0
    var shouldFailSync: Boolean = false

    fun setCachedTracks(tracks: List<TrackInfo>) {
        cacheFlow.value = tracks
    }

    fun setSyncTracks(tracks: List<TrackInfo>) {
        syncTracks = tracks
        lastScanResult =
            LibraryScanResult(
                tracks = tracks,
                indexedCount = tracks.size,
                skippedDurationCount = 0,
                skippedFolderCount = 0,
                skippedArtistCount = 0,
            )
    }

    override suspend fun getCachedTracks(): List<TrackInfo> = cacheFlow.value

    override fun observeCachedTracks(): Flow<List<TrackInfo>> = cacheFlow

    override suspend fun syncLibrary(): LibraryScanResult {
        syncLibraryCallCount++
        if (syncDelayMs > 0) {
            delay(syncDelayMs)
        }
        if (shouldFailSync) {
            throw RuntimeException("sync failed")
        }
        cacheFlow.value = syncTracks
        return lastScanResult
    }

    override suspend fun scanMusicDirectories() {
        scanMusicDirectoriesCallCount++
    }
}

class FakeTrackDao : TrackDao {
    private val tracksFlow = MutableStateFlow<List<TrackEntity>>(emptyList())

    override suspend fun insertAll(tracks: List<TrackEntity>) = Unit

    override suspend fun insert(track: TrackEntity) = Unit

    override suspend fun getById(trackId: Long): TrackEntity? = null

    override suspend fun getAll(): List<TrackEntity> = tracksFlow.value

    override fun observeAll(): Flow<List<TrackEntity>> = tracksFlow

    override suspend fun getByIds(trackIds: List<Long>): List<TrackEntity> = emptyList()

    override fun getRecentlyAddedTracks(limit: Int): Flow<List<TrackEntity>> {
        return MutableStateFlow(emptyList())
    }

    override suspend fun getTracksAddedSince(sinceMs: Long): List<TrackEntity> = emptyList()

    override suspend fun searchTracks(
        query: String,
        limit: Int,
    ): List<TrackEntity> = emptyList()

    override suspend fun deleteStaleEntries(validIds: List<Long>) = Unit

    override suspend fun deleteAll() = Unit

    override suspend fun getDistinctArtists(): List<String> = tracksFlow.value.map { it.artist }.distinct().sorted()

    override suspend fun getTrackPaths(): List<String> = tracksFlow.value.mapNotNull { it.path.takeIf { path -> path.isNotBlank() } }
}

class FakeFavoritesRepository : FavoritesRepository {
    private val favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val favorites = MutableStateFlow<List<TrackInfo>>(emptyList())

    var toggleFavoriteCallCount = 0
        private set
    var lastToggledTrackId: Long? = null
        private set
    var lastToggledTrack: TrackInfo? = null
        private set

    fun setFavoriteIds(ids: Set<Long>) {
        favoriteIds.value = ids
    }

    fun setFavorites(tracks: List<TrackInfo>) {
        favorites.value = tracks
    }

    override suspend fun toggleFavorite(track: TrackInfo) {
        toggleFavoriteCallCount++
        lastToggledTrackId = track.id
        lastToggledTrack = track
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

    override fun getFavoriteTimestamps(): Flow<Map<Long, Long>> {
        return MutableStateFlow(favoriteIds.value.associateWith { 0L })
    }
}
