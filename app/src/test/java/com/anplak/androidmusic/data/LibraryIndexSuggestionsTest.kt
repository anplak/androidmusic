package com.anplak.androidmusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIndexSuggestionsTest {
    @Test
    fun `discoverFoldersFromTracks returns parent directories`() {
        val paths =
            listOf(
                "/storage/emulated/0/Music/Audiobooks/chapter1.mp3",
                "/storage/emulated/0/Music/Audiobooks/chapter2.mp3",
                "/storage/emulated/0/Music/Album/track.mp3",
            )

        val folders = LibraryIndexSuggestions.discoverFoldersFromTracks(paths)

        assertTrue(folders.contains("/storage/emulated/0/Music/Audiobooks"))
        assertTrue(folders.contains("/storage/emulated/0/Music/Album"))
        assertEquals(2, folders.size)
    }

    @Test
    fun `discoverFoldersFromTracks excludes existing rules`() {
        val paths = listOf("/storage/emulated/0/Music/Audiobooks/chapter1.mp3")

        val folders =
            LibraryIndexSuggestions.discoverFoldersFromTracks(
                trackPaths = paths,
                existingRulePaths = setOf("/storage/emulated/0/Music/Audiobooks"),
            )

        assertTrue(folders.isEmpty())
    }

    @Test
    fun `mergeFolderSuggestions deduplicates and sorts`() {
        val merged =
            LibraryIndexSuggestions.mergeFolderSuggestions(
                fromTracks = listOf("/storage/Music/Podcasts"),
                subfolders = listOf("/storage/Music/Audiobooks"),
                presetRoots = listOf("/storage/Music"),
                existingRulePaths = emptySet(),
            )

        assertEquals(
            listOf(
                "/storage/Music",
                "/storage/Music/Audiobooks",
                "/storage/Music/Podcasts",
            ),
            merged,
        )
    }

    @Test
    fun `unknown artist constant matches repository default`() {
        assertEquals("Unknown Artist", LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL)
    }
}
