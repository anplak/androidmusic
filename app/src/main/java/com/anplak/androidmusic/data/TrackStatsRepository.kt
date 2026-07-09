package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.TrackStatsDao
import com.anplak.androidmusic.data.db.TrackStatsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain model for track statistics.
 */
data class TrackStats(
    val trackId: Long,
    val playCount: Int,
    val lastPlayedAt: Long?,
    val completionCount: Int,
    val skipCount: Int = 0
) {
    val completionRatio: Float
        get() = if (playCount > 0) completionCount.toFloat() / playCount else 0f
}

interface TrackStatsRepository {
    /**
     * Records a qualified play event for the track.
     * Increments play count and updates last played timestamp.
     */
    suspend fun recordQualifiedPlay(trackId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Records a fast-skip negative signal without incrementing play count.
     */
    suspend fun recordSkip(trackId: Long)

    /**
     * Records a completion event for the track.
     * Only increments completion count (play count should have been incremented on qualify).
     */
    suspend fun recordCompletion(trackId: Long)

    /**
     * Gets stats for a single track.
     */
    suspend fun getStats(trackId: Long): TrackStats?

    /**
     * Observes stats for a single track.
     */
    fun observeStats(trackId: Long): Flow<TrackStats?>

    /**
     * Gets all stats ordered by play count (descending).
     */
    suspend fun getAllStatsOrderedByPlayCount(): List<TrackStats>
}

class TrackStatsRepositoryImpl(
    private val trackStatsDao: TrackStatsDao
) : TrackStatsRepository {

    override suspend fun recordQualifiedPlay(trackId: Long, timestamp: Long) {
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = trackId))
        trackStatsDao.incrementPlayCount(trackId, timestamp)
    }

    override suspend fun recordSkip(trackId: Long) {
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = trackId))
        trackStatsDao.incrementSkipCount(trackId)
    }

    override suspend fun recordCompletion(trackId: Long) {
        trackStatsDao.incrementCompletionCount(trackId)
    }

    override suspend fun getStats(trackId: Long): TrackStats? {
        return trackStatsDao.getStatsForTrack(trackId)?.toTrackStats()
    }

    override fun observeStats(trackId: Long): Flow<TrackStats?> {
        return trackStatsDao.observeStatsForTrack(trackId).map { it?.toTrackStats() }
    }

    override suspend fun getAllStatsOrderedByPlayCount(): List<TrackStats> {
        return trackStatsDao.getAllStatsOrderedByPlayCount().map { it.toTrackStats() }
    }
}

private fun TrackStatsEntity.toTrackStats(): TrackStats {
    return TrackStats(
        trackId = trackId,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        completionCount = completionCount,
        skipCount = skipCount
    )
}
