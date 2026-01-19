package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.PlaylistDao
import com.anplak.androidmusic.data.db.PlaylistEntity
import com.anplak.androidmusic.data.db.PlaylistTrackCrossRef
import com.anplak.androidmusic.data.db.PlaylistWithTrackCount
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain model for a playlist.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val trackCount: Int = 0
)

interface PlaylistRepository {
    suspend fun createPlaylist(name: String): Long
    suspend fun createPlaylistWithTracks(name: String, trackIds: List<Long>): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun renamePlaylist(playlistId: Long, name: String)
    fun getPlaylists(): Flow<List<Playlist>>
    fun getPlaylistById(playlistId: Long): Flow<Playlist?>
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>)
    suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>)
    suspend fun duplicatePlaylist(sourcePlaylistId: Long, name: String): Long
    suspend fun mergePlaylists(primaryPlaylistId: Long, secondaryPlaylistId: Long, name: String): Long
    fun getPlaylistTracks(playlistId: Long): Flow<List<TrackInfo>>
    suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean
}

class PlaylistRepositoryImpl(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.createPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun createPlaylistWithTracks(name: String, trackIds: List<Long>): Long {
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = name))
        insertTracksAtPositions(playlistId, trackIds, emptyMap())
        return playlistId
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        playlistDao.renamePlaylist(playlistId, name)
    }

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsWithTrackCount().map { entities ->
            entities.map { it.toPlaylist() }
        }
    }

    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistByIdFlow(playlistId).map { entity ->
            entity?.toPlaylist()
        }
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        if (!playlistDao.isTrackInPlaylist(playlistId, trackId)) {
            playlistDao.addTrackToPlaylistAtEnd(playlistId, trackId)
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    override suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>) {
        if (trackIds.isEmpty()) return
        playlistDao.removeTracksFromPlaylist(playlistId, trackIds)
    }

    override suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>) {
        playlistDao.replacePlaylistTracks(playlistId, orderedTrackIds)
    }

    override suspend fun duplicatePlaylist(sourcePlaylistId: Long, name: String): Long {
        val refs = playlistDao.getPlaylistTrackRefs(sourcePlaylistId)
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = name))
        insertTracksAtPositions(
            playlistId = playlistId,
            trackIds = refs.map { it.trackId },
            addedAtByTrackId = refs.associate { it.trackId to it.addedAt }
        )
        return playlistId
    }

    override suspend fun mergePlaylists(
        primaryPlaylistId: Long,
        secondaryPlaylistId: Long,
        name: String
    ): Long {
        val primaryRefs = playlistDao.getPlaylistTrackRefs(primaryPlaylistId)
        val secondaryRefs = playlistDao.getPlaylistTrackRefs(secondaryPlaylistId)
        val mergedAddedAt = LinkedHashMap<Long, Long>()
        (primaryRefs + secondaryRefs).forEach { ref ->
            if (ref.trackId !in mergedAddedAt) {
                mergedAddedAt[ref.trackId] = ref.addedAt
            }
        }
        val playlistId = playlistDao.createPlaylist(PlaylistEntity(name = name))
        insertTracksAtPositions(
            playlistId = playlistId,
            trackIds = mergedAddedAt.keys.toList(),
            addedAtByTrackId = mergedAddedAt
        )
        return playlistId
    }

    override fun getPlaylistTracks(playlistId: Long): Flow<List<TrackInfo>> {
        return playlistDao.getPlaylistTracks(playlistId).map { entities ->
            entities.map { it.toTrackInfo() }
        }
    }

    override suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean {
        return playlistDao.isTrackInPlaylist(playlistId, trackId)
    }

    private suspend fun insertTracksAtPositions(
        playlistId: Long,
        trackIds: List<Long>,
        addedAtByTrackId: Map<Long, Long>
    ) {
        if (trackIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val refs = trackIds.mapIndexed { index, trackId ->
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = index,
                addedAt = addedAtByTrackId[trackId] ?: now
            )
        }
        playlistDao.addTracksToPlaylist(refs)
    }
}

private fun PlaylistWithTrackCount.toPlaylist(): Playlist {
    return Playlist(
        id = id,
        name = name,
        createdAt = createdAt,
        trackCount = trackCount
    )
}

private fun PlaylistEntity.toPlaylist(): Playlist {
    return Playlist(
        id = id,
        name = name,
        createdAt = createdAt
    )
}
