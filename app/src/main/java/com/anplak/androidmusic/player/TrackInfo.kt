package com.anplak.androidmusic.player

import android.net.Uri

data class TrackInfo(
    val uri: Uri,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L
)

