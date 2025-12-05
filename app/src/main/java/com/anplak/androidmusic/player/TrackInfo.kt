package com.anplak.androidmusic.player

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class TrackInfo(
    val uri: Uri,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L
) {
    /**
     * Extracts the MediaStore ID from the content URI.
     */
    val id: Long
        get() = ContentUris.parseId(uri)

    companion object {
        /**
         * Creates a content URI from a MediaStore ID.
         */
        fun uriFromId(id: Long): Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            id
        )
    }
}

