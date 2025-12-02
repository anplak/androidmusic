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
import org.junit.Assert.assertNotNull
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
    fun `onTrackSelected sets selectedTrack correctly`() = runTest {
        val viewModel = createViewModel()
        val track = createTestTrack()
        
        viewModel.onTrackSelected(track)
        advanceUntilIdle()
        
        val selectedTrack = viewModel.uiState.value.selectedTrack
        assertNotNull(selectedTrack)
        assertEquals("Test Song", selectedTrack?.title)
        assertEquals("Test Artist", selectedTrack?.artist)
    }
    
    @Test
    fun `clearTrack resets selectedTrack to null`() = runTest {
        val viewModel = createViewModel()
        val track = createTestTrack()
        
        viewModel.onTrackSelected(track)
        advanceUntilIdle()
        
        viewModel.clearTrack()
        advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.selectedTrack)
    }
    
    @Test
    fun `selecting different track updates selectedTrack`() = runTest {
        val viewModel = createViewModel()
        val track1 = createTestTrack(id = 1, title = "First Song")
        val track2 = createTestTrack(id = 2, title = "Second Song")
        
        viewModel.onTrackSelected(track1)
        advanceUntilIdle()
        assertEquals("First Song", viewModel.uiState.value.selectedTrack?.title)
        
        viewModel.onTrackSelected(track2)
        advanceUntilIdle()
        assertEquals("Second Song", viewModel.uiState.value.selectedTrack?.title)
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
    
    private fun createViewModel(): PlaybackViewModel {
        return PlaybackViewModel(application)
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
