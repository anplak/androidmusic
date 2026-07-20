package com.anplak.androidmusic.data

import android.net.Uri
import com.anplak.androidmusic.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumArtworkUriTest {

    @Test
    fun `forAlbumId builds media albumart content uri`() {
        val uri = AlbumArtworkUri.forAlbumId(42L)

        assertEquals(
            "content://media/external/audio/albumart/42",
            uri.toString()
        )
    }

    @Test
    fun `forAlbumId returns null for null zero and negative ids`() {
        assertNull(AlbumArtworkUri.forAlbumId(null))
        assertNull(AlbumArtworkUri.forAlbumId(0L))
        assertNull(AlbumArtworkUri.forAlbumId(-1L))
    }

    @Test
    fun `withArtistFallbacks reuses cover from another album by same artist`() {
        val withArt = TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/1"),
            title = "Has Cover",
            artist = "Solo",
            album = "Covered",
            albumId = 11L
        )
        val withoutArt = TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/2"),
            title = "No Cover",
            artist = "Solo",
            album = "Bare",
            albumId = null
        )

        val resolved = AlbumArtworkUri.withArtistFallbacks(listOf(withArt, withoutArt))

        assertEquals(
            "content://media/external/audio/albumart/11",
            resolved[1].artworkUri.toString()
        )
        assertEquals(
            "content://media/external/audio/albumart/11",
            resolved[0].artworkUri.toString()
        )
    }

    @Test
    fun `withArtistFallbacks does not cross artists`() {
        val resolved = AlbumArtworkUri.withArtistFallbacks(
            listOf(
                TrackInfo(
                    uri = Uri.parse("content://media/external/audio/media/1"),
                    title = "A",
                    artist = "One",
                    albumId = 5L
                ),
                TrackInfo(
                    uri = Uri.parse("content://media/external/audio/media/2"),
                    title = "B",
                    artist = "Two",
                    albumId = null
                )
            )
        )

        assertNull(resolved[1].artworkUri)
    }
}
