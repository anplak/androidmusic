package com.anplak.androidmusic.data

import android.net.Uri
import com.anplak.androidmusic.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryBrowseAggregatorTest {

    @Test
    fun `aggregateArtists merges case-insensitive duplicates`() {
        val tracks = listOf(
            track(1, artist = "Beatles"),
            track(2, artist = "BEATLES"),
            track(3, artist = "Stones")
        )

        val artists = LibraryBrowseAggregator.aggregateArtists(tracks)

        assertEquals(2, artists.size)
        val beatles = artists.first { it.normalizedKey == "beatles" }
        assertEquals(2, beatles.trackCount)
        assertEquals("Beatles", beatles.displayName)
    }

    @Test
    fun `aggregateArtists groups blank artist as unknown`() {
        val tracks = listOf(
            track(1, artist = ""),
            track(2, artist = "   ")
        )

        val artists = LibraryBrowseAggregator.aggregateArtists(tracks)

        assertEquals(1, artists.size)
        assertEquals(
            LibraryIndexFilter.normalizeArtist(LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL),
            artists.first().normalizedKey
        )
        assertEquals(2, artists.first().trackCount)
    }

    @Test
    fun `aggregateArtists sorts alphabetically`() {
        val tracks = listOf(
            track(1, artist = "Zebra"),
            track(2, artist = "Alpha")
        )

        val artists = LibraryBrowseAggregator.aggregateArtists(tracks)

        assertEquals(listOf("Alpha", "Zebra"), artists.map { it.displayName })
    }

    @Test
    fun `aggregateAlbums splits homonymous titles by artist`() {
        val tracks = listOf(
            track(1, artist = "Artist A", album = "Greatest Hits"),
            track(2, artist = "Artist B", album = "Greatest Hits"),
            track(3, artist = "Artist A", album = "Greatest Hits")
        )

        val albums = LibraryBrowseAggregator.aggregateAlbums(tracks)

        assertEquals(2, albums.size)
        val artistA = albums.first { it.displayArtist == "Artist A" }
        val artistB = albums.first { it.displayArtist == "Artist B" }
        assertEquals(2, artistA.trackCount)
        assertEquals(1, artistB.trackCount)
        assertEquals("artist a", artistA.normalizedArtist)
        assertEquals("artist b", artistB.normalizedArtist)
    }

    @Test
    fun `aggregateAlbums unique title does not require artist key`() {
        val tracks = listOf(
            track(1, artist = "Artist A", album = "Unique Album"),
            track(2, artist = "Artist A", album = "Unique Album")
        )

        val albums = LibraryBrowseAggregator.aggregateAlbums(tracks)

        assertEquals(1, albums.size)
        assertEquals("", albums.first().normalizedArtist)
        assertEquals(2, albums.first().trackCount)
    }

    @Test
    fun `tracksForAlbum filters homonym by artist`() {
        val tracks = listOf(
            track(1, artist = "Artist A", album = "Greatest Hits", title = "One"),
            track(2, artist = "Artist B", album = "Greatest Hits", title = "Two")
        )

        val result = LibraryBrowseAggregator.tracksForAlbum(
            tracks = tracks,
            normalizedTitle = "greatest hits",
            normalizedArtist = "artist a"
        )

        assertEquals(1, result.size)
        assertEquals("One", result.first().title)
    }

    @Test
    fun `tracksForAlbum unique title ignores artist key`() {
        val tracks = listOf(
            track(1, artist = "Artist A", album = "Solo Album", title = "One"),
            track(2, artist = "Artist A", album = "Solo Album", title = "Two")
        )

        val albums = LibraryBrowseAggregator.aggregateAlbums(tracks)
        val solo = albums.single()

        val result = LibraryBrowseAggregator.tracksForAlbum(
            tracks = tracks,
            normalizedTitle = solo.normalizedTitle,
            normalizedArtist = solo.normalizedArtist
        )

        assertEquals(2, result.size)
    }

    @Test
    fun `tracksForArtist is case-insensitive`() {
        val tracks = listOf(
            track(1, artist = "Beatles", title = "A"),
            track(2, artist = "BEATLES", title = "B")
        )

        val result = LibraryBrowseAggregator.tracksForArtist(tracks, "beatles")

        assertEquals(2, result.size)
        assertEquals(listOf("A", "B"), result.map { it.title })
    }

    @Test
    fun `aggregateAlbums sorts by title then artist`() {
        val tracks = listOf(
            track(1, artist = "B Artist", album = "Beta"),
            track(2, artist = "A Artist", album = "Alpha"),
            track(3, artist = "A Artist", album = "Beta")
        )

        val albums = LibraryBrowseAggregator.aggregateAlbums(tracks)

        assertTrue(albums.first().displayTitle == "Alpha")
        assertEquals(listOf("Alpha", "Beta", "Beta"), albums.map { it.displayTitle })
        assertEquals("B Artist", albums[2].displayArtist)
    }

    @Test
    fun `aggregateAlbums picks first valid artwork by track id`() {
        val tracks = listOf(
            track(2, album = "Album", albumId = 20L),
            track(1, album = "Album", albumId = null),
            track(3, album = "Album", albumId = 30L)
        )

        val albums = LibraryBrowseAggregator.aggregateAlbums(tracks)

        assertEquals(1, albums.size)
        assertEquals(
            "content://media/external/audio/albumart/20",
            albums.first().artworkUri.toString()
        )
    }

    @Test
    fun `aggregateArtists skips missing covers and picks deterministic art`() {
        val tracks = listOf(
            track(2, artist = "Solo", albumId = null),
            track(1, artist = "Solo", albumId = 11L),
            track(3, artist = "Solo", albumId = 33L)
        )

        val artists = LibraryBrowseAggregator.aggregateArtists(tracks)

        assertEquals(1, artists.size)
        assertEquals(
            "content://media/external/audio/albumart/11",
            artists.first().artworkUri.toString()
        )
    }

    @Test
    fun `aggregateAlbums falls back to artist cover when album has no art`() {
        val tracks = listOf(
            track(1, artist = "Solo", album = "With Art", albumId = 11L),
            track(2, artist = "Solo", album = "No Art", albumId = null)
        )

        val albums = LibraryBrowseAggregator.aggregateAlbums(tracks)
        val bare = albums.first { it.displayTitle == "No Art" }
        val covered = albums.first { it.displayTitle == "With Art" }

        assertEquals(
            "content://media/external/audio/albumart/11",
            covered.artworkUri.toString()
        )
        assertEquals(
            "content://media/external/audio/albumart/11",
            bare.artworkUri.toString()
        )
    }

    @Test
    fun `aggregateArtists leaves artwork null when no covers exist`() {
        val tracks = listOf(
            track(1, artist = "Bare", albumId = null),
            track(2, artist = "Bare", albumId = 0L)
        )

        val artists = LibraryBrowseAggregator.aggregateArtists(tracks)

        assertNull(artists.first().artworkUri)
    }

    private fun track(
        id: Long,
        title: String = "Song $id",
        artist: String = "Artist",
        album: String = "Album",
        albumId: Long? = null
    ): TrackInfo {
        return TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = artist,
            album = album,
            duration = 180_000L,
            albumId = albumId
        )
    }
}
