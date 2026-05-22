package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.PlayHistoryDao
import com.anplak.androidmusic.data.db.PlayHistoryEntity
import com.anplak.androidmusic.data.db.PlayHistoryWithTrack
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain model for a play history entry with track details.
 */
data class PlayHistoryEntry(
    val id: Long,
    val trackId: Long,
    val playedAt: Long,
    val duration: Long,
    val sessionId: String?,
    val track: TrackInfo
)

/**
 * Domain model for track play count statistics.
 */
data class TrackPlayCount(
    val trackId: Long,
    val playCount: Int
)

/**
 * Domain model for artist play count statistics.
 */
data class ArtistPlayCount(
    val artist: String,
    val playCount: Int
)

interface PlayHistoryRepository {
    /**
     * Records a play event for a track.
     * Returns the ID of the created history entry for later update.
     */
    suspend fun recordPlay(trackId: Long, sessionId: String? = null): Long

    /**
     * Updates the duration of a play history entry.
     * Called when playback completes or moves to another track.
     */
    suspend fun updateDuration(historyId: Long, duration: Long)

    /**
     * Gets play history with pagination, ordered by most recent first.
     */
    fun getHistory(limit: Int, offset: Int = 0): Flow<List<PlayHistoryEntry>>

    /**
     * Gets play history for a specific track.
     */
    fun getHistoryForTrack(trackId: Long, limit: Int = 50): Flow<List<PlayHistoryEntry>>

    /**
     * Gets play history since a given timestamp.
     */
    fun getHistorySince(timestamp: Long): Flow<List<PlayHistoryEntry>>

    /**
     * Gets total play time in milliseconds since a given timestamp.
     */
    fun getTotalPlayTimeSince(timestamp: Long): Flow<Long>

    /**
     * Gets top tracks by play count since a given timestamp.
     */
    fun getTopTracksSince(timestamp: Long, limit: Int): Flow<List<TrackPlayCount>>

    /**
     * Gets top artists by play count since a given timestamp.
     */
    fun getTopArtistsSince(timestamp: Long, limit: Int): Flow<List<ArtistPlayCount>>

    /**
     * Gets total count of history entries.
     */
    suspend fun getHistoryCount(): Int

    /**
     * Deletes history entries older than the specified number of days.
     * Returns the number of entries deleted.
     */
    suspend fun cleanupOldHistory(retentionDays: Int): Int

    /**
     * Track IDs frequently played in the same session as [seedTrackId].
     */
    suspend fun getCoPlayedTrackIds(seedTrackId: Long, limit: Int): List<Long>

    /**
     * Track IDs from the user's most recent listening session.
     */
    suspend fun getLastSessionTrackIds(limit: Int): List<Long>
}

class PlayHistoryRepositoryImpl(
    private val playHistoryDao: PlayHistoryDao
) : PlayHistoryRepository {

    override suspend fun recordPlay(trackId: Long, sessionId: String?): Long {
        val entity = PlayHistoryEntity(
            trackId = trackId,
            playedAt = System.currentTimeMillis(),
            duration = 0,
            sessionId = sessionId
        )
        return playHistoryDao.insert(entity)
    }

    override suspend fun updateDuration(historyId: Long, duration: Long) {
        val existing = playHistoryDao.getById(historyId) ?: return
        playHistoryDao.update(existing.copy(duration = duration))
    }

    override fun getHistory(limit: Int, offset: Int): Flow<List<PlayHistoryEntry>> {
        return playHistoryDao.getHistory(limit, offset).map { entries ->
            entries.map { it.toPlayHistoryEntry() }
        }
    }

    override fun getHistoryForTrack(trackId: Long, limit: Int): Flow<List<PlayHistoryEntry>> {
        return playHistoryDao.getHistoryForTrack(trackId, limit).map { entries ->
            entries.map { it.toPlayHistoryEntry() }
        }
    }

    override fun getHistorySince(timestamp: Long): Flow<List<PlayHistoryEntry>> {
        return playHistoryDao.getHistorySince(timestamp).map { entries ->
            entries.map { it.toPlayHistoryEntry() }
        }
    }

    override fun getTotalPlayTimeSince(timestamp: Long): Flow<Long> {
        return playHistoryDao.getTotalPlayTimeSince(timestamp)
    }

    override fun getTopTracksSince(timestamp: Long, limit: Int): Flow<List<TrackPlayCount>> {
        return playHistoryDao.getTopTracksSince(timestamp, limit).map { results ->
            results.map { TrackPlayCount(trackId = it.trackId, playCount = it.playCount) }
        }
    }

    override fun getTopArtistsSince(timestamp: Long, limit: Int): Flow<List<ArtistPlayCount>> {
        return playHistoryDao.getTopArtistsSince(timestamp, limit).map { results ->
            results.map { ArtistPlayCount(artist = it.artist, playCount = it.playCount) }
        }
    }

    override suspend fun getHistoryCount(): Int {
        return playHistoryDao.getHistoryCount()
    }

    override suspend fun cleanupOldHistory(retentionDays: Int): Int {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        return playHistoryDao.deleteHistoryOlderThan(cutoffTime)
    }

    override suspend fun getCoPlayedTrackIds(seedTrackId: Long, limit: Int): List<Long> {
        return playHistoryDao.getCoPlayedTrackIds(seedTrackId, limit).map { it.trackId }
    }

    override suspend fun getLastSessionTrackIds(limit: Int): List<Long> {
        return playHistoryDao.getLastSessionTrackIds(limit)
    }
}

/**
 * Extension function to convert PlayHistoryWithTrack to PlayHistoryEntry.
 */
private fun PlayHistoryWithTrack.toPlayHistoryEntry(): PlayHistoryEntry {
    return PlayHistoryEntry(
        id = id,
        trackId = trackId,
        playedAt = playedAt,
        duration = duration,
        sessionId = sessionId,
        track = TrackInfo(
            uri = TrackInfo.uriFromId(trackId),
            title = title,
            artist = artist,
            album = album,
            duration = trackDuration
        )
    )
}
