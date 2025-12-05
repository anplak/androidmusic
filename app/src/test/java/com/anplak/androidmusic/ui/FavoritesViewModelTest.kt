package com.anplak.androidmusic.ui

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.FavoritesRepository
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
class FavoritesViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeFavoritesRepository: TestFavoritesRepository
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeFavoritesRepository = TestFavoritesRepository()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state emits Empty when no favorites`() = runTest {
        fakeFavoritesRepository.setFavorites(emptyList())
        
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        assertEquals(FavoritesUiState.Empty, viewModel.uiState.value)
    }
    
    @Test
    fun `emits Content state with favorites`() = runTest {
        val favorites = listOf(
            createTrack(1, "Favorite Song", "Artist A"),
            createTrack(2, "Another Favorite", "Artist B")
        )
        fakeFavoritesRepository.setFavorites(favorites)
        
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is FavoritesUiState.Content)
        assertEquals(2, (state as FavoritesUiState.Content).tracks.size)
        assertEquals("Favorite Song", state.tracks[0].title)
    }
    
    @Test
    fun `toggleFavorite calls repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        viewModel.toggleFavorite(1L)
        advanceUntilIdle()
        
        assertEquals(1, fakeFavoritesRepository.toggleFavoriteCallCount)
        assertEquals(1L, fakeFavoritesRepository.lastToggledTrackId)
    }
    
    @Test
    fun `state updates when favorites change`() = runTest {
        fakeFavoritesRepository.setFavorites(emptyList())
        
        val viewModel = createViewModel()
        advanceUntilIdle()
        
        assertEquals(FavoritesUiState.Empty, viewModel.uiState.value)
        
        // Add a favorite
        fakeFavoritesRepository.setFavorites(listOf(createTrack(1, "New Favorite", "Artist")))
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value is FavoritesUiState.Content)
    }
    
    private fun createViewModel(): FavoritesViewModel {
        return FavoritesViewModel(application, fakeFavoritesRepository)
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

class TestFavoritesRepository : FavoritesRepository {
    private val favorites = MutableStateFlow<List<TrackInfo>>(emptyList())
    private val favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    
    var toggleFavoriteCallCount = 0
        private set
    var lastToggledTrackId: Long? = null
        private set
    
    fun setFavorites(tracks: List<TrackInfo>) {
        favorites.value = tracks
        favoriteIds.value = tracks.map { it.id }.toSet()
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

