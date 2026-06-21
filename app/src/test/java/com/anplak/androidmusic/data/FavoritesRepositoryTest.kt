package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.FavoriteDao
import com.anplak.androidmusic.data.db.FavoriteEntity
import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.data.db.TrackEntity
import com.anplak.androidmusic.player.TrackInfo
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
    private lateinit var fakeTrackDao: FakeTrackDao
    private lateinit var repository: FavoritesRepository

    @Before
    fun setup() {
        fakeFavoriteDao = FakeFavoriteDao()
        fakeTrackDao = FakeTrackDao()
        repository = FavoritesRepositoryImpl(fakeFavoriteDao, fakeTrackDao)
    }

    @Test
    fun `toggleFavorite with TrackInfo upserts track and adds favorite`() = runTest {
        val track = createTrack(1L, "Song")

        repository.toggleFavorite(track)

        assertTrue(fakeTrackDao.insertedTracks.contains(1L))
        assertTrue(fakeFavoriteDao.addFavoriteCalled)
        assertEquals(1L, fakeFavoriteDao.lastAddedFavoriteId)
    }

    @Test
    fun `toggleFavorite with TrackInfo removes favorite when already favorited`() = runTest {
        fakeFavoriteDao.setIsFavorite(true)
        val track = createTrack(1L, "Song")

        repository.toggleFavorite(track)

        assertTrue(fakeFavoriteDao.removeFavoriteCalled)
        assertEquals(1L, fakeFavoriteDao.lastRemovedFavoriteId)
    }

    @Test
    fun `toggleFavorite by id uses cached track when available`() = runTest {
        fakeTrackDao.tracks[1L] = createTrackEntity(1L)
        fakeFavoriteDao.setIsFavorite(false)

        repository.toggleFavorite(1L)

        assertTrue(fakeFavoriteDao.addFavoriteCalled)
    }

    @Test
    fun `toggleFavorite by id removes favorite when track missing from cache`() = runTest {
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
            createTrackEntity(1L),
            createTrackEntity(2L)
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

    private fun createTrack(id: Long, title: String): TrackInfo {
        return TrackInfo(
            uri = TrackInfo.uriFromId(id),
            title = title,
            artist = "Artist",
            album = "Album",
            duration = 180_000L
        )
    }

    private fun createTrackEntity(id: Long) = TrackEntity(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        path = "/music/track$id.mp3"
    )
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

    override fun getAllFavoriteIds(): Flow<List<Long>> =
        MutableStateFlow(favoriteIds.value.toList())

    override fun getFavoriteCount(): Flow<Int> = MutableStateFlow(favoriteIds.value.size)
}

class FakeTrackDao : TrackDao {
    val insertedTracks = mutableSetOf<Long>()
    val tracks = mutableMapOf<Long, TrackEntity>()

    override suspend fun insertAll(tracks: List<TrackEntity>) {
        tracks.forEach { insert(it) }
    }

    override suspend fun insert(track: TrackEntity) {
        insertedTracks.add(track.id)
        this.tracks[track.id] = track
    }

    override suspend fun getById(trackId: Long): TrackEntity? = tracks[trackId]

    override suspend fun getAll(): List<TrackEntity> = tracks.values.toList()

    override fun observeAll(): Flow<List<TrackEntity>> = MutableStateFlow(tracks.values.toList())

    override suspend fun getByIds(trackIds: List<Long>): List<TrackEntity> =
        trackIds.mapNotNull { tracks[it] }

    override fun getRecentlyAddedTracks(limit: Int): Flow<List<TrackEntity>> =
        MutableStateFlow(emptyList())

    override suspend fun getTracksAddedSince(sinceMs: Long): List<TrackEntity> = emptyList()

    override suspend fun searchTracks(query: String, limit: Int): List<TrackEntity> = emptyList()

    override suspend fun deleteStaleEntries(validIds: List<Long>) = Unit

    override suspend fun deleteAll() = Unit
}
