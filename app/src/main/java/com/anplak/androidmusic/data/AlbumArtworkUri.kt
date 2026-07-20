package com.anplak.androidmusic.data

import android.content.ContentUris
import android.net.Uri
import com.anplak.androidmusic.player.TrackInfo

/**
 * Builds MediaStore album-art content URIs from [MediaStore.Audio.Media.ALBUM_ID].
 * Does not open or decode bitmaps — display layers load asynchronously.
 */
object AlbumArtworkUri {
    private val baseUri: Uri = Uri.parse("content://media/external/audio/albumart")

    fun forAlbumId(albumId: Long?): Uri? =
        albumId
            ?.takeIf { it > 0L }
            ?.let { ContentUris.withAppendedId(baseUri, it) }

    /**
     * When a track has no MediaStore album art, reuse a cover from another track
     * by the same artist (deterministic: lowest track id with art wins per artist).
     */
    fun withArtistFallbacks(tracks: List<TrackInfo>): List<TrackInfo> {
        if (tracks.isEmpty()) return tracks

        val artistArtwork = tracks
            .groupBy { artistKey(it.artist) }
            .mapNotNull { (key, group) ->
                val uri = group
                    .sortedBy { it.id }
                    .asSequence()
                    .mapNotNull { forAlbumId(it.albumId) }
                    .firstOrNull()
                uri?.let { key to it }
            }
            .toMap()

        if (artistArtwork.isEmpty()) return tracks

        return tracks.map { track ->
            if (forAlbumId(track.albumId) != null) {
                track
            } else {
                val fallback = artistArtwork[artistKey(track.artist)]
                if (fallback == null || track.artworkUriOverride == fallback) {
                    track
                } else {
                    track.copy(artworkUriOverride = fallback)
                }
            }
        }
    }

    private fun artistKey(artist: String): String =
        LibraryIndexFilter.normalizeArtist(
            artist.ifBlank { LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL }
        )
}
