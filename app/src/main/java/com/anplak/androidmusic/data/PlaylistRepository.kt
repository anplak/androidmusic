package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.PlaylistDao
import com.anplak.androidmusic.data.db.PlaylistEntity
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
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun renamePlaylist(playlistId: Long, name: String)
    fun getPlaylists(): Flow<List<Playlist>>
    fun getPlaylistById(playlistId: Long): Flow<Playlist?>
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
    fun getPlaylistTracks(playlistId: Long): Flow<List<TrackInfo>>
    suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean
}

class PlaylistRepositoryImpl(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.createPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        playlistDao.renamePlaylist(playlistId, name)
    }

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
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

    override fun getPlaylistTracks(playlistId: Long): Flow<List<TrackInfo>> {
        return playlistDao.getPlaylistTracks(playlistId).map { entities ->
            entities.map { it.toTrackInfo() }
        }
    }

    override suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean {
        return playlistDao.isTrackInPlaylist(playlistId, trackId)
    }
}

private fun PlaylistEntity.toPlaylist(): Playlist {
    return Playlist(
        id = id,
        name = name,
        createdAt = createdAt
    )
}

