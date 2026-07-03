package com.anplak.androidmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IndexArtistRuleDao {
    @Query("SELECT * FROM index_artist_rules ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<IndexArtistRuleEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: IndexArtistRuleEntity): Long

    @Query("DELETE FROM index_artist_rules WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM index_artist_rules")
    suspend fun deleteAll()
}
