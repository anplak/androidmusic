package com.anplak.androidmusic.ui

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeMusicLibraryRepository()
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
        assertEquals(1, fakeRepository.getAllTracksCallCount)
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
        
        assertEquals(2, fakeRepository.getAllTracksCallCount)
    }
    
    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(application, fakeRepository)
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
    var getAllTracksCallCount = 0
        private set
    var scanMusicDirectoriesCallCount = 0
        private set
    
    fun setTracks(tracks: List<TrackInfo>) {
        this.tracks = tracks
    }
    
    override suspend fun getAllTracks(): List<TrackInfo> {
        getAllTracksCallCount++
        return tracks
    }
    
    override suspend fun scanMusicDirectories() {
        scanMusicDirectoriesCallCount++
    }
}

