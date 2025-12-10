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
    val completionCount: Int
) {
    val completionRatio: Float
        get() = if (playCount > 0) completionCount.toFloat() / playCount else 0f
}

interface TrackStatsRepository {
    /**
     * Records a play event for the track.
     * Increments play count and updates last played timestamp.
     */
    suspend fun recordPlay(trackId: Long)
    
    /**
     * Records a completion event for the track.
     * Only increments completion count (play count should have been incremented on start).
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
     * Useful for determining high play-count tracks for smart shuffle weighting.
     */
    suspend fun getAllStatsOrderedByPlayCount(): List<TrackStats>
}

class TrackStatsRepositoryImpl(
    private val trackStatsDao: TrackStatsDao
) : TrackStatsRepository {

    override suspend fun recordPlay(trackId: Long) {
        // Ensure stats entry exists
        trackStatsDao.insertIfNotExists(TrackStatsEntity(trackId = trackId))
        // Increment play count and update timestamp
        trackStatsDao.incrementPlayCount(trackId)
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

/**
 * Extension function to convert TrackStatsEntity to TrackStats.
 */
private fun TrackStatsEntity.toTrackStats(): TrackStats {
    return TrackStats(
        trackId = trackId,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        completionCount = completionCount
    )
}

