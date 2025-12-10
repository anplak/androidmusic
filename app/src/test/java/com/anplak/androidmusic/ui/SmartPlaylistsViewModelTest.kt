package com.anplak.androidmusic.ui

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.SmartPlaylistRepository
import com.anplak.androidmusic.data.SmartPlaylistType
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
class SmartPlaylistsViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeRepository: FakeSmartPlaylistRepository
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeSmartPlaylistRepository()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is Loading`() = runTest {
        val viewModel = createViewModel()
        
        assertEquals(SmartPlaylistDetailUiState.Loading, viewModel.detailState.value)
    }
    
    @Test
    fun `loadSmartPlaylist emits Content state with tracks`() = runTest {
        val tracks = listOf(
            createTrack(1, "Song One"),
            createTrack(2, "Song Two")
        )
        fakeRepository.setTracksForType(SmartPlaylistType.MOST_PLAYED, tracks)
        
        val viewModel = createViewModel()
        viewModel.loadSmartPlaylist(SmartPlaylistType.MOST_PLAYED)
        advanceUntilIdle()
        
        val state = viewModel.detailState.value
        assertTrue(state is SmartPlaylistDetailUiState.Content)
        assertEquals(SmartPlaylistType.MOST_PLAYED, (state as SmartPlaylistDetailUiState.Content).type)
        assertEquals(2, state.tracks.size)
        assertEquals("Song One", state.tracks[0].title)
    }
    
    @Test
    fun `loadSmartPlaylist emits Empty state when no tracks`() = runTest {
        fakeRepository.setTracksForType(SmartPlaylistType.RECENTLY_PLAYED, emptyList())
        
        val viewModel = createViewModel()
        viewModel.loadSmartPlaylist(SmartPlaylistType.RECENTLY_PLAYED)
        advanceUntilIdle()
        
        val state = viewModel.detailState.value
        assertEquals(SmartPlaylistDetailUiState.Empty, state)
    }
    
    @Test
    fun `loadSmartPlaylist updates state when called with different type`() = runTest {
        fakeRepository.setTracksForType(SmartPlaylistType.MOST_PLAYED, listOf(createTrack(1, "Most Played")))
        fakeRepository.setTracksForType(SmartPlaylistType.RECENTLY_ADDED, listOf(createTrack(2, "Recently Added")))
        
        val viewModel = createViewModel()
        
        viewModel.loadSmartPlaylist(SmartPlaylistType.MOST_PLAYED)
        advanceUntilIdle()
        
        var state = viewModel.detailState.value as SmartPlaylistDetailUiState.Content
        assertEquals(SmartPlaylistType.MOST_PLAYED, state.type)
        
        viewModel.loadSmartPlaylist(SmartPlaylistType.RECENTLY_ADDED)
        advanceUntilIdle()
        
        state = viewModel.detailState.value as SmartPlaylistDetailUiState.Content
        assertEquals(SmartPlaylistType.RECENTLY_ADDED, state.type)
    }
    
    private fun createViewModel(): SmartPlaylistsViewModel {
        return SmartPlaylistsViewModel(application, fakeRepository)
    }
    
    private fun createTrack(id: Long, title: String): TrackInfo {
        return TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = "Artist",
            album = "Album",
            duration = 180000L
        )
    }
}

class FakeSmartPlaylistRepository : SmartPlaylistRepository {
    private val tracksMap = mutableMapOf<SmartPlaylistType, List<TrackInfo>>()
    
    fun setTracksForType(type: SmartPlaylistType, tracks: List<TrackInfo>) {
        tracksMap[type] = tracks
    }
    
    override fun getMostPlayed(limit: Int): Flow<List<TrackInfo>> {
        return MutableStateFlow(tracksMap[SmartPlaylistType.MOST_PLAYED] ?: emptyList())
    }
    
    override fun getRecentlyPlayed(limit: Int): Flow<List<TrackInfo>> {
        return MutableStateFlow(tracksMap[SmartPlaylistType.RECENTLY_PLAYED] ?: emptyList())
    }
    
    override fun getRecentlyAdded(limit: Int): Flow<List<TrackInfo>> {
        return MutableStateFlow(tracksMap[SmartPlaylistType.RECENTLY_ADDED] ?: emptyList())
    }
    
    override fun getTracksForType(type: SmartPlaylistType, limit: Int): Flow<List<TrackInfo>> {
        return MutableStateFlow(tracksMap[type] ?: emptyList())
    }
}

