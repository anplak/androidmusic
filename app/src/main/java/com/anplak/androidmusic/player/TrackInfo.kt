package com.anplak.androidmusic.player

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.anplak.androidmusic.data.AlbumArtworkUri

data class TrackInfo(
    val uri: Uri,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L,
    val path: String = "",
    val year: Int? = null,
    val dateAddedSec: Long? = null,
    val albumId: Long? = null,
    /**
     * Display-only cover when this track has no MediaStore album art
     * (e.g. another album by the same artist). Never persisted.
     */
    val artworkUriOverride: Uri? = null
) {
    /**
     * Extracts the MediaStore ID from the content URI.
     */
    val id: Long
        get() = ContentUris.parseId(uri)

    /** Prefer MediaStore album art; otherwise [artworkUriOverride]. */
    val artworkUri: Uri?
        get() = AlbumArtworkUri.forAlbumId(albumId) ?: artworkUriOverride

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
