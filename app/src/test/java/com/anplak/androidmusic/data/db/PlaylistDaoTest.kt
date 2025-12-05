package com.anplak.androidmusic.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaylistDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var playlistDao: PlaylistDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        trackDao = database.trackDao()
        playlistDao = database.playlistDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `createPlaylist returns generated ID`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        
        assertTrue(playlistId > 0)
    }
    
    @Test
    fun `getAllPlaylists returns all created playlists`() = runTest {
        playlistDao.createPlaylist(PlaylistEntity(name = "Playlist 1"))
        playlistDao.createPlaylist(PlaylistEntity(name = "Playlist 2"))
        
        val playlists = playlistDao.getAllPlaylists().first()
        
        assertEquals(2, playlists.size)
    }
    
    @Test
    fun `getPlaylistById returns playlist`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        
        val playlist = playlistDao.getPlaylistById(playlistId)
        
        assertNotNull(playlist)
        assertEquals("My Playlist", playlist?.name)
    }
    
    @Test
    fun `deletePlaylist removes playlist`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        
        playlistDao.deletePlaylist(playlistId)
        
        val playlist = playlistDao.getPlaylistById(playlistId)
        assertNull(playlist)
    }
    
    @Test
    fun `renamePlaylist updates name`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "Old Name"))
        
        playlistDao.renamePlaylist(playlistId, "New Name")
        
        val playlist = playlistDao.getPlaylistById(playlistId)
        assertEquals("New Name", playlist?.name)
    }
    
    @Test
    fun `addTrackToPlaylistAtEnd adds track with correct position`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        
        playlistDao.addTrackToPlaylistAtEnd(playlistId, 1)
        playlistDao.addTrackToPlaylistAtEnd(playlistId, 2)
        
        val tracks = playlistDao.getPlaylistTracks(playlistId).first()
        
        assertEquals(2, tracks.size)
        assertEquals(1L, tracks[0].id) // First added
        assertEquals(2L, tracks[1].id) // Second added
    }
    
    @Test
    fun `removeTrackFromPlaylist removes track`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        trackDao.insert(createTrackEntity(1))
        playlistDao.addTrackToPlaylistAtEnd(playlistId, 1)
        
        assertTrue(playlistDao.isTrackInPlaylist(playlistId, 1))
        
        playlistDao.removeTrackFromPlaylist(playlistId, 1)
        
        assertFalse(playlistDao.isTrackInPlaylist(playlistId, 1))
    }
    
    @Test
    fun `isTrackInPlaylist returns correct value`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        
        playlistDao.addTrackToPlaylistAtEnd(playlistId, 1)
        
        assertTrue(playlistDao.isTrackInPlaylist(playlistId, 1))
        assertFalse(playlistDao.isTrackInPlaylist(playlistId, 2))
    }
    
    @Test
    fun `deleting playlist cascades to playlist tracks`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        trackDao.insert(createTrackEntity(1))
        playlistDao.addTrackToPlaylistAtEnd(playlistId, 1)
        
        playlistDao.deletePlaylist(playlistId)
        
        // Playlist tracks should be deleted via cascade
        val tracks = playlistDao.getPlaylistTracks(playlistId).first()
        assertTrue(tracks.isEmpty())
    }
    
    @Test
    fun `deleting track cascades to playlist tracks`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        trackDao.insert(createTrackEntity(1))
        playlistDao.addTrackToPlaylistAtEnd(playlistId, 1)
        
        trackDao.deleteStaleEntries(emptyList())
        
        // Track removal should cascade to playlist_tracks
        assertFalse(playlistDao.isTrackInPlaylist(playlistId, 1))
    }
    
    @Test
    fun `getPlaylistTrackCount returns correct count`() = runTest {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = "My Playlist"))
        trackDao.insert(createTrackEntity(1))
        trackDao.insert(createTrackEntity(2))
        
        playlistDao.addTrackToPlaylistAtEnd(playlistId, 1)
        playlistDao.addTrackToPlaylistAtEnd(playlistId, 2)
        
        val count = playlistDao.getPlaylistTrackCount(playlistId).first()
        
        assertEquals(2, count)
    }
    
    private fun createTrackEntity(id: Long) = TrackEntity(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        duration = 180000L,
        path = "content://media/external/audio/media/$id"
    )
}

