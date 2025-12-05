package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.PlaylistDao
import com.anplak.androidmusic.data.db.PlaylistEntity
import com.anplak.androidmusic.data.db.PlaylistTrackCrossRef
import com.anplak.androidmusic.data.db.TrackEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaylistRepositoryTest {
    
    private lateinit var fakePlaylistDao: FakePlaylistDao
    private lateinit var repository: PlaylistRepository
    
    @Before
    fun setup() {
        fakePlaylistDao = FakePlaylistDao()
        repository = PlaylistRepositoryImpl(fakePlaylistDao)
    }
    
    @Test
    fun `createPlaylist calls DAO and returns ID`() = runTest {
        fakePlaylistDao.setCreatedPlaylistId(42L)
        
        val id = repository.createPlaylist("My Playlist")
        
        assertEquals(42L, id)
        assertTrue(fakePlaylistDao.createPlaylistCalled)
        assertEquals("My Playlist", fakePlaylistDao.lastCreatedPlaylistName)
    }
    
    @Test
    fun `deletePlaylist calls DAO`() = runTest {
        repository.deletePlaylist(1L)
        
        assertTrue(fakePlaylistDao.deletePlaylistCalled)
        assertEquals(1L, fakePlaylistDao.lastDeletedPlaylistId)
    }
    
    @Test
    fun `renamePlaylist calls DAO`() = runTest {
        repository.renamePlaylist(1L, "New Name")
        
        assertTrue(fakePlaylistDao.renamePlaylistCalled)
        assertEquals(1L, fakePlaylistDao.lastRenamedPlaylistId)
        assertEquals("New Name", fakePlaylistDao.lastRenamedPlaylistName)
    }
    
    @Test
    fun `getPlaylists converts entities to domain models`() = runTest {
        val entities = listOf(
            PlaylistEntity(1, "Playlist 1", 1000L),
            PlaylistEntity(2, "Playlist 2", 2000L)
        )
        fakePlaylistDao.setPlaylists(entities)
        
        val playlists = repository.getPlaylists().first()
        
        assertEquals(2, playlists.size)
        assertEquals("Playlist 1", playlists[0].name)
        assertEquals("Playlist 2", playlists[1].name)
    }
    
    @Test
    fun `getPlaylistById returns playlist from DAO`() = runTest {
        val entity = PlaylistEntity(1, "My Playlist", 1000L)
        fakePlaylistDao.setPlaylistById(entity)
        
        val playlist = repository.getPlaylistById(1L).first()
        
        assertEquals("My Playlist", playlist?.name)
    }
    
    @Test
    fun `getPlaylistById returns null when not found`() = runTest {
        fakePlaylistDao.setPlaylistById(null)
        
        val playlist = repository.getPlaylistById(1L).first()
        
        assertNull(playlist)
    }
    
    @Test
    fun `addTrackToPlaylist calls DAO when track not in playlist`() = runTest {
        fakePlaylistDao.setTrackInPlaylist(false)
        
        repository.addTrackToPlaylist(1L, 2L)
        
        assertTrue(fakePlaylistDao.addTrackCalled)
    }
    
    @Test
    fun `addTrackToPlaylist does not add duplicate`() = runTest {
        fakePlaylistDao.setTrackInPlaylist(true)
        
        repository.addTrackToPlaylist(1L, 2L)
        
        assertFalse(fakePlaylistDao.addTrackCalled)
    }
    
    @Test
    fun `removeTrackFromPlaylist calls DAO`() = runTest {
        repository.removeTrackFromPlaylist(1L, 2L)
        
        assertTrue(fakePlaylistDao.removeTrackCalled)
        assertEquals(1L, fakePlaylistDao.lastRemovedFromPlaylistId)
        assertEquals(2L, fakePlaylistDao.lastRemovedTrackId)
    }
    
    @Test
    fun `getPlaylistTracks converts entities to TrackInfo`() = runTest {
        val tracks = listOf(
            TrackEntity(1, "Track 1", "Artist", "Album", 180000, "path1"),
            TrackEntity(2, "Track 2", "Artist", "Album", 200000, "path2")
        )
        fakePlaylistDao.setPlaylistTracks(tracks)
        
        val result = repository.getPlaylistTracks(1L).first()
        
        assertEquals(2, result.size)
        assertEquals("Track 1", result[0].title)
        assertEquals("Track 2", result[1].title)
    }
    
    @Test
    fun `isTrackInPlaylist returns value from DAO`() = runTest {
        fakePlaylistDao.setTrackInPlaylist(true)
        
        assertTrue(repository.isTrackInPlaylist(1L, 2L))
        
        fakePlaylistDao.setTrackInPlaylist(false)
        
        assertFalse(repository.isTrackInPlaylist(1L, 2L))
    }
}

class FakePlaylistDao : PlaylistDao {
    private var createdPlaylistId = 1L
    private val playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    private val playlistById = MutableStateFlow<PlaylistEntity?>(null)
    private val playlistTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    private var trackInPlaylist = false
    
    var createPlaylistCalled = false
        private set
    var lastCreatedPlaylistName: String? = null
        private set
    var deletePlaylistCalled = false
        private set
    var lastDeletedPlaylistId: Long? = null
        private set
    var renamePlaylistCalled = false
        private set
    var lastRenamedPlaylistId: Long? = null
        private set
    var lastRenamedPlaylistName: String? = null
        private set
    var addTrackCalled = false
        private set
    var removeTrackCalled = false
        private set
    var lastRemovedFromPlaylistId: Long? = null
        private set
    var lastRemovedTrackId: Long? = null
        private set
    
    fun setCreatedPlaylistId(id: Long) {
        createdPlaylistId = id
    }
    
    fun setPlaylists(list: List<PlaylistEntity>) {
        playlists.value = list
    }
    
    fun setPlaylistById(entity: PlaylistEntity?) {
        playlistById.value = entity
    }
    
    fun setPlaylistTracks(tracks: List<TrackEntity>) {
        playlistTracks.value = tracks
    }
    
    fun setTrackInPlaylist(value: Boolean) {
        trackInPlaylist = value
    }
    
    override suspend fun createPlaylist(playlist: PlaylistEntity): Long {
        createPlaylistCalled = true
        lastCreatedPlaylistName = playlist.name
        return createdPlaylistId
    }
    
    override suspend fun deletePlaylist(playlistId: Long) {
        deletePlaylistCalled = true
        lastDeletedPlaylistId = playlistId
    }
    
    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        renamePlaylistCalled = true
        lastRenamedPlaylistId = playlistId
        lastRenamedPlaylistName = name
    }
    
    override fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlists
    
    override suspend fun getPlaylistById(playlistId: Long): PlaylistEntity? = playlistById.value
    
    override fun getPlaylistByIdFlow(playlistId: Long): Flow<PlaylistEntity?> = playlistById
    
    override suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef) {
        addTrackCalled = true
    }
    
    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        removeTrackCalled = true
        lastRemovedFromPlaylistId = playlistId
        lastRemovedTrackId = trackId
    }
    
    override fun getPlaylistTracks(playlistId: Long): Flow<List<TrackEntity>> = playlistTracks
    
    override suspend fun getMaxPosition(playlistId: Long): Int? = 0
    
    override fun getPlaylistTrackCount(playlistId: Long): Flow<Int> = MutableStateFlow(playlistTracks.value.size)
    
    override suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean = trackInPlaylist
    
    override suspend fun addTrackToPlaylistAtEnd(playlistId: Long, trackId: Long) {
        addTrackCalled = true
    }
}

