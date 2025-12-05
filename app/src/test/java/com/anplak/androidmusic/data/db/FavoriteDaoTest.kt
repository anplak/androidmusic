package com.anplak.androidmusic.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FavoriteDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var favoriteDao: FavoriteDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        trackDao = database.trackDao()
        favoriteDao = database.favoriteDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `addFavorite and isFavorite returns true`() = runTest {
        // Insert track first (foreign key requirement)
        trackDao.insert(createTrackEntity(1))
        
        favoriteDao.addFavorite(FavoriteEntity(trackId = 1))
        
        assertTrue(favoriteDao.isFavorite(1).first())
    }
    
    @Test
    fun `isFavorite returns false for non-favorite track`() = runTest {
        trackDao.insert(createTrackEntity(1))
        
        assertFalse(favoriteDao.isFavorite(1).first())
    }
    
    @Test
    fun `removeFavorite removes the favorite`() = runTest {
        trackDao.insert(createTrackEntity(1))
        favoriteDao.addFavorite(FavoriteEntity(trackId = 1))
        
        assertTrue(favoriteDao.isFavoriteSync(1))
        
        favoriteDao.removeFavorite(1)
        
        assertFalse(favoriteDao.isFavoriteSync(1))
    }
    
    @Test
    fun `getAllFavorites returns all favorite tracks`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        trackDao.insert(createTrackEntity(3))
        
        favoriteDao.addFavorite(FavoriteEntity(trackId = 1))
        favoriteDao.addFavorite(FavoriteEntity(trackId = 3))
        
        val favorites = favoriteDao.getAllFavorites().first()
        
        assertEquals(2, favorites.size)
        assertTrue(favorites.any { it.id == 1L })
        assertTrue(favorites.any { it.id == 3L })
    }
    
    @Test
    fun `getAllFavoriteIds returns list of favorite track IDs`() = runTest {
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        
        favoriteDao.addFavorite(FavoriteEntity(trackId = 1))
        favoriteDao.addFavorite(FavoriteEntity(trackId = 2))
        
        val ids = favoriteDao.getAllFavoriteIds().first()
        
        assertEquals(setOf(1L, 2L), ids.toSet())
    }
    
    @Test
    fun `deleting track cascades to favorites`() = runTest {
        trackDao.insert(createTrackEntity(1))
        favoriteDao.addFavorite(FavoriteEntity(trackId = 1))
        
        assertTrue(favoriteDao.isFavoriteSync(1))
        
        trackDao.deleteStaleEntries(emptyList())
        
        assertFalse(favoriteDao.isFavoriteSync(1))
    }
    
    private fun createTrackEntity(id: Long) = TrackEntity(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        duration = 180000L,
        path = "content://media/external/audio/media/$id",
        firstSeenAt = System.currentTimeMillis()
    )
}

