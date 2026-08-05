package com.anplak.androidmusic.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.Playlist
import com.anplak.androidmusic.data.PlaylistOperationResult
import com.anplak.androidmusic.data.PlaylistRepository
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
class PlaylistsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakePlaylistRepository: TestPlaylistRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakePlaylistRepository = TestPlaylistRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state emits Empty when no playlists`() =
        runTest {
            fakePlaylistRepository.setPlaylists(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(PlaylistsUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun `emits Content state with playlists`() =
        runTest {
            val playlists =
                listOf(
                    Playlist(1, "My Playlist", System.currentTimeMillis()),
                    Playlist(2, "Another Playlist", System.currentTimeMillis()),
                )
            fakePlaylistRepository.setPlaylists(playlists)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is PlaylistsUiState.Content)
            assertEquals(2, (state as PlaylistsUiState.Content).playlists.size)
            assertEquals("My Playlist", state.playlists[0].name)
        }

    @Test
    fun `createPlaylist calls repository with name`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.createPlaylist("New Playlist")
            advanceUntilIdle()

            assertEquals(1, fakePlaylistRepository.createPlaylistCallCount)
            assertEquals("New Playlist", fakePlaylistRepository.lastCreatedPlaylistName)
        }

    @Test
    fun `createPlaylist ignores blank name`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.createPlaylist("   ")
            advanceUntilIdle()

            assertEquals(0, fakePlaylistRepository.createPlaylistCallCount)
        }

    @Test
    fun `deletePlaylist calls repository`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.deletePlaylist(1L)
            advanceUntilIdle()

            assertEquals(1, fakePlaylistRepository.deletePlaylistCallCount)
            assertEquals(1L, fakePlaylistRepository.lastDeletedPlaylistId)
        }

    @Test
    fun `addTrackToPlaylist calls repository`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.addTrackToPlaylist(1L, 2L)
            advanceUntilIdle()

            assertEquals(1, fakePlaylistRepository.addTrackCallCount)
            assertEquals(1L, fakePlaylistRepository.lastAddedToPlaylistId)
            assertEquals(2L, fakePlaylistRepository.lastAddedTrackId)
        }

    @Test
    fun `removeTrackFromPlaylist calls repository`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.removeTrackFromPlaylist(1L, 2L)
            advanceUntilIdle()

            assertEquals(1, fakePlaylistRepository.removeTrackCallCount)
        }

    @Test
    fun `createPlaylistAndAddTrack creates playlist and adds track`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.createPlaylistAndAddTrack("New Playlist", 42L)
            advanceUntilIdle()

            // Verify playlist was created
            assertEquals(1, fakePlaylistRepository.createPlaylistCallCount)
            assertEquals("New Playlist", fakePlaylistRepository.lastCreatedPlaylistName)

            // Verify track was added to the newly created playlist
            assertEquals(1, fakePlaylistRepository.addTrackCallCount)
            assertEquals(1L, fakePlaylistRepository.lastAddedToPlaylistId) // returned from createPlaylist
            assertEquals(42L, fakePlaylistRepository.lastAddedTrackId)
        }

    @Test
    fun `createPlaylistAndAddTrack ignores blank name`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.createPlaylistAndAddTrack("   ", 42L)
            advanceUntilIdle()

            assertEquals(0, fakePlaylistRepository.createPlaylistCallCount)
            assertEquals(0, fakePlaylistRepository.addTrackCallCount)
        }

    @Test
    fun `loadPlaylistDetail emits Content state with playlist and tracks`() =
        runTest {
            val playlist = Playlist(1L, "My Playlist", System.currentTimeMillis(), trackCount = 2)
            val tracks =
                listOf(
                    TrackInfo(TrackInfo.uriFromId(1), "Track 1", "Artist", "Album", 180000),
                    TrackInfo(TrackInfo.uriFromId(2), "Track 2", "Artist", "Album", 200000),
                )
            fakePlaylistRepository.setPlaylistById(playlist)
            fakePlaylistRepository.setPlaylistTracks(tracks)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.loadPlaylistDetail(1L)
            advanceUntilIdle()

            val detailState = viewModel.detailState.value
            assertTrue(detailState is PlaylistDetailUiState.Content)
            assertEquals("My Playlist", (detailState as PlaylistDetailUiState.Content).playlist.name)
            assertEquals(2, detailState.tracks.size)
        }

    @Test
    fun `loadPlaylistDetail emits NotFound when playlist does not exist`() =
        runTest {
            fakePlaylistRepository.setPlaylistById(null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.loadPlaylistDetail(999L)
            advanceUntilIdle()

            assertEquals(PlaylistDetailUiState.NotFound, viewModel.detailState.value)
        }

    // Collection-level addToPlaylist tests

    @Test
    fun `addCollectionToPlaylist emits Error when track list is empty`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.addCollectionToPlaylist(1L, "Test Collection", emptyList())
            advanceUntilIdle()

            val state = viewModel.operationState.value
            assertTrue(state is PlaylistOperationState.Error)
            assertEquals("No tracks to add", (state as PlaylistOperationState.Error).message)
        }

    @Test
    fun `addCollectionToPlaylist prevents duplicate submit during loading`() =
        runTest {
            fakePlaylistRepository.setAddTracksDelay(1000L)
            fakePlaylistRepository.setAddTracksResult(
                PlaylistOperationResult(addedCount = 5, skippedCount = 0, playlistId = 1L),
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            // First call
            viewModel.addCollectionToPlaylist(1L, "Test Collection", listOf(1L, 2L, 3L, 4L, 5L))

            // Second call during loading - should be ignored
            viewModel.addCollectionToPlaylist(1L, "Test Collection", listOf(1L, 2L))

            advanceUntilIdle()

            // Repository should only be called once
            assertEquals(1, fakePlaylistRepository.addTracksCallCount)
        }

    @Test
    fun `addCollectionToPlaylist emits Success with counts for existing playlist`() =
        runTest {
            fakePlaylistRepository.setAddTracksResult(
                PlaylistOperationResult(addedCount = 8, skippedCount = 2, playlistId = 1L),
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.addCollectionToPlaylist(1L, "Test Album", listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L))
            advanceUntilIdle()

            val state = viewModel.operationState.value
            assertTrue(state is PlaylistOperationState.Success)
            val result = (state as PlaylistOperationState.Success).result
            assertEquals(8, result.addedCount)
            assertEquals(2, result.skippedCount)
            assertEquals(1L, result.playlistId)
        }

    @Test
    fun `addCollectionToPlaylist emits Success with counts for new playlist`() =
        runTest {
            fakePlaylistRepository.setCreatePlaylistId(99L)
            fakePlaylistRepository.setAddTracksResult(
                PlaylistOperationResult(addedCount = 10, skippedCount = 0, playlistId = 99L),
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            // Pass null playlistId to create new playlist
            viewModel.addCollectionToPlaylist(null, "New Artist Playlist", listOf(1L, 2L, 3L, 4L, 5L))
            advanceUntilIdle()

            // Verify playlist was created
            assertEquals(1, fakePlaylistRepository.createPlaylistCallCount)
            assertEquals("New Artist Playlist", fakePlaylistRepository.lastCreatedPlaylistName)

            val state = viewModel.operationState.value
            assertTrue(state is PlaylistOperationState.Success)
            val result = (state as PlaylistOperationState.Success).result
            assertEquals(10, result.addedCount)
            assertEquals(99L, result.playlistId)
        }

    @Test
    fun `addCollectionToPlaylist emits Error on database failure`() =
        runTest {
            fakePlaylistRepository.setAddTracksError(RuntimeException("Database connection failed"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.addCollectionToPlaylist(1L, "Test Collection", listOf(1L, 2L, 3L))
            advanceUntilIdle()

            val state = viewModel.operationState.value
            assertTrue(state is PlaylistOperationState.Error)
            assertEquals("Database connection failed", (state as PlaylistOperationState.Error).message)
        }

    @Test
    fun `clearOperationState resets to Idle`() =
        runTest {
            fakePlaylistRepository.setAddTracksResult(
                PlaylistOperationResult(addedCount = 5, skippedCount = 0, playlistId = 1L),
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.addCollectionToPlaylist(1L, "Test Collection", listOf(1L, 2L, 3L))
            advanceUntilIdle()

            // Verify we're in Success state
            assertTrue(viewModel.operationState.value is PlaylistOperationState.Success)

            viewModel.clearOperationState()

            // Should be back to Idle
            assertEquals(PlaylistOperationState.Idle, viewModel.operationState.value)
        }

    private fun createViewModel(): PlaylistsViewModel {
        return PlaylistsViewModel(application, fakePlaylistRepository)
    }
}

class TestPlaylistRepository : PlaylistRepository {
    private val playlists = MutableStateFlow<List<Playlist>>(emptyList())
    private val playlistTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    private val playlistById = MutableStateFlow<Playlist?>(null)

    var createPlaylistCallCount = 0
        private set
    var lastCreatedPlaylistName: String? = null
        private set
    var deletePlaylistCallCount = 0
        private set
    var lastDeletedPlaylistId: Long? = null
        private set
    var addTrackCallCount = 0
        private set
    var lastAddedToPlaylistId: Long? = null
        private set
    var lastAddedTrackId: Long? = null
        private set
    var removeTrackCallCount = 0
        private set
    var addTracksCallCount = 0
        private set
    var lastAddedTracksPlaylistId: Long? = null
        private set
    var lastAddedTrackIds: List<Long> = emptyList()
        private set

    private var createPlaylistId = 1L
    private var addTracksDelay = 0L
    private var addTracksResult: PlaylistOperationResult = PlaylistOperationResult(0, 0, 0L)
    private var addTracksError: Throwable? = null

    fun setPlaylists(playlistList: List<Playlist>) {
        playlists.value = playlistList
    }

    fun setPlaylistById(playlist: Playlist?) {
        playlistById.value = playlist
    }

    fun setPlaylistTracks(tracks: List<TrackInfo>) {
        playlistTracks.value = tracks
    }

    fun setCreatePlaylistId(id: Long) {
        createPlaylistId = id
    }

    fun setAddTracksDelay(delay: Long) {
        addTracksDelay = delay
    }

    fun setAddTracksResult(result: PlaylistOperationResult) {
        addTracksResult = result
        addTracksError = null
    }

    fun setAddTracksError(error: Throwable) {
        addTracksError = error
    }

    override suspend fun createPlaylist(name: String): Long {
        createPlaylistCallCount++
        lastCreatedPlaylistName = name
        return createPlaylistId
    }

    override suspend fun createPlaylistWithTracks(
        name: String,
        trackIds: List<Long>,
    ): Long {
        createPlaylistCallCount++
        lastCreatedPlaylistName = name
        return createPlaylistId
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        deletePlaylistCallCount++
        lastDeletedPlaylistId = playlistId
    }

    override suspend fun renamePlaylist(
        playlistId: Long,
        name: String,
    ) {
        // Not tested in this suite
    }

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlists
    }

    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> {
        return playlistById
    }

    override suspend fun addTrackToPlaylist(
        playlistId: Long,
        trackId: Long,
    ) {
        addTrackCallCount++
        lastAddedToPlaylistId = playlistId
        lastAddedTrackId = trackId
    }

    override suspend fun removeTrackFromPlaylist(
        playlistId: Long,
        trackId: Long,
    ) {
        removeTrackCallCount++
    }

    override suspend fun removeTracksFromPlaylist(
        playlistId: Long,
        trackIds: List<Long>,
    ) {
        removeTrackCallCount++
    }

    override suspend fun reorderPlaylistTracks(
        playlistId: Long,
        orderedTrackIds: List<Long>,
    ) = Unit

    override suspend fun duplicatePlaylist(
        sourcePlaylistId: Long,
        name: String,
    ): Long = 2L

    override suspend fun mergePlaylists(
        primaryPlaylistId: Long,
        secondaryPlaylistId: Long,
        name: String,
    ): Long = 3L

    override fun getPlaylistTracks(playlistId: Long): Flow<List<TrackInfo>> {
        return playlistTracks
    }

    override suspend fun isTrackInPlaylist(
        playlistId: Long,
        trackId: Long,
    ): Boolean {
        return false
    }

    override suspend fun addTracksToPlaylist(
        playlistId: Long,
        trackIds: List<Long>,
    ): PlaylistOperationResult {
        if (addTracksDelay > 0) {
            kotlinx.coroutines.delay(addTracksDelay)
        }
        addTracksError?.let { throw it }
        addTracksCallCount++
        lastAddedTracksPlaylistId = playlistId
        lastAddedTrackIds = trackIds
        return addTracksResult
    }
}
