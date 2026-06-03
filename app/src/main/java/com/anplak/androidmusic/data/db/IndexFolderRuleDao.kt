package com.anplak.androidmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IndexFolderRuleDao {
    @Query("SELECT * FROM index_folder_rules ORDER BY path ASC")
    suspend fun getAll(): List<IndexFolderRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: IndexFolderRuleEntity)

    @Query("DELETE FROM index_folder_rules WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM index_folder_rules")
    suspend fun deleteAll()
}
