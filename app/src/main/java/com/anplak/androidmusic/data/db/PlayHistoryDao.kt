package com.anplak.androidmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data class for track play count from aggregation query.
 */
data class TrackPlayCountResult(
    val trackId: Long,
    val playCount: Int
)

/**
 * Data class for artist play count from aggregation query.
 */
data class ArtistPlayCountResult(
    val artist: String,
    val playCount: Int
)

/**
 * Data class for play history with track details.
 */
data class PlayHistoryWithTrack(
    val id: Long,
    val trackId: Long,
    val playedAt: Long,
    val duration: Long,
    val sessionId: String?,
    val title: String,
    val artist: String,
    val album: String,
    val trackDuration: Long
)

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun insert(history: PlayHistoryEntity): Long

    @Update
    suspend fun update(history: PlayHistoryEntity)

    @Query("SELECT * FROM play_history WHERE id = :id")
    suspend fun getById(id: Long): PlayHistoryEntity?

    /**
     * Gets play history with track details, ordered by most recent first.
     */
    @Query("""
        SELECT ph.id, ph.trackId, ph.playedAt, ph.duration, ph.sessionId,
               t.title, t.artist, t.album, t.duration as trackDuration
        FROM play_history ph
        INNER JOIN tracks t ON ph.trackId = t.id
        ORDER BY ph.playedAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getHistory(limit: Int, offset: Int = 0): Flow<List<PlayHistoryWithTrack>>

    /**
     * Gets play history for a specific track.
     */
    @Query("""
        SELECT ph.id, ph.trackId, ph.playedAt, ph.duration, ph.sessionId,
               t.title, t.artist, t.album, t.duration as trackDuration
        FROM play_history ph
        INNER JOIN tracks t ON ph.trackId = t.id
        WHERE ph.trackId = :trackId
        ORDER BY ph.playedAt DESC
        LIMIT :limit
    """)
    fun getHistoryForTrack(trackId: Long, limit: Int): Flow<List<PlayHistoryWithTrack>>

    /**
     * Gets play history since a given timestamp.
     */
    @Query("""
        SELECT ph.id, ph.trackId, ph.playedAt, ph.duration, ph.sessionId,
               t.title, t.artist, t.album, t.duration as trackDuration
        FROM play_history ph
        INNER JOIN tracks t ON ph.trackId = t.id
        WHERE ph.playedAt >= :timestamp
        ORDER BY ph.playedAt DESC
    """)
    fun getHistorySince(timestamp: Long): Flow<List<PlayHistoryWithTrack>>

    /**
     * Gets total play time (sum of durations) since a given timestamp.
     */
    @Query("""
        SELECT COALESCE(SUM(duration), 0) FROM play_history
        WHERE playedAt >= :timestamp
    """)
    fun getTotalPlayTimeSince(timestamp: Long): Flow<Long>

    /**
     * Gets top tracks by play count since a given timestamp.
     */
    @Query("""
        SELECT trackId, COUNT(*) as playCount
        FROM play_history
        WHERE playedAt >= :timestamp
        GROUP BY trackId
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    fun getTopTracksSince(timestamp: Long, limit: Int): Flow<List<TrackPlayCountResult>>

    /**
     * Gets top artists by play count since a given timestamp.
     */
    @Query("""
        SELECT t.artist, COUNT(*) as playCount
        FROM play_history ph
        INNER JOIN tracks t ON ph.trackId = t.id
        WHERE ph.playedAt >= :timestamp
        GROUP BY t.artist
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    fun getTopArtistsSince(timestamp: Long, limit: Int): Flow<List<ArtistPlayCountResult>>

    /**
     * Gets the total count of history entries.
     */
    @Query("SELECT COUNT(*) FROM play_history")
    suspend fun getHistoryCount(): Int

    /**
     * Deletes history entries older than the given timestamp.
     * Used for data retention policy.
     */
    @Query("DELETE FROM play_history WHERE playedAt < :timestamp")
    suspend fun deleteHistoryOlderThan(timestamp: Long): Int

    /**
     * Deletes all play history entries.
     */
    @Query("DELETE FROM play_history")
    suspend fun deleteAll()
}
