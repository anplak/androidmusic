package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.FavoriteDao
import com.anplak.androidmusic.data.db.FavoriteEntity
import com.anplak.androidmusic.data.db.TrackEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FavoritesRepositoryTest {
    
    private lateinit var fakeFavoriteDao: FakeFavoriteDao
    private lateinit var repository: FavoritesRepository
    
    @Before
    fun setup() {
        fakeFavoriteDao = FakeFavoriteDao()
        repository = FavoritesRepositoryImpl(fakeFavoriteDao)
    }
    
    @Test
    fun `toggleFavorite adds favorite when not favorited`() = runTest {
        fakeFavoriteDao.setIsFavorite(false)
        
        repository.toggleFavorite(1L)
        
        assertTrue(fakeFavoriteDao.addFavoriteCalled)
        assertEquals(1L, fakeFavoriteDao.lastAddedFavoriteId)
    }
    
    @Test
    fun `toggleFavorite removes favorite when already favorited`() = runTest {
        fakeFavoriteDao.setIsFavorite(true)
        
        repository.toggleFavorite(1L)
        
        assertTrue(fakeFavoriteDao.removeFavoriteCalled)
        assertEquals(1L, fakeFavoriteDao.lastRemovedFavoriteId)
    }
    
    @Test
    fun `isFavorite returns flow from DAO`() = runTest {
        fakeFavoriteDao.setIsFavoriteFlow(true)
        
        val result = repository.isFavorite(1L).first()
        
        assertTrue(result)
    }
    
    @Test
    fun `getAllFavorites converts entities to TrackInfo`() = runTest {
        val entities = listOf(
            TrackEntity(1, "Track 1", "Artist", "Album", 180000, "path1", System.currentTimeMillis()),
            TrackEntity(2, "Track 2", "Artist", "Album", 200000, "path2", System.currentTimeMillis())
        )
        fakeFavoriteDao.setFavoriteTracks(entities)
        
        val favorites = repository.getAllFavorites().first()
        
        assertEquals(2, favorites.size)
        assertEquals("Track 1", favorites[0].title)
        assertEquals("Track 2", favorites[1].title)
    }
    
    @Test
    fun `getAllFavoriteIds returns flow from DAO`() = runTest {
        fakeFavoriteDao.setFavoriteIds(setOf(1L, 2L, 3L))
        
        val ids = repository.getAllFavoriteIds().first()
        
        assertEquals(setOf(1L, 2L, 3L), ids)
    }
}

class FakeFavoriteDao : FavoriteDao {
    private var isFavoriteSync = false
    private val isFavoriteFlow = MutableStateFlow(false)
    private val favoriteTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    private val favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    
    var addFavoriteCalled = false
        private set
    var lastAddedFavoriteId: Long? = null
        private set
    var removeFavoriteCalled = false
        private set
    var lastRemovedFavoriteId: Long? = null
        private set
    
    fun setIsFavorite(value: Boolean) {
        isFavoriteSync = value
    }
    
    fun setIsFavoriteFlow(value: Boolean) {
        isFavoriteFlow.value = value
    }
    
    fun setFavoriteTracks(tracks: List<TrackEntity>) {
        favoriteTracks.value = tracks
    }
    
    fun setFavoriteIds(ids: Set<Long>) {
        favoriteIds.value = ids
    }
    
    override suspend fun addFavorite(favorite: FavoriteEntity) {
        addFavoriteCalled = true
        lastAddedFavoriteId = favorite.trackId
    }
    
    override suspend fun removeFavorite(trackId: Long) {
        removeFavoriteCalled = true
        lastRemovedFavoriteId = trackId
    }
    
    override fun isFavorite(trackId: Long): Flow<Boolean> = isFavoriteFlow
    
    override suspend fun isFavoriteSync(trackId: Long): Boolean = isFavoriteSync
    
    override fun getAllFavorites(): Flow<List<TrackEntity>> = favoriteTracks
    
    override fun getAllFavoriteIds(): Flow<List<Long>> = MutableStateFlow(favoriteIds.value.toList())
    
    override fun getFavoriteCount(): Flow<Int> = MutableStateFlow(favoriteIds.value.size)
}

