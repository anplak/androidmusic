package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo

data class LibraryScanResult(
    val tracks: List<TrackInfo>,
    val indexedCount: Int,
    val skippedDurationCount: Int,
    val skippedFolderCount: Int
)
