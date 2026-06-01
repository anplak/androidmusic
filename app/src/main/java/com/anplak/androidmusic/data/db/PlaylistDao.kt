package com.anplak.androidmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data class for playlist with track count from JOIN query.
 */
data class PlaylistWithTrackCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val trackCount: Int
)

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    
    @Query("""
        SELECT p.id, p.name, p.createdAt, COUNT(pt.trackId) as trackCount
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    fun getAllPlaylistsWithTrackCount(): Flow<List<PlaylistWithTrackCount>>

    @Query("""
        SELECT DISTINCT p.id, p.name, p.createdAt,
            (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId
        LEFT JOIN tracks t ON pt.trackId = t.id
        WHERE p.name LIKE '%' || :query || '%' COLLATE NOCASE
           OR t.title LIKE '%' || :query || '%' COLLATE NOCASE
           OR t.artist LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY p.createdAt DESC
        LIMIT :limit
    """)
    suspend fun searchPlaylists(query: String, limit: Int): List<PlaylistWithTrackCount>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistByIdFlow(playlistId: Long): Flow<PlaylistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTracksToPlaylist(crossRefs: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId IN (:trackIds)")
    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
    """)
    fun getPlaylistTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query("""
        SELECT * FROM playlist_tracks
        WHERE playlistId = :playlistId
        ORDER BY position ASC
    """)
    suspend fun getPlaylistTrackRefs(playlistId: Long): List<PlaylistTrackCrossRef>

    @Query("""
        SELECT trackId FROM playlist_tracks
        WHERE playlistId = :playlistId
        ORDER BY position ASC
    """)
    suspend fun getPlaylistTrackIds(playlistId: Long): List<Long>

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCount(playlistId: Long): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId)")
    suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean

    @Transaction
    suspend fun addTrackToPlaylistAtEnd(playlistId: Long, trackId: Long) {
        val maxPosition = getMaxPosition(playlistId) ?: -1
        addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = maxPosition + 1
            )
        )
    }

    @Transaction
    suspend fun replacePlaylistTracks(
        playlistId: Long,
        orderedTrackIds: List<Long>
    ) {
        val existing = getPlaylistTrackRefs(playlistId).associateBy { it.trackId }
        clearPlaylistTracks(playlistId)
        val newRefs = orderedTrackIds.mapIndexed { index, trackId ->
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = index,
                addedAt = existing[trackId]?.addedAt ?: System.currentTimeMillis()
            )
        }
        if (newRefs.isNotEmpty()) {
            addTracksToPlaylist(newRefs)
        }
    }
}

