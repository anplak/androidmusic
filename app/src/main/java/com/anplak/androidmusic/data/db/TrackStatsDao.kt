package com.anplak.androidmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackStatsDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(stats: TrackStatsEntity)
    
    @Query("""
        UPDATE track_stats 
        SET playCount = playCount + 1, lastPlayedAt = :timestamp 
        WHERE trackId = :trackId
    """)
    suspend fun incrementPlayCount(trackId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE track_stats 
        SET completionCount = completionCount + 1 
        WHERE trackId = :trackId
    """)
    suspend fun incrementCompletionCount(trackId: Long)
    
    @Query("SELECT * FROM track_stats WHERE trackId = :trackId")
    suspend fun getStatsForTrack(trackId: Long): TrackStatsEntity?
    
    @Query("SELECT * FROM track_stats WHERE trackId = :trackId")
    fun observeStatsForTrack(trackId: Long): Flow<TrackStatsEntity?>
    
    @Query("""
        SELECT trackId FROM track_stats 
        WHERE playCount > 0 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getMostPlayedTrackIds(limit: Int): Flow<List<Long>>
    
    @Query("""
        SELECT trackId FROM track_stats 
        WHERE lastPlayedAt IS NOT NULL 
        ORDER BY lastPlayedAt DESC 
        LIMIT :limit
    """)
    fun getRecentlyPlayedTrackIds(limit: Int): Flow<List<Long>>
    
    @Query("SELECT * FROM track_stats WHERE playCount > 0 ORDER BY playCount DESC")
    suspend fun getAllStatsOrderedByPlayCount(): List<TrackStatsEntity>
    
    @Query("DELETE FROM track_stats WHERE trackId = :trackId")
    suspend fun deleteStats(trackId: Long)
    
    @Query("DELETE FROM track_stats")
    suspend fun deleteAll()
}

