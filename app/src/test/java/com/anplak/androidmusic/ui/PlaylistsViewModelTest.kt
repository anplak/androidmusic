package com.anplak.androidmusic.ui

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.Playlist
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
    fun `initial state emits Empty when no playlists`() = runTest {
        fakePlaylistRepository.setPlaylists(emptyList())
        
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        assertEquals(PlaylistsUiState.Empty, viewModel.uiState.value)
    }
    
    @Test
    fun `emits Content state with playlists`() = runTest {
        val playlists = listOf(
            Playlist(1, "My Playlist", System.currentTimeMillis()),
            Playlist(2, "Another Playlist", System.currentTimeMillis())
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
    fun `createPlaylist calls repository with name`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        viewModel.createPlaylist("New Playlist")
        advanceUntilIdle()
        
        assertEquals(1, fakePlaylistRepository.createPlaylistCallCount)
        assertEquals("New Playlist", fakePlaylistRepository.lastCreatedPlaylistName)
    }
    
    @Test
    fun `createPlaylist ignores blank name`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        viewModel.createPlaylist("   ")
        advanceUntilIdle()
        
        assertEquals(0, fakePlaylistRepository.createPlaylistCallCount)
    }
    
    @Test
    fun `deletePlaylist calls repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        viewModel.deletePlaylist(1L)
        advanceUntilIdle()
        
        assertEquals(1, fakePlaylistRepository.deletePlaylistCallCount)
        assertEquals(1L, fakePlaylistRepository.lastDeletedPlaylistId)
    }
    
    @Test
    fun `addTrackToPlaylist calls repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        viewModel.addTrackToPlaylist(1L, 2L)
        advanceUntilIdle()
        
        assertEquals(1, fakePlaylistRepository.addTrackCallCount)
        assertEquals(1L, fakePlaylistRepository.lastAddedToPlaylistId)
        assertEquals(2L, fakePlaylistRepository.lastAddedTrackId)
    }
    
    @Test
    fun `removeTrackFromPlaylist calls repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        viewModel.removeTrackFromPlaylist(1L, 2L)
        advanceUntilIdle()
        
        assertEquals(1, fakePlaylistRepository.removeTrackCallCount)
    }
    
    private fun createViewModel(): PlaylistsViewModel {
        return PlaylistsViewModel(application, fakePlaylistRepository)
    }
}

class TestPlaylistRepository : PlaylistRepository {
    private val playlists = MutableStateFlow<List<Playlist>>(emptyList())
    private val playlistTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    
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
    
    fun setPlaylists(playlistList: List<Playlist>) {
        playlists.value = playlistList
    }
    
    fun setPlaylistTracks(tracks: List<TrackInfo>) {
        playlistTracks.value = tracks
    }
    
    override suspend fun createPlaylist(name: String): Long {
        createPlaylistCallCount++
        lastCreatedPlaylistName = name
        return 1L
    }
    
    override suspend fun deletePlaylist(playlistId: Long) {
        deletePlaylistCallCount++
        lastDeletedPlaylistId = playlistId
    }
    
    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        // Not tested in this suite
    }
    
    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlists
    }
    
    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> {
        return MutableStateFlow(playlists.value.find { it.id == playlistId })
    }
    
    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        addTrackCallCount++
        lastAddedToPlaylistId = playlistId
        lastAddedTrackId = trackId
    }
    
    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        removeTrackCallCount++
    }
    
    override fun getPlaylistTracks(playlistId: Long): Flow<List<TrackInfo>> {
        return playlistTracks
    }
    
    override suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean {
        return false
    }
}

