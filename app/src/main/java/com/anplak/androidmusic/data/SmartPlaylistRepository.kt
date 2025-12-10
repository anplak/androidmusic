package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.data.db.TrackStatsDao
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Types of smart playlists available.
 */
enum class SmartPlaylistType {
    MOST_PLAYED,
    RECENTLY_PLAYED,
    RECENTLY_ADDED
}

interface SmartPlaylistRepository {
    /**
     * Gets the most played tracks ordered by play count descending.
     */
    fun getMostPlayed(limit: Int = 25): Flow<List<TrackInfo>>
    
    /**
     * Gets recently played tracks ordered by last played timestamp descending.
     */
    fun getRecentlyPlayed(limit: Int = 25): Flow<List<TrackInfo>>
    
    /**
     * Gets recently added tracks ordered by first seen timestamp descending.
     */
    fun getRecentlyAdded(limit: Int = 25): Flow<List<TrackInfo>>
    
    /**
     * Gets tracks for a specific smart playlist type.
     */
    fun getTracksForType(type: SmartPlaylistType, limit: Int = 25): Flow<List<TrackInfo>>
}

class SmartPlaylistRepositoryImpl(
    private val trackDao: TrackDao,
    private val trackStatsDao: TrackStatsDao
) : SmartPlaylistRepository {

    override fun getMostPlayed(limit: Int): Flow<List<TrackInfo>> {
        return trackStatsDao.getMostPlayedTrackIds(limit).map { trackIds ->
            if (trackIds.isEmpty()) {
                emptyList()
            } else {
                val tracks = trackDao.getByIds(trackIds)
                // Maintain the order from stats query (most played first)
                val trackMap = tracks.associateBy { it.id }
                trackIds.mapNotNull { id -> trackMap[id]?.toTrackInfo() }
            }
        }
    }

    override fun getRecentlyPlayed(limit: Int): Flow<List<TrackInfo>> {
        return trackStatsDao.getRecentlyPlayedTrackIds(limit).map { trackIds ->
            if (trackIds.isEmpty()) {
                emptyList()
            } else {
                val tracks = trackDao.getByIds(trackIds)
                // Maintain the order from stats query (most recent first)
                val trackMap = tracks.associateBy { it.id }
                trackIds.mapNotNull { id -> trackMap[id]?.toTrackInfo() }
            }
        }
    }

    override fun getRecentlyAdded(limit: Int): Flow<List<TrackInfo>> {
        return trackDao.getRecentlyAddedTracks(limit).map { tracks ->
            tracks.map { it.toTrackInfo() }
        }
    }

    override fun getTracksForType(type: SmartPlaylistType, limit: Int): Flow<List<TrackInfo>> {
        return when (type) {
            SmartPlaylistType.MOST_PLAYED -> getMostPlayed(limit)
            SmartPlaylistType.RECENTLY_PLAYED -> getRecentlyPlayed(limit)
            SmartPlaylistType.RECENTLY_ADDED -> getRecentlyAdded(limit)
        }
    }
}

