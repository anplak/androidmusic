package com.anplak.androidmusic.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "index_folder_rules")
data class IndexFolderRuleEntity(
    @PrimaryKey
    val path: String,
    val mode: String
)
