package com.anplak.androidmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getById(trackId: Long): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    suspend fun getAll(): List<TrackEntity>
    
    @Query("SELECT * FROM tracks WHERE id IN (:trackIds)")
    suspend fun getByIds(trackIds: List<Long>): List<TrackEntity>
    
    @Query("SELECT * FROM tracks ORDER BY firstSeenAt DESC LIMIT :limit")
    fun getRecentlyAddedTracks(limit: Int): Flow<List<TrackEntity>>

    @Query("""
        SELECT * FROM tracks
        WHERE firstSeenAt >= :sinceMs
        ORDER BY firstSeenAt DESC
    """)
    suspend fun getTracksAddedSince(sinceMs: Long): List<TrackEntity>

    @Query("""
        SELECT * FROM tracks
        WHERE title LIKE '%' || :query || '%' COLLATE NOCASE
           OR artist LIKE '%' || :query || '%' COLLATE NOCASE
           OR album LIKE '%' || :query || '%' COLLATE NOCASE
           OR path LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY title ASC
        LIMIT :limit
    """)
    suspend fun searchTracks(query: String, limit: Int): List<TrackEntity>

    @Query("DELETE FROM tracks WHERE id NOT IN (:validIds)")
    suspend fun deleteStaleEntries(validIds: List<Long>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}

