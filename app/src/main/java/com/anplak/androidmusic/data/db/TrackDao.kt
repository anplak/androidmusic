package com.anplak.androidmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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

    @Query("DELETE FROM tracks WHERE id NOT IN (:validIds)")
    suspend fun deleteStaleEntries(validIds: List<Long>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}

