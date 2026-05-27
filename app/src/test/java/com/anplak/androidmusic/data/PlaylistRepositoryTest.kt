package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.PlaylistDao
import com.anplak.androidmusic.data.db.PlaylistEntity
import com.anplak.androidmusic.data.db.PlaylistTrackCrossRef
import com.anplak.androidmusic.data.db.PlaylistWithTrackCount
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
    fun `getPlaylists converts entities to domain models with track count`() = runTest {
        val entities = listOf(
            PlaylistEntity(1, "Playlist 1", 1000L),
            PlaylistEntity(2, "Playlist 2", 2000L)
        )
        fakePlaylistDao.setPlaylists(entities)
        // Set track count for playlists
        val tracks = listOf(
            TrackEntity(1, "Track 1", "Artist", "Album", 180000, "path1", System.currentTimeMillis()),
            TrackEntity(2, "Track 2", "Artist", "Album", 200000, "path2", System.currentTimeMillis())
        )
        fakePlaylistDao.setPlaylistTracks(tracks)
        
        val playlists = repository.getPlaylists().first()
        
        assertEquals(2, playlists.size)
        assertEquals("Playlist 1", playlists[0].name)
        assertEquals("Playlist 2", playlists[1].name)
        // Verify track count is populated
        assertEquals(2, playlists[0].trackCount)
        assertEquals(2, playlists[1].trackCount)
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
    fun `createPlaylistWithTracks inserts tracks in order`() = runTest {
        fakePlaylistDao.setCreatedPlaylistId(10L)
        
        val playlistId = repository.createPlaylistWithTracks("Mix", listOf(4L, 2L, 7L))
        
        assertEquals(10L, playlistId)
        assertTrue(fakePlaylistDao.addTracksCalled)
        val refs = fakePlaylistDao.getPlaylistTrackRefs(10L)
        assertEquals(listOf(4L, 2L, 7L), refs.map { it.trackId })
        assertEquals(listOf(0, 1, 2), refs.map { it.position })
    }

    @Test
    fun `removeTracksFromPlaylist calls DAO with ids`() = runTest {
        repository.removeTracksFromPlaylist(3L, listOf(1L, 2L))
        
        assertTrue(fakePlaylistDao.removeTracksCalled)
        assertEquals(3L, fakePlaylistDao.lastRemovedFromPlaylistId)
        assertEquals(listOf(1L, 2L), fakePlaylistDao.lastRemovedTrackIds)
    }

    @Test
    fun `reorderPlaylistTracks updates positions`() = runTest {
        fakePlaylistDao.setPlaylistTrackRefs(
            listOf(
                PlaylistTrackCrossRef(5L, 1L, 0),
                PlaylistTrackCrossRef(5L, 2L, 1),
                PlaylistTrackCrossRef(5L, 3L, 2)
            )
        )
        
        repository.reorderPlaylistTracks(5L, listOf(3L, 1L, 2L))
        
        val reordered = fakePlaylistDao.getPlaylistTrackRefs(5L)
        assertEquals(listOf(3L, 1L, 2L), reordered.map { it.trackId })
        assertEquals(listOf(0, 1, 2), reordered.map { it.position })
    }

    @Test
    fun `duplicatePlaylist copies tracks into new playlist`() = runTest {
        fakePlaylistDao.setCreatedPlaylistId(8L)
        fakePlaylistDao.setPlaylistTrackRefs(
            listOf(
                PlaylistTrackCrossRef(2L, 11L, 0, 100L),
                PlaylistTrackCrossRef(2L, 12L, 1, 200L)
            )
        )
        
        val newId = repository.duplicatePlaylist(2L, "Copy")
        
        assertEquals(8L, newId)
        val copied = fakePlaylistDao.getPlaylistTrackRefs(8L)
        assertEquals(listOf(11L, 12L), copied.map { it.trackId })
    }

    @Test
    fun `mergePlaylists dedupes tracks and preserves order`() = runTest {
        fakePlaylistDao.setCreatedPlaylistId(9L)
        fakePlaylistDao.setPlaylistTrackRefs(
            listOf(
                PlaylistTrackCrossRef(1L, 1L, 0, 100L),
                PlaylistTrackCrossRef(1L, 2L, 1, 200L),
                PlaylistTrackCrossRef(2L, 2L, 0, 300L),
                PlaylistTrackCrossRef(2L, 3L, 1, 400L)
            )
        )
        
        val mergedId = repository.mergePlaylists(1L, 2L, "Merged")
        
        assertEquals(9L, mergedId)
        val merged = fakePlaylistDao.getPlaylistTrackRefs(9L)
        assertEquals(listOf(1L, 2L, 3L), merged.map { it.trackId })
        assertEquals(listOf(0, 1, 2), merged.map { it.position })
    }
    
    @Test
    fun `getPlaylistTracks converts entities to TrackInfo`() = runTest {
        val tracks = listOf(
            TrackEntity(1, "Track 1", "Artist", "Album", 180000, "path1", System.currentTimeMillis()),
            TrackEntity(2, "Track 2", "Artist", "Album", 200000, "path2", System.currentTimeMillis())
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
    private val playlistTrackRefs = mutableListOf<PlaylistTrackCrossRef>()
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
    var addTracksCalled = false
        private set
    var removeTrackCalled = false
        private set
    var removeTracksCalled = false
        private set
    var lastRemovedFromPlaylistId: Long? = null
        private set
    var lastRemovedTrackId: Long? = null
        private set
    var lastRemovedTrackIds: List<Long> = emptyList()
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

    fun setPlaylistTrackRefs(refs: List<PlaylistTrackCrossRef>) {
        playlistTrackRefs.clear()
        playlistTrackRefs.addAll(refs)
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
    
    override fun getAllPlaylistsWithTrackCount(): Flow<List<PlaylistWithTrackCount>> {
        return MutableStateFlow(playlists.value.map { playlist ->
            PlaylistWithTrackCount(
                id = playlist.id,
                name = playlist.name,
                createdAt = playlist.createdAt,
                trackCount = playlistTracks.value.size
            )
        })
    }
    
    override suspend fun getPlaylistById(playlistId: Long): PlaylistEntity? = playlistById.value
    
    override fun getPlaylistByIdFlow(playlistId: Long): Flow<PlaylistEntity?> = playlistById
    
    override suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef) {
        addTrackCalled = true
        playlistTrackRefs.add(crossRef)
    }

    override suspend fun addTracksToPlaylist(crossRefs: List<PlaylistTrackCrossRef>) {
        addTracksCalled = true
        playlistTrackRefs.addAll(crossRefs)
    }
    
    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        removeTrackCalled = true
        lastRemovedFromPlaylistId = playlistId
        lastRemovedTrackId = trackId
        playlistTrackRefs.removeAll { it.playlistId == playlistId && it.trackId == trackId }
    }

    override suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>) {
        removeTracksCalled = true
        lastRemovedFromPlaylistId = playlistId
        lastRemovedTrackIds = trackIds
        playlistTrackRefs.removeAll { it.playlistId == playlistId && it.trackId in trackIds }
    }

    override suspend fun clearPlaylistTracks(playlistId: Long) {
        playlistTrackRefs.removeAll { it.playlistId == playlistId }
    }
    
    override fun getPlaylistTracks(playlistId: Long): Flow<List<TrackEntity>> = playlistTracks
    
    override suspend fun getPlaylistTrackRefs(playlistId: Long): List<PlaylistTrackCrossRef> {
        return playlistTrackRefs.filter { it.playlistId == playlistId }.sortedBy { it.position }
    }

    override suspend fun getPlaylistTrackIds(playlistId: Long): List<Long> {
        return playlistTrackRefs.filter { it.playlistId == playlistId }.sortedBy { it.position }.map { it.trackId }
    }

    override suspend fun getMaxPosition(playlistId: Long): Int? = 0
    
    override fun getPlaylistTrackCount(playlistId: Long): Flow<Int> = MutableStateFlow(playlistTracks.value.size)
    
    override suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean = trackInPlaylist
    
    override suspend fun addTrackToPlaylistAtEnd(playlistId: Long, trackId: Long) {
        addTrackCalled = true
    }

    override suspend fun searchPlaylists(query: String, limit: Int): List<PlaylistWithTrackCount> =
        emptyList()
}

