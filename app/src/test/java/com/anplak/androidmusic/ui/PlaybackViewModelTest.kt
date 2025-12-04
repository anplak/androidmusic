package com.anplak.androidmusic.ui

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state has no selected track`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.selectedTrack)
    }
    
    @Test
    fun `initial playback state is not playing`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        assertEquals(false, viewModel.uiState.value.isPlaying)
    }
    
    @Test
    fun `initial position and duration are zero`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        assertEquals(0L, viewModel.uiState.value.currentPosition)
        assertEquals(0L, viewModel.uiState.value.duration)
    }
    
    @Test
    fun `initial state has no error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `initial queue state is empty`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        assertEquals(0, viewModel.uiState.value.queuePosition)
        assertEquals(0, viewModel.uiState.value.queueSize)
        assertEquals(false, viewModel.uiState.value.hasNext)
        assertEquals(false, viewModel.uiState.value.hasPrevious)
    }
    
    @Test
    fun `clearTrack resets state`() = runTest {
        val viewModel = createViewModel()
        val tracks = createTestTracks(3)
        
        viewModel.onTrackSelected(tracks, 0)
        advanceUntilIdle()
        
        viewModel.clearTrack()
        advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.selectedTrack)
        assertEquals(0, viewModel.uiState.value.queueSize)
    }
    
    private fun createViewModel(): PlaybackViewModel {
        return PlaybackViewModel(application)
    }
    
    private fun createTestTracks(count: Int): List<TrackInfo> {
        return (1..count).map { index ->
            TrackInfo(
                uri = Uri.parse("content://media/external/audio/media/$index"),
                title = "Track $index",
                artist = "Artist $index",
                album = "Album $index",
                duration = 180000L
            )
        }
    }
    
    private fun createTestTrack(
        id: Long = 1,
        title: String = "Test Song",
        artist: String = "Test Artist"
    ): TrackInfo {
        return TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = artist,
            album = "Test Album",
            duration = 180000L
        )
    }
}
