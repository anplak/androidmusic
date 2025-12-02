package com.anplak.androidmusic.player

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val error: PlayerError? = null
)

