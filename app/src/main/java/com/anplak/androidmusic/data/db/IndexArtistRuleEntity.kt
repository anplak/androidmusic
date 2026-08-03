package com.anplak.androidmusic.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "index_artist_rules")
data class IndexArtistRuleEntity(
    @PrimaryKey
    val name: String,
)
