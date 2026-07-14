package com.anplak.androidmusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderTagExtractorTest {

    @Test
    fun `extractTags returns metal for nested artist album path`() {
        val tags = FolderTagExtractor.extractTags(
            "/storage/emulated/0/Music/metal/Artist A/Album/track.mp3"
        )
        assertEquals(listOf("metal"), tags)
    }

    @Test
    fun `extractTags returns rock for artist-only depth`() {
        val tags = FolderTagExtractor.extractTags("/storage/emulated/0/Music/rock/Artist B/track.mp3")
        assertEquals(listOf("rock"), tags)
    }

    @Test
    fun `extractTags strips generic music root`() {
        val tags = FolderTagExtractor.extractTags("/storage/emulated/0/Music/Downloads/Artist/track.mp3")
        assertEquals(emptyList<String>(), tags)
    }

    @Test
    fun `extractTags normalizes case`() {
        val tags = FolderTagExtractor.extractTags("/storage/Music/METAL/Artist/Album/track.mp3")
        assertEquals(listOf("metal"), tags)
    }

    @Test
    fun `primaryTag returns first classifier`() {
        assertEquals("jazz", FolderTagExtractor.primaryTag("/Music/jazz/Artist/Album/song.mp3"))
    }

    @Test
    fun `primaryTag returns null for shallow path`() {
        assertNull(FolderTagExtractor.primaryTag("/Music/track.mp3"))
    }
}
