package com.anplak.androidmusic.player

sealed class PlayerError {
    data object FileNotFound : PlayerError()
    data object UnsupportedFormat : PlayerError()
    data class Unknown(val message: String) : PlayerError()
}

